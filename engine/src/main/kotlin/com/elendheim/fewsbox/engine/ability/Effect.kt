package com.elendheim.fewsbox.engine.ability

/**
 * The composable building blocks of every ability. Each effect is one atomic
 * thing that can happen. Mixing = combining effects that read and write the
 * same shared state (statuses, shields, HP).
 */
sealed class Effect {

    // --- Damage patterns ---

    data class DealDamage(
        val multiplier: Float,       // of attacker's baseAttack
        val hits: Int = 1,
        val canCrit: Boolean = true,
        val extraActionOnKill: Boolean = false  // Reaper's Scythe: kills refund a turn
    ) : Effect()

    /** Bonus damage against low-HP targets. */
    data class ExecuteDamage(
        val multiplier: Float,
        val hpThreshold: Float,      // e.g. 0.3 = below 30%
        val bonusMultiplier: Float   // extra multiplier applied below threshold
    ) : Effect()

    /** Heal the attacker for a fraction of damage dealt so far this action. */
    data class Lifesteal(val fraction: Float) : Effect()

    /** Heal EVERY living ally for a fraction of damage dealt this action. */
    data class TeamLifesteal(val fraction: Float) : Effect()

    /** Heal the most wounded ally for a fraction of damage dealt this action. */
    data class HealLowestAlly(val fraction: Float) : Effect()

    /** Exact damage: ignores attack, never crits. Ultimate material. */
    data class DealFlatDamage(val amount: Int) : Effect()

    /**
     * A strike whose damage grows from somewhere else on the battlefield:
     * base multiplier of attack, plus +1 flat per unit of the chosen source.
     */
    data class ScalingStrike(
        val multiplier: Float,
        val scaling: ScalingSource,
        val canCrit: Boolean = false
    ) : Effect()

    /**
     * One huge hit on the chosen target, then the blast rolls over every
     * other enemy with each one taking `falloff` less than the one before.
     */
    data class CascadeDamage(
        val multiplier: Float,
        val falloff: Float           // 0.4 = each next enemy takes 40% less
    ) : Effect()

    // --- Defense / utility ---

    data class GainShield(val amount: Int) : Effect()

    /** Shield the ACTOR, regardless of who the ability targets. */
    data class ShieldSelf(val amount: Int) : Effect()

    data class Heal(val amount: Int) : Effect()

    /** Heal for a fraction of the target's max health. */
    data class HealPercent(val fraction: Float) : Effect()

    /** Heal every living ally; healing past max HP can become shield. */
    data class HealAllAllies(val amount: Int, val overflowToShield: Boolean = false) : Effect()

    /** Give the target extra actions this round, spendable even after acting. */
    data class GrantExtraActions(val count: Int) : Effect()
    data class Taunt(val turns: Int) : Effect()
    data object Cleanse : Effect()

    /** Strip the BUFFS off the target instead (Tanglewood's dispel). */
    data object DispelBuffs : Effect()

    /** Tick the target's ability cooldowns down (Windrunner's Boon). */
    data class ReduceCooldowns(val amount: Int) : Effect()

    // --- Status application (the combo language) ---

    data class ApplyStatus(
        val statusId: String,
        val stacks: Int,
        val duration: Int,
        val chance: Float = 1f       // 0.35 = 35% roll; misses stay silent
    ) : Effect()

    // --- Status consumption (where mixing becomes multiplicative) ---

    data class ConsumeStatus(
        val statusId: String,
        val perStackEffect: Effect   // e.g. detonate each Burn stack for damage
    ) : Effect()

    /** Nightfall: eat every debuff on the target for flat damage apiece. */
    data class ConsumeAllDebuffs(val flatPerDebuff: Int) : Effect()

    // --- Conditional wrapper ---

    data class Conditional(
        val condition: Condition,
        val then: Effect,
        val otherwise: Effect? = null
    ) : Effect()
}

/** What a ScalingStrike feeds on. */
enum class ScalingSource {
    ULT_PERCENT,      // +1 damage per 1% of stored party ult charge
    OWN_SHIELD,       // +1 damage per point of the attacker's current shield
    HEALING_DONE      // +1 damage per 15 points the attacker healed this battle
}
