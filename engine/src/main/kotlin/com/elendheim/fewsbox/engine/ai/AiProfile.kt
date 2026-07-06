package com.elendheim.fewsbox.engine.ai

/**
 * Fair RNG with light nudges. Nudges add weight, they never hard-force a
 * move — a hurt enemy is more likely to heal, not guaranteed to.
 */
data class AiProfile(
    val weightedMoves: List<WeightedMove>,
    val nudges: List<AiNudge> = emptyList()
)

data class WeightedMove(val abilityId: String, val weight: Int)

sealed class AiNudge {
    /** Below hpFraction of max HP, add bonusWeight to a heal ability. */
    data class HealWhenLow(val hpFraction: Float, val healAbilityId: String, val bonusWeight: Int) : AiNudge()

    /** When any ally on this unit's team is below half HP, favor shielding. */
    data class ShieldWhenThreatened(val shieldAbilityId: String, val bonusWeight: Int) : AiNudge()
}
