package com.elendheim.fewsbox.engine.model

import com.elendheim.fewsbox.engine.ability.Ability
import com.elendheim.fewsbox.engine.ai.AiProfile

/**
 * A single combatant. Called CombatUnit rather than Unit because kotlin.Unit
 * shadows it and turns every unqualified reference into a bug hunt.
 */
data class CombatUnit(
    val id: String,                  // unique instance id, e.g. "enemy_3"
    val name: String,                // debugging/accessibility only, never gameplay text
    val iconId: String,              // presentation looks this up; engine ignores meaning
    val maxHp: Int,
    var hp: Int,
    val team: Team,
    val baseAttack: Int,
    var shield: Int = 0,             // absorbed before HP
    val abilities: List<Ability>,
    val statuses: MutableList<ActiveStatus> = mutableListOf(),
    val charge: ChargeState? = null, // non-null for elites that telegraph big hits
    val aiProfile: AiProfile? = null, // null for player-controlled units
    val cooldowns: MutableMap<String, Int> = mutableMapOf(), // abilityId -> turns left
    val ultimateId: String? = null,  // which ability is gated by the ult meter
    var ultCharge: Int = 0           // 0..100, filled by dealing and taking damage
) {
    val ultReady: Boolean get() = ultCharge >= 100

    val isAlive: Boolean get() = hp > 0

    fun statusStacks(statusId: String): Int =
        statuses.firstOrNull { it.defId == statusId }?.stacks ?: 0

    fun hasStatus(statusId: String): Boolean = statusStacks(statusId) > 0

    fun cooldownLeft(abilityId: String): Int = cooldowns[abilityId] ?: 0
}

enum class Team { PLAYER, ENEMY }
