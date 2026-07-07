package com.elendheim.fewsbox.data

import com.elendheim.fewsbox.engine.model.BattleState
import com.elendheim.fewsbox.engine.model.CombatUnit
import com.elendheim.fewsbox.engine.model.ResourceState
import com.elendheim.fewsbox.engine.model.Team

/**
 * A hero on the roster. Each color has its own restricted equipment pools —
 * five of the six weapons and five of the six offhands — so every hero is
 * missing a different piece and no two builds play the same.
 */
data class HeroDef(
    val id: String,
    val name: String,
    val iconId: String,
    val maxHp: Int,
    val baseAttack: Int,
    val weaponIds: List<String>,
    val offhandIds: List<String>,
    val defaultWeaponId: String,
    val defaultOffhandId: String
)

/** A hero plus the equipment currently chosen for it. */
data class Loadout(
    val hero: HeroDef,
    val weapon: Weapon,
    val offhand: Offhand
)

object Party {

    // Battle party cap. The roster is bigger; you pick who fights.
    const val MAX_SIZE = 3

    private val allWeapons = Weapons.ALL.map { it.id }
    private val allOffhands = Offhands.ALL.map { it.id }

    // The rainbow roster. Hidden colors (Pink, Black, White) and the
    // grayscale bosses live in PLAN.md until they're built.

    val RED = HeroDef(
        id = "hero_red", name = "Red", iconId = "ic_hero_red",
        maxHp = 55, baseAttack = 11,
        weaponIds = allWeapons - "wpn_fan_blades",
        offhandIds = allOffhands - "off_medkit",
        defaultWeaponId = "wpn_cleaver", defaultOffhandId = "off_spiked_shield"
    )

    val ORANGE = HeroDef(
        id = "hero_orange", name = "Orange", iconId = "ic_hero_orange",
        maxHp = 42, baseAttack = 10,
        weaponIds = allWeapons - "wpn_leech",
        offhandIds = allOffhands - "off_tower_shield",
        defaultWeaponId = "wpn_ember_blade", defaultOffhandId = "off_detonator"
    )

    val YELLOW = HeroDef(
        id = "hero_yellow", name = "Yellow", iconId = "ic_hero_yellow",
        maxHp = 48, baseAttack = 8,
        weaponIds = allWeapons - "wpn_reaper",
        offhandIds = allOffhands - "off_detonator",
        defaultWeaponId = "wpn_leech", defaultOffhandId = "off_medkit"
    )

    val GREEN = HeroDef(
        id = "hero_green", name = "Green", iconId = "ic_hero_green",
        maxHp = 45, baseAttack = 9,
        weaponIds = allWeapons - "wpn_cleaver",
        offhandIds = allOffhands - "off_banner",
        defaultWeaponId = "wpn_fan_blades", defaultOffhandId = "off_cleanser"
    )

    val BLUE = HeroDef(
        id = "hero_blue", name = "Blue", iconId = "ic_hero_blue",
        maxHp = 60, baseAttack = 8,
        weaponIds = allWeapons - "wpn_piercer",
        offhandIds = allOffhands - "off_cleanser",
        defaultWeaponId = "wpn_cleaver", defaultOffhandId = "off_tower_shield"
    )

    val VIOLET = HeroDef(
        id = "hero_violet", name = "Violet", iconId = "ic_hero_violet",
        maxHp = 40, baseAttack = 10,
        weaponIds = allWeapons - "wpn_ember_blade",
        offhandIds = allOffhands - "off_spiked_shield",
        defaultWeaponId = "wpn_reaper", defaultOffhandId = "off_detonator"
    )

    val ROSTER = listOf(RED, ORANGE, YELLOW, GREEN, BLUE, VIOLET)

    val DEFAULT_PARTY_IDS = setOf("hero_red", "hero_green", "hero_blue")

    fun defaultLoadout(hero: HeroDef) = Loadout(
        hero = hero,
        weapon = Weapons.REGISTRY.getValue(hero.defaultWeaponId),
        offhand = Offhands.REGISTRY.getValue(hero.defaultOffhandId)
    )

    fun rosterDefaults(): List<Loadout> = ROSTER.map { defaultLoadout(it) }

    fun defaultParty(): List<Loadout> =
        rosterDefaults().filter { it.hero.id in DEFAULT_PARTY_IDS }
}

fun Loadout.toUnit(): CombatUnit = CombatUnit(
    id = hero.id,
    name = hero.name,
    iconId = hero.iconId,
    maxHp = hero.maxHp,
    hp = hero.maxHp,
    team = Team.PLAYER,
    baseAttack = hero.baseAttack + weapon.attackBonus,
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
        // Three heroes at ~2 energy per action need a bigger pool than two did.
        return BattleState(
            units = players + enemies,
            resources = ResourceState(energy = 6, maxEnergy = 6, regenPerRound = 4)
        )
    }
}
