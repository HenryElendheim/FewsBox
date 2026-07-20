package com.elendheim.fewsbox.engine.model

data class BattleState(
    val units: List<CombatUnit>,
    var round: Int = 1,
    var phase: TurnPhase = TurnPhase.PLAYER_INPUT,
    // Actions are counted, not flagged: heroes get one per round plus any
    // extras granted mid-round (Green's ultimate hands out two each).
    val actionsTaken: MutableMap<String, Int> = mutableMapOf(),
    val extraActions: MutableMap<String, Int> = mutableMapOf(),
    // Enemies waiting to act this enemy phase; lets the UI step through
    // one turn at a time and pace the round.
    val enemyQueue: MutableList<String> = mutableListOf(),
    // The party's shared ultimate meter in TENTHS of a percent (0..1000):
    // +5% per landed attack, +3% per hit taken (+15% for a hit that costs
    // over half the hero's max HP), never over full.
    var partyUltCharge: Int = 0
) {
    val partyUltPercent: Int get() = partyUltCharge / 10
    val partyUltReady: Boolean get() = partyUltCharge >= 1000

    val players get() = units.filter { it.team == Team.PLAYER && it.isAlive }
    val enemies get() = units.filter { it.team == Team.ENEMY && it.isAlive }
    val isVictory get() = enemies.isEmpty()
    val isDefeat get() = players.isEmpty()

    fun unit(id: String): CombatUnit = units.first { it.id == id }
    fun unitOrNull(id: String): CombatUnit? = units.firstOrNull { it.id == id }

    fun actionsLeft(unit: CombatUnit): Int =
        1 + (extraActions[unit.id] ?: 0) - (actionsTaken[unit.id] ?: 0)

    fun spendAction(unitId: String) {
        actionsTaken[unitId] = (actionsTaken[unitId] ?: 0) + 1
    }

    /** Living player units that still get to act this round. */
    val pendingPlayers get() = players.filter { actionsLeft(it) > 0 }
}

enum class TurnPhase { PLAYER_INPUT, RESOLVING, ENEMY_TURN, BATTLE_OVER }
