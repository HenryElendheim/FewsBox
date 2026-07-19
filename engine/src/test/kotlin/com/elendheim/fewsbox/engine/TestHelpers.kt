package com.elendheim.fewsbox.engine

import com.elendheim.fewsbox.data.Statuses
import com.elendheim.fewsbox.engine.ability.Ability
import com.elendheim.fewsbox.engine.ability.Effect
import com.elendheim.fewsbox.engine.ability.Targeting
import com.elendheim.fewsbox.engine.event.CombatEvent
import com.elendheim.fewsbox.engine.model.BattleState
import com.elendheim.fewsbox.engine.model.ChargeState
import com.elendheim.fewsbox.engine.model.CombatUnit
import com.elendheim.fewsbox.engine.model.Team
import com.elendheim.fewsbox.engine.ai.AiProfile
import kotlin.random.Random

fun unit(
    id: String,
    team: Team,
    hp: Int = 50,
    attack: Int = 10,
    abilities: List<Ability> = emptyList(),
    charge: ChargeState? = null,
    aiProfile: AiProfile? = null,
    ultimateId: String? = null
) = CombatUnit(
    id = id, name = id, iconId = "ic_test",
    maxHp = hp, hp = hp, team = team, baseAttack = attack,
    abilities = abilities, charge = charge, aiProfile = aiProfile,
    ultimateId = ultimateId
)

/** A plain deterministic hit: no crit, so damage math is exact in tests. */
fun plainHit(multiplier: Float, hits: Int = 1, targeting: Targeting = Targeting.SINGLE_ENEMY) = Ability(
    id = "test_hit_${multiplier}_$hits", iconId = "ic_test",
    targeting = targeting,
    effects = listOf(Effect.DealDamage(multiplier = multiplier, hits = hits, canCrit = false))
)

fun battle(vararg units: CombatUnit) = BattleState(units = units.toList())

class Recorder {
    val events = mutableListOf<CombatEvent>()
    val emit: (CombatEvent) -> Unit = { events.add(it) }
    inline fun <reified T : CombatEvent> all(): List<T> = events.filterIsInstance<T>()
}

fun engine(recorder: Recorder, seed: Int = 42) =
    BattleEngine(Statuses.REGISTRY, Random(seed), recorder.emit)
