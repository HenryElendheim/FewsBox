package com.elendheim.fewsbox.engine.ability

sealed class Condition {
    data class TargetHasStatus(val statusId: String) : Condition()
    data class TargetBelowHp(val fraction: Float) : Condition()
    data object TargetIsShielded : Condition()
    data object SelfBelowHalfHp : Condition()
}
