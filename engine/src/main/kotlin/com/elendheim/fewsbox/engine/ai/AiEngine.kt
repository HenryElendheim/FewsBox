package com.elendheim.fewsbox.engine.ai

import com.elendheim.fewsbox.engine.ability.Ability
import com.elendheim.fewsbox.engine.model.BattleState
import com.elendheim.fewsbox.engine.model.CombatUnit
import kotlin.random.Random

/**
 * Weighted random move selection with conditional nudges. Nudges only add
 * weight — the odds shift, the outcome stays uncertain. That is the fairness
 * contract: predictable odds, unpredictable specifics.
 */
object AiEngine {

    fun chooseAction(state: BattleState, enemy: CombatUnit, rng: Random): Pair<Ability, List<String>>? {
        val profile = enemy.aiProfile ?: return null

        val weights = LinkedHashMap<String, Int>()
        for (move in profile.weightedMoves) {
            weights.merge(move.abilityId, move.weight, Int::plus)
        }

        for (nudge in profile.nudges) {
            when (nudge) {
                is AiNudge.HealWhenLow ->
                    if (enemy.hp.toFloat() / enemy.maxHp < nudge.hpFraction)
                        weights.merge(nudge.healAbilityId, nudge.bonusWeight, Int::plus)

                is AiNudge.ShieldWhenThreatened ->
                    if (teamIsThreatened(state, enemy))
                        weights.merge(nudge.shieldAbilityId, nudge.bonusWeight, Int::plus)
            }
        }

        // Only moves the unit actually has and that are off cooldown.
        val known = enemy.abilities.associateBy { it.id }
        val legal = weights.filterKeys { id -> id in known && enemy.cooldownLeft(id) == 0 }
        if (legal.isEmpty()) return null

        val ability = known.getValue(weightedPick(legal, rng))
        val targets = pickTargets(state, enemy, rng)
        return ability to targets
    }

    fun weightedPick(weights: Map<String, Int>, rng: Random): String {
        val total = weights.values.sum()
        var roll = rng.nextInt(total)
        for ((id, weight) in weights) {
            roll -= weight
            if (roll < 0) return id
        }
        return weights.keys.first()
    }

    private fun teamIsThreatened(state: BattleState, enemy: CombatUnit): Boolean =
        state.units.any { it.team == enemy.team && it.isAlive && it.hp * 2 < it.maxHp }

    private fun pickTargets(state: BattleState, enemy: CombatUnit, rng: Random): List<String> {
        val foes = state.units.filter { it.team != enemy.team && it.isAlive }
        if (foes.isEmpty()) return emptyList()
        // The Resolver enforces taunt and handles self/ally targeting; a
        // random living foe is the right default suggestion for everything else.
        return listOf(foes.random(rng).id)
    }
}
