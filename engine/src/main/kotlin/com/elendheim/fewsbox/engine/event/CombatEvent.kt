package com.elendheim.fewsbox.engine.event

/**
 * Everything visually meaningful that the engine does is announced here.
 * Today the UI reacts with flashes and floating numbers; later the same
 * events drive real animations. The engine never changes for that.
 */
sealed class CombatEvent {
    data class AbilityUsed(val actorId: String, val abilityId: String, val targetIds: List<String>) : CombatEvent()
    data class DamageDealt(val targetId: String, val amount: Int, val isCrit: Boolean) : CombatEvent()
    data class Healed(val targetId: String, val amount: Int) : CombatEvent()
    data class ShieldGained(val targetId: String, val amount: Int) : CombatEvent()
    data class ShieldBroken(val targetId: String) : CombatEvent()
    data class StatusApplied(val targetId: String, val statusId: String, val stacks: Int) : CombatEvent()
    data class StatusTicked(val targetId: String, val statusId: String, val amount: Int) : CombatEvent()
    data class StatusExpired(val targetId: String, val statusId: String) : CombatEvent()
    data class StatusConsumed(val targetId: String, val statusId: String, val stacks: Int) : CombatEvent()
    data class ChargeAdvanced(val unitId: String, val progress: Float) : CombatEvent()
    data class ChargeFired(val unitId: String, val abilityId: String) : CombatEvent()
    data class TurnSkipped(val unitId: String) : CombatEvent()
    data class UnitDied(val unitId: String) : CombatEvent()
    data class RoundStarted(val round: Int) : CombatEvent()
    data class UltChargeChanged(val percent: Int) : CombatEvent()
    data class Dodged(val unitId: String) : CombatEvent()
    data class HitNegated(val unitId: String) : CombatEvent()
    data class FewsDropped(val unitId: String, val amount: Int) : CombatEvent()
    data class ExtraActionsGranted(val unitId: String, val count: Int) : CombatEvent()
    data object BattleWon : CombatEvent()
    data object BattleLost : CombatEvent()
}
