package com.elendheim.fewsbox.ui.battle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elendheim.fewsbox.data.Battles
import com.elendheim.fewsbox.data.Loadout
import com.elendheim.fewsbox.data.Statuses
import com.elendheim.fewsbox.engine.BattleEngine
import com.elendheim.fewsbox.engine.event.CombatEvent
import com.elendheim.fewsbox.engine.model.BattleState
import com.elendheim.fewsbox.engine.model.Team
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
 *
 * Levels can run several stages. The view model owns the handoff: survivors
 * walk into the next stage with their HP, statuses and the ult meter intact,
 * and damage credit for fallen heroes is kept so XP still pays out.
 */
class BattleViewModel : ViewModel() {

    data class Snapshot(
        val battle: BattleState? = null,
        val version: Int = 0,
        val enemyTurnRunning: Boolean = false,
        val levelIndex: Int = 0,
        val stage: Int = 0,
        val stageCount: Int = 1,
        val stageClearing: Boolean = false
    ) {
        val onFinalStage: Boolean get() = stage >= stageCount - 1
    }

    private val _snapshot = MutableStateFlow(Snapshot())
    val snapshot: StateFlow<Snapshot> = _snapshot

    private val _events = MutableSharedFlow<CombatEvent>(extraBufferCapacity = 256)
    val events: SharedFlow<CombatEvent> = _events

    private var engine: BattleEngine? = null
    private var state: BattleState? = null
    private var party: List<Loadout> = emptyList()
    private var heroLevels: Map<String, Int> = emptyMap()
    // Damage totals of heroes who died in earlier stages; survivors carry
    // their own totals inside their unit.
    private val fallenDamage = mutableMapOf<String, Int>()

    fun startLevel(
        levelIndex: Int,
        party: List<Loadout>,
        heroLevels: Map<String, Int> = emptyMap(),
        seed: Long = System.nanoTime()
    ) {
        this.party = party
        this.heroLevels = heroLevels
        fallenDamage.clear()
        val stageCount = Battles.stageCountFor(levelIndex)
        val newState = Battles.createStage(levelIndex, 0, party, heroLevels)
        val newEngine = BattleEngine(Statuses.REGISTRY, Random(seed)) { event ->
            _events.tryEmit(event)
        }
        engine = newEngine
        state = newState
        _snapshot.update {
            it.copy(levelIndex = levelIndex, stage = 0, stageCount = stageCount, stageClearing = false)
        }
        newEngine.startBattle(newState)
        push()
        maybeRunEnemyTurn()
    }

    fun playerAction(actorId: String, abilityId: String, targetIds: List<String>) {
        val s = state ?: return
        val e = engine ?: return
        val snap = _snapshot.value
        if (snap.enemyTurnRunning || snap.stageClearing) return
        if (!e.playerAction(s, actorId, abilityId, targetIds)) return
        push()
        maybeAdvanceStage()
        maybeRunEnemyTurn()
    }

    /** Damage dealt this level per hero id, including heroes who fell. */
    fun damageByHero(): Map<String, Int> {
        val out = fallenDamage.toMutableMap()
        state?.units?.filter { it.team == Team.PLAYER }?.forEach {
            out[it.id] = (out[it.id] ?: 0) + it.damageDealtTotal
        }
        return out
    }

    /** Hero ids still standing at the end of the last stage. */
    fun survivors(): Set<String> =
        state?.players?.map { it.id }?.toSet() ?: emptySet()

    private fun maybeAdvanceStage() {
        val s = state ?: return
        val snap = _snapshot.value
        if (!s.isVictory || snap.onFinalStage || snap.stageClearing) return

        _snapshot.update { it.copy(stageClearing = true) }
        viewModelScope.launch {
            // Let the last kill land visually before the next wave walks in.
            delay(1100)
            val e = engine ?: return@launch
            // Record what fallen heroes contributed before they drop out.
            s.units.filter { it.team == Team.PLAYER && !it.isAlive }.forEach {
                fallenDamage[it.id] = (fallenDamage[it.id] ?: 0) + it.damageDealtTotal
            }
            val nextStage = snap.stage + 1
            val nextState = Battles.createStage(
                index = snap.levelIndex,
                stage = nextStage,
                party = party,
                heroLevels = heroLevels,
                carriedPlayers = s.players,
                carriedUltCharge = s.partyUltCharge
            )
            state = nextState
            _snapshot.update { it.copy(stage = nextStage, stageClearing = false) }
            e.startBattle(nextState)
            push()
            maybeRunEnemyTurn()
        }
    }

    private fun maybeRunEnemyTurn() {
        val s = state ?: return
        val e = engine ?: return
        if (s.phase != TurnPhase.PLAYER_INPUT || s.pendingPlayers.isNotEmpty()) return
        if (_snapshot.value.enemyTurnRunning || _snapshot.value.stageClearing) return

        _snapshot.update { it.copy(enemyTurnRunning = true) }
        viewModelScope.launch {
            // A beat after the player's last tap, then one enemy at a time
            // with a pause between turns, so the round reads as a sequence
            // instead of a burst of numbers.
            delay(600)
            if (e.beginEnemyPhase(s)) {
                push()
                while (e.nextEnemyTurn(s)) {
                    push()
                    delay(700)
                }
                e.completeRound(s)
            }
            _snapshot.update { it.copy(enemyTurnRunning = false) }
            push()
            maybeAdvanceStage()
        }
    }

    private fun push() {
        val s = state
        _snapshot.update { it.copy(battle = s, version = it.version + 1) }
    }
}
