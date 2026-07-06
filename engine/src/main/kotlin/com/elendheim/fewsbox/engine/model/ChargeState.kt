package com.elendheim.fewsbox.engine.model

/**
 * The telegraph mechanic. An elite winds up a big attack over several turns.
 * The UI renders [progress] as a filling ring; that alone is the telegraph.
 */
data class ChargeState(
    val chargingAbilityId: String,
    val turnsRequired: Int,
    var turnsElapsed: Int = 0
) {
    val isReady: Boolean get() = turnsElapsed >= turnsRequired
    val progress: Float get() = turnsElapsed.toFloat() / turnsRequired
}
