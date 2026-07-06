package com.elendheim.fewsbox.ui.battle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elendheim.fewsbox.data.Battles
import com.elendheim.fewsbox.data.Loadout
import com.elendheim.fewsbox.data.Statuses
import com.elendheim.fewsbox.engine.BattleEngine
import com.elendheim.fewsbox.engine.event.CombatEvent
import com.elendheim.fewsbox.engine.model.BattleState
import com.elendheim.fewsbox.engine.model.TurnPhase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Thin bridge between the pure engine and Compose. The engine mutates the
 * BattleState in place, so snapshots carry a version counter — that is what
 * makes StateFlow treat each push as a new emission.
 */
class BattleViewModel : ViewModel() {

    data class Snapshot(
        val battle: BattleState? = null,
        val version: Int = 0,
        val enemyTurnRunning: Boolean = false
    )

    private val _snapshot = MutableStateFlow(Snapshot())
    val snapshot: StateFlow<Snapshot> = _snapshot

    private val _events = MutableSharedFlow<CombatEvent>(extraBufferCapacity = 256)
    val events: SharedFlow<CombatEvent> = _events

    private var engine: BattleEngine? = null
    private var state: BattleState? = null

    fun startBattle(battleIndex: Int, party: List<Loadout>, seed: Long = System.nanoTime()) {
        val newState = Battles.create(battleIndex, party)
        val newEngine = BattleEngine(Statuses.REGISTRY, Random(seed)) { event ->
            _events.tryEmit(event)
        }
        engine = newEngine
        state = newState
        newEngine.startBattle(newState)
        push()
        maybeRunEnemyTurn()
    }

    fun playerAction(actorId: String, abilityId: String, targetIds: List<String>) {
        val s = state ?: return
        val e = engine ?: return
        if (_snapshot.value.enemyTurnRunning) return
        if (!e.playerAction(s, actorId, abilityId, targetIds)) return
        push()
        maybeRunEnemyTurn()
    }

    /** Skip a unit's action (nothing affordable, or the player chooses to hold). */
    fun passUnit(actorId: String) {
        val s = state ?: return
        if (s.phase != TurnPhase.PLAYER_INPUT) return
        if (s.pendingPlayers.none { it.id == actorId }) return
        s.actedThisRound.add(actorId)
        push()
        maybeRunEnemyTurn()
    }

    private fun maybeRunEnemyTurn() {
        val s = state ?: return
        val e = engine ?: return
        if (s.phase != TurnPhase.PLAYER_INPUT || s.pendingPlayers.isNotEmpty()) return
        if (_snapshot.value.enemyTurnRunning) return

        _snapshot.update { it.copy(enemyTurnRunning = true) }
        viewModelScope.launch {
            // A beat between the player's last tap and the enemy response so
            // the outcome of the player's action is readable.
            delay(600)
            e.finishRound(s)
            _snapshot.update { it.copy(enemyTurnRunning = false) }
            push()
        }
    }

    private fun push() {
        val s = state
        _snapshot.update { it.copy(battle = s, version = it.version + 1) }
    }
}
