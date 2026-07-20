package com.elendheim.fewsbox.engine.ability

import com.elendheim.fewsbox.engine.event.CombatEvent
import com.elendheim.fewsbox.engine.model.ActiveStatus
import com.elendheim.fewsbox.engine.model.BattleState
import com.elendheim.fewsbox.engine.model.CombatUnit
import com.elendheim.fewsbox.engine.model.Team
import com.elendheim.fewsbox.engine.status.PassiveEffect
import com.elendheim.fewsbox.engine.status.StatusDef
import com.elendheim.fewsbox.engine.status.StatusKind
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Resolves a single ability use: targeting, every effect in order, damage
 * math, status bookkeeping. Mutates state and narrates everything through
 * the event emitter. Knows nothing about UI, energy or turn order — that is
 * BattleEngine's job.
 */
class Resolver(
    private val statusRegistry: Map<String, StatusDef>,
    private val rng: Random,
    private val emit: (CombatEvent) -> Unit
) {

    companion object {
        const val TAUNT_STATUS_ID = "taunt"
        const val CRIT_CHANCE = 0.10f
        const val CRIT_MULTIPLIER = 1.5f

        // Ultimate meter: percent gained per point of damage dealt / taken.
        const val ULT_PER_DAMAGE_DEALT = 2
        const val ULT_PER_DAMAGE_TAKEN = 3
    }

    /** Running totals for one ability use; lifesteal reads damageDealt. */
    private class ActionContext(var damageDealt: Int = 0)

    fun resolve(state: BattleState, actor: CombatUnit, ability: Ability, chosenTargetIds: List<String>) {
        val targets = resolveTargets(state, actor, ability.targeting, chosenTargetIds)
        emit(CombatEvent.AbilityUsed(actor.id, ability.id, targets.map { it.id }))

        val ctx = ActionContext()
        val spread = ability.targeting == Targeting.RANDOM_ENEMIES_MULTI
        for (effect in ability.effects) {
            if (spread && effect is Effect.DealDamage) {
                // Each hit picks its own random living target so multi-hit
                // weapons hose down a whole group.
                repeat(effect.hits) {
                    val pool = opposing(state, actor)
                    if (pool.isEmpty()) return@repeat
                    dealDamageHit(state, actor, pool.random(rng), effect.multiplier, effect.canCrit, ctx)
                }
            } else {
                for (target in targets) {
                    applyEffect(state, actor, target, effect, ctx)
                }
            }
        }
    }

    // ------------------------------------------------------------------
    //  Targeting
    // ------------------------------------------------------------------

    private fun opposing(state: BattleState, actor: CombatUnit) =
        state.units.filter { it.team != actor.team && it.isAlive }

    private fun allied(state: BattleState, actor: CombatUnit) =
        state.units.filter { it.team == actor.team && it.isAlive }

    private fun resolveTargets(
        state: BattleState,
        actor: CombatUnit,
        targeting: Targeting,
        chosenIds: List<String>
    ): List<CombatUnit> {
        val foes = opposing(state, actor)
        val friends = allied(state, actor)
        if (foes.isEmpty() && targeting != Targeting.SELF &&
            targeting != Targeting.SINGLE_ALLY && targeting != Targeting.ALL_ALLIES
        ) return emptyList()

        return when (targeting) {
            Targeting.SINGLE_ENEMY -> {
                // A living taunter on the opposing side overrides the choice.
                val taunter = foes.firstOrNull { it.hasStatus(TAUNT_STATUS_ID) }
                val chosen = chosenIds.firstOrNull()?.let { id -> foes.firstOrNull { it.id == id } }
                listOfNotNull(taunter ?: chosen ?: foes.random(rng))
            }
            Targeting.ALL_ENEMIES -> foes
            Targeting.RANDOM_ENEMY -> listOf(foes.random(rng))
            Targeting.RANDOM_ENEMIES_MULTI -> foes  // per-hit picks happen in resolve()
            Targeting.SELF -> listOf(actor)
            Targeting.SINGLE_ALLY -> {
                val chosen = chosenIds.firstOrNull()?.let { id -> friends.firstOrNull { it.id == id } }
                listOfNotNull(chosen ?: actor.takeIf { it.isAlive } ?: friends.firstOrNull())
            }
            Targeting.ALL_ALLIES -> friends
            Targeting.ADJACENT_ENEMIES -> {
                val chosen = chosenIds.firstOrNull()?.let { id -> foes.firstOrNull { it.id == id } }
                    ?: foes.random(rng)
                val idx = foes.indexOf(chosen)
                foes.filterIndexed { i, _ -> i in (idx - 1)..(idx + 1) }
            }
        }
    }

    // ------------------------------------------------------------------
    //  Effects
    // ------------------------------------------------------------------

    private fun applyEffect(
        state: BattleState,
        actor: CombatUnit,
        target: CombatUnit,
        effect: Effect,
        ctx: ActionContext
    ) {
        when (effect) {
            is Effect.DealDamage -> repeat(effect.hits) {
                if (target.isAlive) dealDamageHit(state, actor, target, effect.multiplier, effect.canCrit, ctx)
            }

            is Effect.ExecuteDamage -> {
                if (!target.isAlive) return
                val belowThreshold = target.hp.toFloat() / target.maxHp < effect.hpThreshold
                val multiplier =
                    if (belowThreshold) effect.multiplier + effect.bonusMultiplier else effect.multiplier
                dealDamageHit(state, actor, target, multiplier, canCrit = true, ctx)
            }

            is Effect.Lifesteal -> {
                val amount = (ctx.damageDealt * effect.fraction).roundToInt()
                if (amount > 0) healUnit(actor, amount)
            }

            is Effect.GainShield -> {
                target.shield += effect.amount
                emit(CombatEvent.ShieldGained(target.id, effect.amount))
            }

            is Effect.Heal -> healUnit(target, effect.amount)

            is Effect.Taunt -> {
                addStatus(target, TAUNT_STATUS_ID, stacks = 1, duration = effect.turns)
            }

            is Effect.Cleanse -> {
                val debuffs = target.statuses.filter {
                    statusRegistry[it.defId]?.kind == StatusKind.DEBUFF
                }
                for (status in debuffs) {
                    target.statuses.remove(status)
                    emit(CombatEvent.StatusExpired(target.id, status.defId))
                }
            }

            is Effect.ApplyStatus -> {
                if (!target.isAlive) return
                addStatus(target, effect.statusId, effect.stacks, effect.duration)
            }

            is Effect.ConsumeStatus -> {
                val stacks = target.statusStacks(effect.statusId)
                if (stacks <= 0) return
                target.statuses.removeAll { it.defId == effect.statusId }
                emit(CombatEvent.StatusConsumed(target.id, effect.statusId, stacks))
                repeat(stacks) {
                    applyEffect(state, actor, target, effect.perStackEffect, ctx)
                }
            }

            is Effect.Conditional -> {
                if (checkCondition(actor, target, effect.condition)) {
                    applyEffect(state, actor, target, effect.then, ctx)
                }
            }
        }
    }

    private fun checkCondition(actor: CombatUnit, target: CombatUnit, condition: Condition): Boolean =
        when (condition) {
            is Condition.TargetHasStatus -> target.hasStatus(condition.statusId)
            is Condition.TargetBelowHp -> target.hp.toFloat() / target.maxHp < condition.fraction
            is Condition.TargetIsShielded -> target.shield > 0
            is Condition.SelfBelowHalfHp -> actor.hp * 2 < actor.maxHp
        }

    // ------------------------------------------------------------------
    //  Damage & healing
    // ------------------------------------------------------------------

    private fun dealDamageHit(
        state: BattleState,
        actor: CombatUnit,
        target: CombatUnit,
        multiplier: Float,
        canCrit: Boolean,
        ctx: ActionContext
    ) {
        var raw = actor.baseAttack * multiplier

        // Weaken on the attacker reduces output.
        raw *= passiveFactor(actor, PassiveEffect.DAMAGE_DEALT_DOWN, reduces = true)
        // Vulnerable / Fortify on the target scale what lands.
        raw *= passiveFactor(target, PassiveEffect.DAMAGE_TAKEN_UP, reduces = false)
        raw *= passiveFactor(target, PassiveEffect.DAMAGE_TAKEN_DOWN, reduces = true)

        val isCrit = canCrit && rng.nextFloat() < CRIT_CHANCE
        if (isCrit) raw *= CRIT_MULTIPLIER

        val amount = raw.roundToInt().coerceAtLeast(1)
        applyDamage(target, amount)
        ctx.damageDealt += amount
        emit(CombatEvent.DamageDealt(target.id, amount, isCrit))
        if (!target.isAlive) emit(CombatEvent.UnitDied(target.id))

        // The party's shared meter fills whenever heroes deal or take damage.
        if (actor.team == Team.PLAYER) gainPartyUlt(state, amount * ULT_PER_DAMAGE_DEALT)
        if (target.team == Team.PLAYER) gainPartyUlt(state, amount * ULT_PER_DAMAGE_TAKEN)

        // Thorns: the target strikes back a flat amount per hit taken. The
        // reflection never re-reflects and doesn't feed lifesteal.
        val reflect = target.statuses.sumOf { status ->
            val def = statusRegistry[status.defId]
            if (def?.passive == PassiveEffect.THORNS) def.magnitude * status.stacks else 0
        }
        if (reflect > 0 && actor.isAlive) {
            applyDamage(actor, reflect)
            emit(CombatEvent.DamageDealt(actor.id, reflect, false))
            if (target.team == Team.PLAYER) gainPartyUlt(state, reflect * ULT_PER_DAMAGE_DEALT)
            if (actor.team == Team.PLAYER) gainPartyUlt(state, reflect * ULT_PER_DAMAGE_TAKEN)
            if (!actor.isAlive) emit(CombatEvent.UnitDied(actor.id))
        }
    }

    /** Fill the party's shared ultimate meter, capped at 100. */
    fun gainPartyUlt(state: BattleState, amount: Int) {
        if (amount <= 0) return
        val before = state.partyUltCharge
        state.partyUltCharge = (state.partyUltCharge + amount).coerceAtMost(100)
        if (state.partyUltCharge != before) emit(CombatEvent.UltChargeChanged(state.partyUltCharge))
    }

    private fun passiveFactor(unit: CombatUnit, passive: PassiveEffect, reduces: Boolean): Float {
        var factor = 1.0f
        for (status in unit.statuses) {
            val def = statusRegistry[status.defId] ?: continue
            if (def.passive == passive) {
                val pct = def.magnitude / 100f
                factor *= if (reduces) (1f - pct) else (1f + pct)
            }
        }
        return factor.coerceAtLeast(0f)
    }

    /** Shields soak damage before HP. Exposed for status ticks too. */
    fun applyDamage(target: CombatUnit, amount: Int) {
        var remaining = amount
        if (target.shield > 0) {
            val absorbed = minOf(target.shield, remaining)
            target.shield -= absorbed
            remaining -= absorbed
            if (target.shield == 0 && absorbed > 0) emit(CombatEvent.ShieldBroken(target.id))
        }
        target.hp = (target.hp - remaining).coerceAtLeast(0)
    }

    fun healUnit(target: CombatUnit, amount: Int) {
        if (!target.isAlive) return
        val healed = minOf(amount, target.maxHp - target.hp)
        if (healed <= 0) return
        target.hp += healed
        emit(CombatEvent.Healed(target.id, healed))
    }

    fun addStatus(target: CombatUnit, statusId: String, stacks: Int, duration: Int) {
        val existing = target.statuses.firstOrNull { it.defId == statusId }
        if (existing != null) {
            existing.stacks += stacks
            existing.remainingTurns = maxOf(existing.remainingTurns, duration)
        } else {
            target.statuses.add(ActiveStatus(statusId, stacks, duration))
        }
        emit(CombatEvent.StatusApplied(target.id, statusId, stacks))
    }
}
