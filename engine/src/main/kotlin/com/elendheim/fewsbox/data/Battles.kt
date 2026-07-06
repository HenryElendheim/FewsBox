package com.elendheim.fewsbox.data

import com.elendheim.fewsbox.engine.model.BattleState
import com.elendheim.fewsbox.engine.model.CombatUnit
import com.elendheim.fewsbox.engine.model.ResourceState
import com.elendheim.fewsbox.engine.model.Team

/** A player unit's chosen loadout, picked on the loadout screen. */
data class Loadout(
    val unitId: String,
    val unitName: String,
    val iconId: String,
    val maxHp: Int,
    val baseAttack: Int,
    val weapon: Weapon,
    val offhand: Offhand
)

object Party {

    fun vanguard(weapon: Weapon = Weapons.CLEAVER, offhand: Offhand = Offhands.TOWER_SHIELD) = Loadout(
        unitId = "player_vanguard", unitName = "Vanguard", iconId = "ic_hero_vanguard",
        maxHp = 60, baseAttack = 8, weapon = weapon, offhand = offhand
    )

    fun ranger(weapon: Weapon = Weapons.FAN_BLADES, offhand: Offhand = Offhands.MEDKIT) = Loadout(
        unitId = "player_ranger", unitName = "Ranger", iconId = "ic_hero_ranger",
        maxHp = 45, baseAttack = 9, weapon = weapon, offhand = offhand
    )

    fun defaultParty() = listOf(vanguard(), ranger())
}

fun Loadout.toUnit(): CombatUnit = CombatUnit(
    id = unitId,
    name = unitName,
    iconId = iconId,
    maxHp = maxHp,
    hp = maxHp,
    team = Team.PLAYER,
    baseAttack = baseAttack + weapon.attackBonus,
    abilities = buildAbilities(weapon, offhand)
)

/**
 * The starter battle sequence: composition difficulty ramps, culminating in
 * a mixed line with an elite behind trash.
 */
object Battles {

    val count = 5

    fun create(index: Int, party: List<Loadout>): BattleState {
        val players = party.map { it.toUnit() }
        val enemies = when (index) {
            0 -> listOf(
                Enemies.grunt("enemy_1"), Enemies.grunt("enemy_2"), Enemies.grunt("enemy_3")
            )
            1 -> listOf(
                Enemies.grunt("enemy_1"), Enemies.stinger("enemy_2"),
                Enemies.stinger("enemy_3"), Enemies.grunt("enemy_4")
            )
            2 -> listOf(
                Enemies.shaman("enemy_1"), Enemies.grunt("enemy_2"),
                Enemies.grunt("enemy_3"), Enemies.shaman("enemy_4")
            )
            3 -> listOf(
                Enemies.hexer("enemy_1"), Enemies.stinger("enemy_2"), Enemies.stinger("enemy_3")
            )
            else -> listOf(
                Enemies.brute("enemy_1"), Enemies.grunt("enemy_2"),
                Enemies.shaman("enemy_3"), Enemies.stinger("enemy_4")
            )
        }
        return BattleState(
            units = players + enemies,
            resources = ResourceState(energy = 5, maxEnergy = 5, regenPerRound = 3)
        )
    }
}
