package com.elendheim.fewsbox.engine.model

data class ResourceState(
    var energy: Int,
    val maxEnergy: Int,
    val regenPerRound: Int
)

data class BattleState(
    val units: List<CombatUnit>,
    val resources: ResourceState,
    var round: Int = 1,
    var phase: TurnPhase = TurnPhase.PLAYER_INPUT,
    val actedThisRound: MutableSet<String> = mutableSetOf()
) {
    val players get() = units.filter { it.team == Team.PLAYER && it.isAlive }
    val enemies get() = units.filter { it.team == Team.ENEMY && it.isAlive }
    val isVictory get() = enemies.isEmpty()
    val isDefeat get() = players.isEmpty()

    fun unit(id: String): CombatUnit = units.first { it.id == id }
    fun unitOrNull(id: String): CombatUnit? = units.firstOrNull { it.id == id }

    /** Living player units that still get to act this round. */
    val pendingPlayers get() = players.filter { it.id !in actedThisRound }
}

enum class TurnPhase { PLAYER_INPUT, RESOLVING, ENEMY_TURN, BATTLE_OVER }
