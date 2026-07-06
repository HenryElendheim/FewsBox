package com.elendheim.fewsbox.engine.model

/** A status instance living on a unit. Definition data lives in StatusDef. */
data class ActiveStatus(
    val defId: String,
    var stacks: Int,
    var remainingTurns: Int
)
