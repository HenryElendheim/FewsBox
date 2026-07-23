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
 * the event emitter. Knows nothing about UI or turn order — that is
 * BattleEngine's job.
 */
class Resolver(
    private val statusRegistry: Map<String, StatusDef>,
    private val rng: Random,
    private val emit: (CombatEvent) -> Unit
) {

    companion object {
        const val TAUNT_STATUS_ID = "taunt"
        const val BURN_STATUS_ID = "burn"
        const val VULNERABLE_STATUS_ID = "vulnerable"
        const val DULL_STATUS_ID = "dull"
        const val WIND_STATUS_ID = "wind"
        const val CRIT_CHANCE = 0.10f
        const val CRIT_MULTIPLIER = 1.5f

        // Ultimate meter, in tenths of a percent. Landing an attack pays a
        // flat 5%; each hit taken pays 3% — or 15% when a single hit costs
        // more than half the hero's max HP.
        const val ULT_PER_ATTACK = 50
        const val ULT_PER_HIT_TAKEN = 30
        const val ULT_BIG_HIT_TAKEN = 150

        fun ultForHitTaken(target: CombatUnit, amount: Int): Int =
            if (amount > target.maxHp / 2) ULT_BIG_HIT_TAKEN else ULT_PER_HIT_TAKEN
    }

    /** Running totals for one ability use; lifesteal reads damageDealt. */
    private class ActionContext(var damageDealt: Int = 0, var primaryTarget: CombatUnit? = null)

    fun resolve(state: BattleState, actor: CombatUnit, ability: Ability, chosenTargetIds: List<String>) {
        val targets = resolveTargets(state, actor, ability.targeting, chosenTargetIds)
        emit(CombatEvent.AbilityUsed(actor.id, ability.id, targets.map { it.id }))

        val ctx = ActionContext(primaryTarget = targets.firstOrNull())
        when (ability.targeting) {
            Targeting.RANDOM_ENEMIES_MULTI -> resolveSpread(state, actor, ability, chosenTargetIds, ctx)
            Targeting.WHIP_ADJACENT -> resolveWhip(state, actor, ability, chosenTargetIds, ctx)
            Targeting.VOLLEY_ADJACENT -> resolveVolley(state, actor, ability, chosenTargetIds, ctx)
            else -> for (effect in ability.effects) {
                for (target in targets) {
                    applyEffect(state, actor, target, effect, ctx)
                }
            }
        }

        // Mirror Image: the attack echoes once, for free, at reduced power.
        val echoPct = passiveMagnitude(actor, PassiveEffect.ECHO)
        if (echoPct > 0 && ctx.damageDealt > 0) {
            val echoTarget = ctx.primaryTarget?.takeIf { it.isAlive && it.team != actor.team }
                ?: opposing(state, actor).firstOrNull()
            if (echoTarget != null) {
                val echo = (ctx.damageDealt * echoPct / 100f).roundToInt().coerceAtLeast(1)
                landHit(state, actor, echoTarget, echo, isCrit = false, ctx, isReaction = true)
            }
        }

        // Attacking builds the party meter: one flat payment per swing that
        // actually lands damage, no matter how many hits it splits into.
        if (actor.team == Team.PLAYER && ctx.damageDealt > 0) {
            gainPartyUlt(state, ULT_PER_ATTACK)
        }
    }

    /** Random spray: first hit honors the aim, the rest pick their own targets. */
    private fun resolveSpread(
        state: BattleState, actor: CombatUnit, ability: Ability,
        chosenTargetIds: List<String>, ctx: ActionContext
    ) {
        for (effect in ability.effects) {
            if (effect is Effect.DealDamage) {
                repeat(effect.hits) { hitIndex ->
                    val pool = opposing(state, actor)
                    if (pool.isEmpty()) return@repeat
                    val aimed = chosenTargetIds.firstOrNull()
                        ?.let { id -> pool.firstOrNull { it.id == id } }
                    val target = if (hitIndex == 0 && aimed != null) aimed else pool.random(rng)
                    dealDamageHit(state, actor, target, effect.multiplier, effect.canCrit, ctx)
                }
            } else {
                for (target in opposing(state, actor)) applyEffect(state, actor, target, effect, ctx)
            }
        }
    }

    private fun neighborsOf(state: BattleState, actor: CombatUnit, main: CombatUnit): Pair<CombatUnit?, CombatUnit?> {
        val foes = opposing(state, actor)
        val idx = foes.indexOf(main)
        return Pair(foes.getOrNull(idx - 1), foes.getOrNull(idx + 1))
    }

    /** Flame Whip: 3 lashes across target and neighbors; loners eat all 3. */
    private fun resolveWhip(
        state: BattleState, actor: CombatUnit, ability: Ability,
        chosenTargetIds: List<String>, ctx: ActionContext
    ) {
        val foes = opposing(state, actor)
        if (foes.isEmpty()) return
        val main = chosenTargetIds.firstOrNull()?.let { id -> foes.firstOrNull { it.id == id } }
            ?: foes.random(rng)
        ctx.primaryTarget = main
        val (left, right) = neighborsOf(state, actor, main)
        val lashes = listOf(main, left ?: main, right ?: main)
        for (effect in ability.effects) {
            if (effect is Effect.DealDamage) {
                for (lash in lashes) {
                    if (lash.isAlive) dealDamageHit(state, actor, lash, effect.multiplier, effect.canCrit, ctx)
                }
            } else {
                for (target in lashes.distinct()) applyEffect(state, actor, target, effect, ctx)
            }
        }
    }

    /** Volley: full hits on the target, one spillover hit per neighbor. */
    private fun resolveVolley(
        state: BattleState, actor: CombatUnit, ability: Ability,
        chosenTargetIds: List<String>, ctx: ActionContext
    ) {
        val foes = opposing(state, actor)
        if (foes.isEmpty()) return
        val main = chosenTargetIds.firstOrNull()?.let { id -> foes.firstOrNull { it.id == id } }
            ?: foes.random(rng)
        ctx.primaryTarget = main
        val (left, right) = neighborsOf(state, actor, main)
        val touched = listOfNotNull(main, left, right)
        for (effect in ability.effects) {
            if (effect is Effect.DealDamage) {
                repeat(effect.hits) {
                    if (main.isAlive) dealDamageHit(state, actor, main, effect.multiplier, effect.canCrit, ctx)
                }
                for (side in listOfNotNull(left, right)) {
                    if (side.isAlive) dealDamageHit(state, actor, side, effect.multiplier, effect.canCrit, ctx)
                }
            } else {
                for (target in touched) applyEffect(state, actor, target, effect, ctx)
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

    private fun hasPassive(unit: CombatUnit, passive: PassiveEffect): Boolean =
        unit.statuses.any { statusRegistry[it.defId]?.passive == passive }

    private fun passiveMagnitude(unit: CombatUnit, passive: PassiveEffect): Int =
        unit.statuses.sumOf { status ->
            statusRegistry[status.defId]?.takeIf { it.passive == passive }?.magnitude ?: 0
        }

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

        // Cloaked units slip out of single-target selection (AoE still finds
        // them); if everyone is cloaked, the cloak stops mattering.
        val pickable = foes.filterNot { hasPassive(it, PassiveEffect.UNTARGETABLE) }.ifEmpty { foes }

        return when (targeting) {
            Targeting.SINGLE_ENEMY -> {
                // A lured attacker swings at the healthiest opposing unit,
                // no matter what; otherwise a taunter overrides the choice.
                if (hasPassive(actor, PassiveEffect.LURE)) {
                    listOf(pickable.maxBy { it.hp })
                } else {
                    val taunter = pickable.firstOrNull { it.hasStatus(TAUNT_STATUS_ID) }
                    val chosen = chosenIds.firstOrNull()?.let { id -> pickable.firstOrNull { it.id == id } }
                    listOfNotNull(taunter ?: chosen ?: pickable.random(rng))
                }
            }
            Targeting.HIGHEST_HP_ENEMY -> listOf(pickable.maxBy { it.hp })
            Targeting.ALL_ENEMIES -> foes
            Targeting.RANDOM_ENEMY -> listOf(pickable.random(rng))
            Targeting.RANDOM_ENEMIES_MULTI -> foes  // per-hit picks happen in resolve()
            Targeting.WHIP_ADJACENT, Targeting.VOLLEY_ADJACENT -> {
                val chosen = chosenIds.firstOrNull()?.let { id -> foes.firstOrNull { it.id == id } }
                listOfNotNull(chosen ?: pickable.randomOrNull(rng))
            }
            Targeting.SELF -> listOf(actor)
            Targeting.SINGLE_ALLY -> {
                val chosen = chosenIds.firstOrNull()?.let { id -> friends.firstOrNull { it.id == id } }
                listOfNotNull(chosen ?: actor.takeIf { it.isAlive } ?: friends.firstOrNull())
            }
            Targeting.ALL_ALLIES -> friends
            Targeting.ADJACENT_ENEMIES -> {
                val chosen = chosenIds.firstOrNull()?.let { id -> foes.firstOrNull { it.id == id } }
                    ?: pickable.random(rng)
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
                if (target.isAlive) {
                    dealDamageHit(
                        state, actor, target, effect.multiplier, effect.canCrit, ctx,
                        extraActionOnKill = effect.extraActionOnKill
                    )
                }
            }

            is Effect.ExecuteDamage -> {
                if (!target.isAlive) return
                val belowThreshold = target.hp.toFloat() / target.maxHp < effect.hpThreshold
                val multiplier =
                    if (belowThreshold) effect.multiplier + effect.bonusMultiplier else effect.multiplier
                dealDamageHit(state, actor, target, multiplier, canCrit = true, ctx)
            }

            is Effect.ScalingStrike -> {
                if (!target.isAlive) return
                val bonus = when (effect.scaling) {
                    ScalingSource.ULT_PERCENT -> state.partyUltPercent
                    ScalingSource.OWN_SHIELD -> actor.shield
                    ScalingSource.HEALING_DONE -> actor.healingDoneTotal / 15
                }
                dealDamageHit(state, actor, target, effect.multiplier, effect.canCrit, ctx, flatBonus = bonus)
            }

            is Effect.CascadeDamage -> {
                if (!target.isAlive) return
                var multiplier = effect.multiplier
                dealDamageHit(state, actor, target, multiplier, canCrit = false, ctx)
                for (other in opposing(state, actor).filter { it.id != target.id }) {
                    multiplier *= (1f - effect.falloff)
                    if (multiplier <= 0.01f) break
                    dealDamageHit(state, actor, other, multiplier, canCrit = false, ctx)
                }
            }

            is Effect.Lifesteal -> {
                val amount = (ctx.damageDealt * effect.fraction).roundToInt()
                if (amount > 0) healUnit(actor, amount, healer = actor)
            }

            is Effect.TeamLifesteal -> {
                val amount = (ctx.damageDealt * effect.fraction).roundToInt()
                if (amount > 0) for (ally in allied(state, actor)) healUnit(ally, amount, healer = actor)
            }

            is Effect.HealLowestAlly -> {
                val amount = (ctx.damageDealt * effect.fraction).roundToInt()
                val wounded = allied(state, actor).minByOrNull { it.hp.toFloat() / it.maxHp }
                if (amount > 0 && wounded != null) healUnit(wounded, amount, healer = actor)
            }

            is Effect.DealFlatDamage -> {
                if (!target.isAlive) return
                // Exact damage: target-side modifiers still count, crits never do.
                var raw = effect.amount.toFloat()
                raw *= passiveFactor(target, PassiveEffect.DAMAGE_TAKEN_UP, reduces = false)
                raw *= passiveFactor(target, PassiveEffect.DAMAGE_TAKEN_DOWN, reduces = true)
                landHit(state, actor, target, raw.roundToInt().coerceAtLeast(1), isCrit = false, ctx)
            }

            is Effect.HealPercent -> {
                healUnit(target, (target.maxHp * effect.fraction).roundToInt(), healer = actor)
            }

            is Effect.GrantExtraActions -> {
                if (!target.isAlive) return
                state.extraActions[target.id] = (state.extraActions[target.id] ?: 0) + effect.count
                emit(CombatEvent.ExtraActionsGranted(target.id, effect.count))
            }

            is Effect.GainShield -> {
                target.shield += effect.amount
                emit(CombatEvent.ShieldGained(target.id, effect.amount))
            }

            is Effect.ShieldSelf -> {
                actor.shield += effect.amount
                emit(CombatEvent.ShieldGained(actor.id, effect.amount))
            }

            is Effect.Heal -> healUnit(target, effect.amount, healer = actor)

            is Effect.HealAllAllies -> {
                for (ally in allied(state, actor)) {
                    val healed = healUnit(ally, effect.amount, healer = actor)
                    if (effect.overflowToShield) {
                        val spill = effect.amount - healed
                        if (spill > 0) {
                            ally.shield += spill
                            emit(CombatEvent.ShieldGained(ally.id, spill))
                        }
                    }
                }
            }

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

            is Effect.DispelBuffs -> {
                val buffs = target.statuses.filter {
                    statusRegistry[it.defId]?.kind == StatusKind.BUFF
                }
                for (status in buffs) {
                    target.statuses.remove(status)
                    emit(CombatEvent.StatusExpired(target.id, status.defId))
                }
            }

            is Effect.ReduceCooldowns -> {
                val keys = target.cooldowns.keys.toList()
                for (id in keys) {
                    val left = target.cooldowns.getValue(id) - effect.amount
                    if (left <= 0) target.cooldowns.remove(id) else target.cooldowns[id] = left
                }
            }

            is Effect.ApplyStatus -> {
                if (!target.isAlive) return
                if (effect.chance < 1f && rng.nextFloat() >= effect.chance) return
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

            is Effect.ConsumeAllDebuffs -> {
                if (!target.isAlive) return
                val debuffs = target.statuses.filter {
                    statusRegistry[it.defId]?.kind == StatusKind.DEBUFF
                }
                if (debuffs.isEmpty()) return
                for (status in debuffs) {
                    target.statuses.remove(status)
                    emit(CombatEvent.StatusConsumed(target.id, status.defId, status.stacks))
                }
                landHit(state, actor, target, effect.flatPerDebuff * debuffs.size, isCrit = false, ctx)
            }

            is Effect.Conditional -> {
                if (checkCondition(actor, target, effect.condition)) {
                    applyEffect(state, actor, target, effect.then, ctx)
                } else {
                    effect.otherwise?.let { applyEffect(state, actor, target, it, ctx) }
                }
            }
        }
    }

    private fun checkCondition(actor: CombatUnit, target: CombatUnit, condition: Condition): Boolean =
        when (condition) {
            is Condition.TargetHasStatus -> target.hasStatus(condition.statusId)
            is Condition.TargetBelowHp -> target.hp.toFloat() / target.maxHp < condition.fraction
            is Condition.TargetAtOrAboveHp -> target.hp.toFloat() / target.maxHp >= condition.fraction
            is Condition.TargetIsShielded -> target.shield > 0
            is Condition.TargetHasAnyDebuff -> target.statuses.any {
                statusRegistry[it.defId]?.kind == StatusKind.DEBUFF
            }
            is Condition.SelfBelowHalfHp -> actor.hp * 2 < actor.maxHp
            is Condition.SelfIsShielded -> actor.shield > 0
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
        ctx: ActionContext,
        flatBonus: Int = 0,
        extraActionOnKill: Boolean = false
    ) {
        // Wind in the attacker's eyes: the whole swing can miss outright.
        val missChance = passiveMagnitude(actor, PassiveEffect.MISS_CHANCE)
        if (missChance > 0 && rng.nextInt(100) < missChance) {
            emit(CombatEvent.Dodged(target.id))
            return
        }

        var raw = actor.baseAttack * multiplier + flatBonus

        // The attacker's own ups and downs.
        raw *= passiveFactor(actor, PassiveEffect.DAMAGE_DEALT_DOWN, reduces = true)
        raw *= passiveFactor(actor, PassiveEffect.DAMAGE_DEALT_UP, reduces = false)
        raw += passiveMagnitude(actor, PassiveEffect.DAMAGE_DEALT_UP_FLAT)
        // Vulnerable / Fortify on the target scale what lands.
        raw *= passiveFactor(target, PassiveEffect.DAMAGE_TAKEN_UP, reduces = false)
        raw *= passiveFactor(target, PassiveEffect.DAMAGE_TAKEN_DOWN, reduces = true)

        val isCrit = canCrit && rng.nextFloat() < CRIT_CHANCE
        if (isCrit) raw *= CRIT_MULTIPLIER

        val wasAlive = target.isAlive
        landHit(state, actor, target, raw.roundToInt().coerceAtLeast(1), isCrit, ctx)

        // On-hit enchants ride real attack hits only.
        if (target.isAlive) {
            if (hasPassive(actor, PassiveEffect.ON_HIT_APPLY_BURN)) {
                addStatus(target, BURN_STATUS_ID, stacks = 1, duration = 2)
            }
            if (hasPassive(actor, PassiveEffect.ON_HIT_APPLY_VULN)) {
                addStatus(target, VULNERABLE_STATUS_ID, stacks = 1, duration = 2)
            }
        }

        // Reaper's Scythe: a kill hands the attacker their turn back.
        if (extraActionOnKill && wasAlive && !target.isAlive && actor.isAlive) {
            state.extraActions[actor.id] = (state.extraActions[actor.id] ?: 0) + 1
            emit(CombatEvent.ExtraActionsGranted(actor.id, 1))
        }
    }

    /** The landing half of any hit: damage, events, meter gains, reactions. */
    private fun landHit(
        state: BattleState,
        actor: CombatUnit,
        target: CombatUnit,
        amount: Int,
        isCrit: Boolean,
        ctx: ActionContext,
        isReaction: Boolean = false
    ) {
        // A bubble swallows the hit whole.
        val bubble = target.statuses.firstOrNull {
            statusRegistry[it.defId]?.passive == PassiveEffect.NEGATE_HIT
        }
        if (bubble != null) {
            bubble.stacks--
            if (bubble.stacks <= 0) target.statuses.remove(bubble)
            emit(CombatEvent.HitNegated(target.id))
            return
        }

        // Smokescreen: a clean dodge takes nothing at all.
        val dodge = passiveMagnitude(target, PassiveEffect.DODGE)
        if (dodge > 0 && rng.nextInt(100) < dodge) {
            emit(CombatEvent.Dodged(target.id))
            return
        }

        applyDamage(target, amount)
        ctx.damageDealt += amount
        actor.damageDealtTotal += amount
        emit(CombatEvent.DamageDealt(target.id, amount, isCrit))
        if (!target.isAlive) emit(CombatEvent.UnitDied(target.id))

        // Getting hit builds the party meter, and getting truly hurt —
        // over half your max HP in one blow — builds it fast.
        if (target.team == Team.PLAYER) gainPartyUlt(state, ultForHitTaken(target, amount))

        // The Thief enchant shakes fews loose with every landed hit.
        if (target.team != actor.team) {
            val fews = passiveMagnitude(actor, PassiveEffect.THIEF)
            if (fews > 0) {
                state.fewsEarned += fews
                emit(CombatEvent.FewsDropped(target.id, fews))
            }
        }

        // Mommy Kisses: the wound closes a little the instant it's made.
        val healPct = passiveMagnitude(target, PassiveEffect.HEAL_WHEN_HIT)
        if (healPct > 0 && target.isAlive) {
            healUnit(target, (amount * healPct / 100f).roundToInt())
        }

        // Reactions only fire off primary hits — no infinite mirror halls.
        if (!isReaction && actor.isAlive && actor.team != target.team) {
            // Thorns: flat retaliation per hit taken.
            val thorns = target.statuses.sumOf { status ->
                val def = statusRegistry[status.defId]
                if (def?.passive == PassiveEffect.THORNS) def.magnitude * status.stacks else 0
            }
            if (thorns > 0) {
                landHit(state, target, actor, thorns, isCrit = false, ctx = ActionContext(), isReaction = true)
            }

            // Return To Sender: proportional payback.
            val reflectPct = passiveMagnitude(target, PassiveEffect.REFLECT_PERCENT)
            if (reflectPct > 0 && actor.isAlive) {
                val back = (amount * reflectPct / 100f).roundToInt().coerceAtLeast(1)
                landHit(state, target, actor, back, isCrit = false, ctx = ActionContext(), isReaction = true)
            }

            // Matchstick: attackers catch fire scaled to the pain they caused.
            val burnPct = passiveMagnitude(target, PassiveEffect.BURN_REFLECT)
            if (burnPct > 0 && actor.isAlive) {
                val burnDef = statusRegistry[BURN_STATUS_ID]
                val perStack = (burnDef?.magnitude ?: 3).coerceAtLeast(1)
                val stacks = ((amount * burnPct / 100f) / perStack).roundToInt().coerceAtLeast(1)
                addStatus(actor, BURN_STATUS_ID, stacks, duration = 3)
            }

            // Payback Charm: touching the marked one dulls your blade.
            if (hasPassive(target, PassiveEffect.PUNISH_WEAKEN) && actor.isAlive) {
                addStatus(actor, DULL_STATUS_ID, stacks = 1, duration = 1)
            }

            // Windrunner's Boon: hit the sheltered and the wind takes your
            // next swings — half of them just miss.
            if (hasPassive(target, PassiveEffect.PUNISH_WIND) && actor.isAlive) {
                addStatus(actor, WIND_STATUS_ID, stacks = 1, duration = 2)
            }

            // Scorching Skies: the marked one swings straight back, no turn spent.
            if (hasPassive(target, PassiveEffect.COUNTER) && target.isAlive && actor.isAlive) {
                landHit(state, target, actor, target.baseAttack, isCrit = false,
                    ctx = ActionContext(), isReaction = true)
            }
        }
    }

    /** Fill the party's shared ultimate meter, hard-capped at full. */
    fun gainPartyUlt(state: BattleState, amount: Int) {
        if (amount <= 0) return
        val before = state.partyUltCharge
        state.partyUltCharge = (state.partyUltCharge + amount).coerceIn(0, 1000)
        if (state.partyUltCharge != before) emit(CombatEvent.UltChargeChanged(state.partyUltPercent))
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

    /** Shields soak damage before HP; a death ward refuses the final blow. */
    fun applyDamage(target: CombatUnit, amount: Int) {
        var remaining = amount
        if (target.shield > 0) {
            val absorbed = minOf(target.shield, remaining)
            target.shield -= absorbed
            remaining -= absorbed
            if (target.shield == 0 && absorbed > 0) emit(CombatEvent.ShieldBroken(target.id))
        }
        if (remaining >= target.hp) {
            val ward = target.statuses.firstOrNull {
                statusRegistry[it.defId]?.passive == PassiveEffect.DEATH_WARD
            }
            if (ward != null) {
                target.statuses.remove(ward)
                emit(CombatEvent.StatusConsumed(target.id, ward.defId, ward.stacks))
                target.hp = 1
                return
            }
        }
        target.hp = (target.hp - remaining).coerceAtLeast(0)
    }

    fun healUnit(target: CombatUnit, amount: Int, healer: CombatUnit? = null): Int {
        if (!target.isAlive) return 0
        val healed = minOf(amount, target.maxHp - target.hp)
        if (healed <= 0) return 0
        target.hp += healed
        healer?.let { it.healingDoneTotal += healed }
        emit(CombatEvent.Healed(target.id, healed))
        return healed
    }

    fun addStatus(target: CombatUnit, statusId: String, stacks: Int, duration: Int) {
        // Sunrise: debuffs bounce off the immune.
        val def = statusRegistry[statusId]
        if (def?.kind == StatusKind.DEBUFF &&
            target.statuses.any { statusRegistry[it.defId]?.passive == PassiveEffect.DEBUFF_IMMUNE }
        ) return

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
