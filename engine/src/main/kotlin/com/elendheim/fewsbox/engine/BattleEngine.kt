package com.elendheim.fewsbox.engine

import com.elendheim.fewsbox.engine.ability.Resolver
import com.elendheim.fewsbox.engine.ai.AiEngine
import com.elendheim.fewsbox.engine.event.CombatEvent
import com.elendheim.fewsbox.engine.model.BattleState
import com.elendheim.fewsbox.engine.model.CombatUnit
import com.elendheim.fewsbox.engine.model.Team
import com.elendheim.fewsbox.engine.model.TurnPhase
import com.elendheim.fewsbox.engine.status.StatusDef
import com.elendheim.fewsbox.engine.status.StatusKind
import com.elendheim.fewsbox.engine.status.StatusTiming

/**
 * Owns the turn loop. Strict order of operations, always the same:
 *
 *  round start -> refill energy, tick player statuses (stunned units sit out)
 *  player phase -> each living player unit acts once, resolved immediately
 *  enemy phase -> per enemy: tick statuses, stun check, charge logic or AI move
 *  end of round -> durations down, cooldowns down, round++
 *
 * Win/lose is checked after every single action, not just at round end.
 */
class BattleEngine(
    private val statusRegistry: Map<String, StatusDef>,
    private val rng: kotlin.random.Random,
    private val emit: (CombatEvent) -> Unit
) {

    companion object { const val STUN_STATUS_ID = "stun" }

    private val resolver = Resolver(statusRegistry, rng, emit)

    /** Call once after constructing the BattleState. */
    fun startBattle(state: BattleState) {
        emit(CombatEvent.RoundStarted(state.round))
        emit(CombatEvent.EnergyChanged(state.resources.energy, state.resources.maxEnergy))
        tickPlayerPhaseStart(state)
    }

    /**
     * One player unit uses one ability. Returns false if the action is not
     * legal right now (wrong phase, dead/spent actor, cost, cooldown).
     */
    fun playerAction(state: BattleState, actorId: String, abilityId: String, targetIds: List<String>): Boolean {
        if (state.phase != TurnPhase.PLAYER_INPUT) return false
        val actor = state.unitOrNull(actorId) ?: return false
        if (!actor.isAlive || actor.team != Team.PLAYER || actor.id in state.actedThisRound) return false
        val ability = actor.abilities.firstOrNull { it.id == abilityId } ?: return false
        if (state.resources.energy < ability.cost) return false
        if (actor.cooldownLeft(ability.id) > 0) return false

        state.resources.energy -= ability.cost
        emit(CombatEvent.EnergyChanged(state.resources.energy, state.resources.maxEnergy))
        if (ability.cooldown > 0) actor.cooldowns[ability.id] = ability.cooldown

        state.phase = TurnPhase.RESOLVING
        resolver.resolve(state, actor, ability, targetIds)
        state.actedThisRound.add(actor.id)
        if (!checkBattleEnd(state)) state.phase = TurnPhase.PLAYER_INPUT
        return true
    }

    /**
     * Runs the enemy phase and closes out the round. The UI calls this once
     * every living player unit has acted (or the player passes).
     */
    fun finishRound(state: BattleState) {
        if (state.phase != TurnPhase.PLAYER_INPUT) return
        state.phase = TurnPhase.ENEMY_TURN

        for (enemy in state.units.filter { it.team == Team.ENEMY }) {
            if (!enemy.isAlive) continue

            tickStartOfTurnStatuses(state, enemy)
            if (checkBattleEnd(state)) return
            if (!enemy.isAlive) continue

            if (consumeStunIfPresent(enemy)) continue

            val charge = enemy.charge
            if (charge != null) {
                if (charge.isReady) {
                    val ability = enemy.abilities.firstOrNull { it.id == charge.chargingAbilityId }
                    if (ability != null) {
                        emit(CombatEvent.ChargeFired(enemy.id, ability.id))
                        resolver.resolve(state, enemy, ability, emptyList())
                    }
                    charge.turnsElapsed = 0
                    emit(CombatEvent.ChargeAdvanced(enemy.id, charge.progress))
                } else {
                    // Charging enemies only charge. Readable on purpose.
                    charge.turnsElapsed++
                    emit(CombatEvent.ChargeAdvanced(enemy.id, charge.progress))
                }
            } else {
                val action = AiEngine.chooseAction(state, enemy, rng)
                if (action != null) {
                    val (ability, targets) = action
                    if (ability.cooldown > 0) enemy.cooldowns[ability.id] = ability.cooldown
                    resolver.resolve(state, enemy, ability, targets)
                }
            }
            if (checkBattleEnd(state)) return
        }

        endOfRound(state)
        startNextRound(state)
    }

    // ------------------------------------------------------------------
    //  Round bookkeeping
    // ------------------------------------------------------------------

    private fun endOfRound(state: BattleState) {
        for (unit in state.units.filter { it.isAlive }) {
            val expired = mutableListOf<String>()
            for (status in unit.statuses) {
                status.remainingTurns--
                if (status.remainingTurns <= 0) expired.add(status.defId)
            }
            for (id in expired) {
                unit.statuses.removeAll { it.defId == id }
                emit(CombatEvent.StatusExpired(unit.id, id))
            }

            val done = unit.cooldowns.filterValues { it <= 1 }.keys.toList()
            for (id in done) unit.cooldowns.remove(id)
            for (id in unit.cooldowns.keys) unit.cooldowns[id] = unit.cooldowns.getValue(id) - 1
        }
        state.round++
    }

    private fun startNextRound(state: BattleState) {
        state.actedThisRound.clear()
        val res = state.resources
        res.energy = minOf(res.maxEnergy, res.energy + res.regenPerRound)
        emit(CombatEvent.RoundStarted(state.round))
        emit(CombatEvent.EnergyChanged(res.energy, res.maxEnergy))
        state.phase = TurnPhase.PLAYER_INPUT
        tickPlayerPhaseStart(state)
    }

    /** Player units tick their statuses at the start of their phase; a
     *  stunned unit burns the stun and sits the round out. */
    private fun tickPlayerPhaseStart(state: BattleState) {
        for (player in state.units.filter { it.team == Team.PLAYER }) {
            if (!player.isAlive) continue
            tickStartOfTurnStatuses(state, player)
            if (checkBattleEnd(state)) return
            if (player.isAlive && consumeStunIfPresent(player)) {
                state.actedThisRound.add(player.id)
            }
        }
    }

    // ------------------------------------------------------------------
    //  Statuses
    // ------------------------------------------------------------------

    private fun tickStartOfTurnStatuses(state: BattleState, unit: CombatUnit) {
        val ticking = unit.statuses.filter {
            statusRegistry[it.defId]?.timing == StatusTiming.TICK_START_OF_TURN
        }
        for (status in ticking) {
            val def = statusRegistry.getValue(status.defId)
            val amount = def.magnitude * status.stacks
            if (amount <= 0) continue

            if (def.kind == StatusKind.DEBUFF) {
                resolver.applyDamage(unit, amount)
                emit(CombatEvent.StatusTicked(unit.id, def.id, amount))
                if (!unit.isAlive) {
                    emit(CombatEvent.UnitDied(unit.id))
                    return
                }
            } else {
                resolver.healUnit(unit, amount)
                emit(CombatEvent.StatusTicked(unit.id, def.id, amount))
            }

            if (def.decaysOnTick) {
                status.stacks--
                if (status.stacks <= 0) {
                    unit.statuses.remove(status)
                    emit(CombatEvent.StatusExpired(unit.id, def.id))
                }
            }
        }
    }

    private fun consumeStunIfPresent(unit: CombatUnit): Boolean {
        if (!unit.hasStatus(STUN_STATUS_ID)) return false
        unit.statuses.removeAll { it.defId == STUN_STATUS_ID }
        emit(CombatEvent.StatusExpired(unit.id, STUN_STATUS_ID))
        emit(CombatEvent.TurnSkipped(unit.id))
        // Stunning a charging elite resets the wind-up. This is what makes
        // the Lockdown play real: deny the big hit, not just delay it.
        unit.charge?.let {
            it.turnsElapsed = 0
            emit(CombatEvent.ChargeAdvanced(unit.id, it.progress))
        }
        return true
    }

    private fun checkBattleEnd(state: BattleState): Boolean {
        if (state.phase == TurnPhase.BATTLE_OVER) return true
        if (state.isVictory) {
            state.phase = TurnPhase.BATTLE_OVER
            emit(CombatEvent.BattleWon)
            return true
        }
        if (state.isDefeat) {
            state.phase = TurnPhase.BATTLE_OVER
            emit(CombatEvent.BattleLost)
            return true
        }
        return false
    }
}
