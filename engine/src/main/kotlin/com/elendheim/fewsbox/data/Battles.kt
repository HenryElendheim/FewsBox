package com.elendheim.fewsbox.data

import com.elendheim.fewsbox.engine.model.BattleState
import com.elendheim.fewsbox.engine.model.CombatUnit
import com.elendheim.fewsbox.engine.model.Team

/**
 * A hero on the roster. Each color carries its own three signature weapons
 * that nobody else can use, plus a restricted slice of the shared offhands —
 * kits read as personality, not shopping.
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
    val defaultOffhandId: String,
    val ultimateId: String
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

    private val allOffhands = Offhands.ALL.map { it.id }

    // The rainbow roster. Hidden colors (Pink, Black, White) and the
    // grayscale bosses live in PLAN.md until they're built.

    val RED = HeroDef(
        id = "hero_red", name = "Red", iconId = "ic_hero_red",
        maxHp = 55, baseAttack = 11,
        weaponIds = listOf("wpn_red_maul", "wpn_red_twin", "wpn_red_guillotine"),
        offhandIds = allOffhands - "off_medkit",
        defaultWeaponId = "wpn_red_maul", defaultOffhandId = "off_spiked_shield",
        ultimateId = "ult_red"
    )

    val ORANGE = HeroDef(
        id = "hero_orange", name = "Orange", iconId = "ic_hero_orange",
        maxHp = 42, baseAttack = 10,
        weaponIds = listOf("wpn_orange_brand", "wpn_orange_whip", "wpn_orange_fan"),
        offhandIds = allOffhands - "off_tower_shield",
        defaultWeaponId = "wpn_orange_brand", defaultOffhandId = "off_detonator",
        ultimateId = "ult_orange"
    )

    val YELLOW = HeroDef(
        id = "hero_yellow", name = "Yellow", iconId = "ic_hero_yellow",
        maxHp = 48, baseAttack = 8,
        weaponIds = listOf("wpn_yellow_siphon", "wpn_yellow_lance", "wpn_yellow_bell"),
        offhandIds = allOffhands - "off_detonator",
        defaultWeaponId = "wpn_yellow_siphon", defaultOffhandId = "off_medkit",
        ultimateId = "ult_yellow"
    )

    val GREEN = HeroDef(
        id = "hero_green", name = "Green", iconId = "ic_hero_green",
        maxHp = 45, baseAttack = 9,
        weaponIds = listOf("wpn_green_fan", "wpn_green_volley", "wpn_green_scythe"),
        offhandIds = allOffhands - "off_banner",
        defaultWeaponId = "wpn_green_fan", defaultOffhandId = "off_cleanser",
        ultimateId = "ult_green"
    )

    val BLUE = HeroDef(
        id = "hero_blue", name = "Blue", iconId = "ic_hero_blue",
        maxHp = 60, baseAttack = 8,
        weaponIds = listOf("wpn_blue_hammer", "wpn_blue_pike", "wpn_blue_undertow"),
        offhandIds = allOffhands - "off_cleanser",
        defaultWeaponId = "wpn_blue_hammer", defaultOffhandId = "off_tower_shield",
        ultimateId = "ult_blue"
    )

    val VIOLET = HeroDef(
        id = "hero_violet", name = "Violet", iconId = "ic_hero_violet",
        maxHp = 40, baseAttack = 10,
        weaponIds = listOf("wpn_violet_reaper", "wpn_violet_fang", "wpn_violet_needle"),
        offhandIds = allOffhands - "off_spiked_shield",
        defaultWeaponId = "wpn_violet_reaper", defaultOffhandId = "off_detonator",
        ultimateId = "ult_violet"
    )

    // The first grayscale defector: beaten as the campaign boss, then
    // playable. Not in ROSTER — the UI adds Silver once unlocked.
    val SILVER = HeroDef(
        id = "hero_silver", name = "Silver", iconId = "ic_hero_silver",
        maxHp = 70, baseAttack = 10,
        weaponIds = listOf("wpn_silver_edge", "wpn_silver_lash", "wpn_silver_spike"),
        offhandIds = allOffhands - "off_medkit",
        defaultWeaponId = "wpn_silver_edge", defaultOffhandId = "off_tower_shield",
        ultimateId = "ult_silver"
    )

    val ROSTER = listOf(RED, ORANGE, YELLOW, GREEN, BLUE, VIOLET)

    val DEFAULT_PARTY_IDS = setOf("hero_red", "hero_green", "hero_blue")

    const val SILVER_ID = "hero_silver"

    fun silverLoadout() = defaultLoadout(SILVER)

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
    abilities = buildAbilities(weapon, offhand, extra = listOf(Ultimates.REGISTRY.getValue(hero.ultimateId))),
    ultimateId = hero.ultimateId
)

/**
 * The starter battle sequence: composition difficulty ramps, culminating in
 * a mixed line with an elite behind trash.
 */
object Battles {

    private val setups: List<() -> List<CombatUnit>> = listOf(
        // 1: learn to tap
        {
            listOf(Enemies.grunt("enemy_1"), Enemies.grunt("enemy_2"), Enemies.grunt("enemy_3"))
        },
        // 2: poison shows up
        {
            listOf(
                Enemies.grunt("enemy_1"), Enemies.stinger("enemy_2"),
                Enemies.stinger("enemy_3"), Enemies.grunt("enemy_4")
            )
        },
        // 3: healers make you pick targets
        {
            listOf(
                Enemies.shaman("enemy_1"), Enemies.grunt("enemy_2"),
                Enemies.grunt("enemy_3"), Enemies.shaman("enemy_4")
            )
        },
        // 4: first telegraph
        {
            listOf(Enemies.hexer("enemy_1"), Enemies.stinger("enemy_2"), Enemies.stinger("enemy_3"))
        },
        // 5: the Brute behind a line
        {
            listOf(
                Enemies.brute("enemy_1"), Enemies.grunt("enemy_2"),
                Enemies.shaman("enemy_3"), Enemies.stinger("enemy_4")
            )
        },
        // 6: two telegraphs at once - who do you stun?
        {
            listOf(Enemies.brute("enemy_1"), Enemies.hexer("enemy_2"), Enemies.stinger("enemy_3"))
        },
        // 7: elites behind healers
        {
            listOf(
                Enemies.brute("enemy_1"), Enemies.shaman("enemy_2"),
                Enemies.shaman("enemy_3"), Enemies.hexer("enemy_4")
            )
        },
        // 8: SILVER. The first boss, guarded by healers.
        {
            listOf(
                Enemies.shaman("enemy_1"), Enemies.silver("enemy_2"), Enemies.shaman("enemy_3")
            )
        }
    )

    val count = setups.size

    fun create(index: Int, party: List<Loadout>): BattleState {
        val players = party.map { it.toUnit() }
        val enemies = setups[index.coerceIn(0, setups.lastIndex)]()
        return BattleState(units = players + enemies)
    }
}
