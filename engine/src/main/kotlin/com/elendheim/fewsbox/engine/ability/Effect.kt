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
        val canCrit: Boolean = true
    ) : Effect()

    /** Bonus damage against low-HP targets. */
    data class ExecuteDamage(
        val multiplier: Float,
        val hpThreshold: Float,      // e.g. 0.3 = below 30%
        val bonusMultiplier: Float   // extra multiplier applied below threshold
    ) : Effect()

    /** Heal the attacker for a fraction of damage dealt so far this action. */
    data class Lifesteal(val fraction: Float) : Effect()

    /** Exact damage: ignores attack, never crits. Ultimate material. */
    data class DealFlatDamage(val amount: Int) : Effect()

    // --- Defense / utility ---

    data class GainShield(val amount: Int) : Effect()
    data class Heal(val amount: Int) : Effect()

    /** Heal for a fraction of the target's max health. */
    data class HealPercent(val fraction: Float) : Effect()

    /** Give the target extra actions this round, spendable even after acting. */
    data class GrantExtraActions(val count: Int) : Effect()
    data class Taunt(val turns: Int) : Effect()
    data object Cleanse : Effect()

    // --- Status application (the combo language) ---

    data class ApplyStatus(val statusId: String, val stacks: Int, val duration: Int) : Effect()

    // --- Status consumption (where mixing becomes multiplicative) ---

    data class ConsumeStatus(
        val statusId: String,
        val perStackEffect: Effect   // e.g. detonate each Burn stack for damage
    ) : Effect()

    // --- Conditional wrapper ---

    data class Conditional(
        val condition: Condition,
        val then: Effect
    ) : Effect()
}
