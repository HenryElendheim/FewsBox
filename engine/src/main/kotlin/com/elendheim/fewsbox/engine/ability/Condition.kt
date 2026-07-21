package com.elendheim.fewsbox.engine.ability

sealed class Condition {
    data class TargetHasStatus(val statusId: String) : Condition()
    data class TargetBelowHp(val fraction: Float) : Condition()
    data class TargetAtOrAboveHp(val fraction: Float) : Condition()
    data object TargetIsShielded : Condition()
    data object TargetHasAnyDebuff : Condition()
    data object SelfBelowHalfHp : Condition()
    data object SelfIsShielded : Condition()
}
