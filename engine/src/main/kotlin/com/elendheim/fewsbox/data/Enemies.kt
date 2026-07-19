package com.elendheim.fewsbox.data

import com.elendheim.fewsbox.engine.ai.AiNudge
import com.elendheim.fewsbox.engine.ai.AiProfile
import com.elendheim.fewsbox.engine.ai.WeightedMove
import com.elendheim.fewsbox.engine.model.ChargeState
import com.elendheim.fewsbox.engine.model.CombatUnit
import com.elendheim.fewsbox.engine.model.Team

/**
 * Enemy factories. Trash mobs are individually weaker than a player unit
 * and come in numbers; elites are stronger and telegraph their big hit by
 * charging over several turns.
 */
object Enemies {

    // --- Trash (3 types) ---

    fun grunt(id: String) = CombatUnit(
        id = id, name = "Grunt", iconId = "ic_enemy_grunt",
        maxHp = 20, hp = 20, team = Team.ENEMY, baseAttack = 5,
        abilities = listOf(EnemyAbilities.BASIC_SLASH, EnemyAbilities.SMALL_GUARD),
        aiProfile = AiProfile(
            weightedMoves = listOf(
                WeightedMove("basic_slash", 8),
                WeightedMove("small_guard", 2)
            ),
            nudges = listOf(AiNudge.ShieldWhenThreatened("small_guard", 4))
        )
    )

    fun stinger(id: String) = CombatUnit(
        id = id, name = "Stinger", iconId = "ic_enemy_stinger",
        maxHp = 14, hp = 14, team = Team.ENEMY, baseAttack = 6,
        abilities = listOf(EnemyAbilities.VENOM_SPIT, EnemyAbilities.BASIC_SLASH),
        aiProfile = AiProfile(
            weightedMoves = listOf(
                WeightedMove("venom_spit", 6),
                WeightedMove("basic_slash", 4)
            )
        )
    )

    fun shaman(id: String) = CombatUnit(
        id = id, name = "Shaman", iconId = "ic_enemy_shaman",
        maxHp = 16, hp = 16, team = Team.ENEMY, baseAttack = 4,
        abilities = listOf(EnemyAbilities.BASIC_SLASH, EnemyAbilities.SMALL_HEAL),
        aiProfile = AiProfile(
            weightedMoves = listOf(
                WeightedMove("basic_slash", 6),
                WeightedMove("small_heal", 4)
            ),
            nudges = listOf(AiNudge.HealWhenLow(0.5f, "small_heal", 8))
        )
    )

    // --- Elites (2 types) ---

    fun brute(id: String) = CombatUnit(
        id = id, name = "Brute", iconId = "ic_enemy_brute",
        maxHp = 90, hp = 90, team = Team.ENEMY, baseAttack = 12,
        abilities = listOf(EnemyAbilities.CRUSHING_BLOW),
        charge = ChargeState(chargingAbilityId = "crushing_blow", turnsRequired = 3)
    )

    fun hexer(id: String) = CombatUnit(
        id = id, name = "Hexer", iconId = "ic_enemy_hexer",
        maxHp = 70, hp = 70, team = Team.ENEMY, baseAttack = 10,
        abilities = listOf(EnemyAbilities.DOOM_BOLT),
        charge = ChargeState(chargingAbilityId = "doom_bolt", turnsRequired = 2)
    )

    // --- Bosses ---

    // The second grayscale boss (in campaign order, the first you meet).
    // No telegraph: Ash is an AI boss that smothers the party in slow
    // death and shields up when pressed. Defectable, per PLAN.md.
    fun ash(id: String) = CombatUnit(
        id = id, name = "Ash", iconId = "ic_enemy_ash",
        maxHp = 120, hp = 120, team = Team.ENEMY, baseAttack = 9,
        abilities = listOf(EnemyAbilities.ASH_CLOUD, EnemyAbilities.CINDER_SPIT, EnemyAbilities.EMBER_GUARD),
        aiProfile = AiProfile(
            weightedMoves = listOf(
                WeightedMove("ash_cloud", 4),
                WeightedMove("cinder_spit", 5),
                WeightedMove("ember_guard", 2)
            ),
            nudges = listOf(AiNudge.ShieldWhenThreatened("ember_guard", 5))
        )
    )


    // The first grayscale boss. A short, relentless telegraph: Silver Storm
    // fires every third turn and leaves the party weakened. Per PLAN.md,
    // defeated grayscale bosses eventually defect to the player's side —
    // Silver is the first candidate. Gray never will.
    fun silver(id: String) = CombatUnit(
        id = id, name = "Silver", iconId = "ic_enemy_silver",
        maxHp = 150, hp = 150, team = Team.ENEMY, baseAttack = 13,
        abilities = listOf(EnemyAbilities.SILVER_STORM),
        charge = ChargeState(chargingAbilityId = "silver_storm", turnsRequired = 2)
    )
}
