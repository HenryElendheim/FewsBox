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

    // The rainbow roster. Hidden colors (Pink, Black, White) and the
    // grayscale bosses live in PLAN.md until they're built.

    val RED = HeroDef(
        id = "hero_red", name = "Red", iconId = "ic_hero_red",
        maxHp = 55, baseAttack = 11,
        weaponIds = listOf("wpn_red_maul", "wpn_red_twin", "wpn_red_guillotine"),
        offhandIds = listOf("off_spiked_shield", "off_tower_shield", "off_banner", "off_detonator", "off_cleanser"),
        defaultWeaponId = "wpn_red_maul", defaultOffhandId = "off_spiked_shield",
        ultimateId = "ult_red"
    )

    val ORANGE = HeroDef(
        id = "hero_orange", name = "Orange", iconId = "ic_hero_orange",
        maxHp = 42, baseAttack = 10,
        weaponIds = listOf("wpn_orange_brand", "wpn_orange_whip", "wpn_orange_fan"),
        offhandIds = listOf("off_detonator", "off_medkit", "off_spiked_shield", "off_banner", "off_cleanser"),
        defaultWeaponId = "wpn_orange_brand", defaultOffhandId = "off_detonator",
        ultimateId = "ult_orange"
    )

    val YELLOW = HeroDef(
        id = "hero_yellow", name = "Yellow", iconId = "ic_hero_yellow",
        maxHp = 48, baseAttack = 8,
        weaponIds = listOf("wpn_yellow_siphon", "wpn_yellow_lance", "wpn_yellow_bell"),
        offhandIds = listOf("off_medkit", "off_cleanser", "off_tower_shield", "off_banner", "off_spiked_shield"),
        defaultWeaponId = "wpn_yellow_siphon", defaultOffhandId = "off_medkit",
        ultimateId = "ult_yellow"
    )

    val GREEN = HeroDef(
        id = "hero_green", name = "Green", iconId = "ic_hero_green",
        maxHp = 45, baseAttack = 9,
        weaponIds = listOf("wpn_green_fan", "wpn_green_volley", "wpn_green_scythe"),
        offhandIds = listOf("off_cleanser", "off_medkit", "off_tower_shield", "off_spiked_shield", "off_detonator"),
        defaultWeaponId = "wpn_green_fan", defaultOffhandId = "off_cleanser",
        ultimateId = "ult_green"
    )

    val BLUE = HeroDef(
        id = "hero_blue", name = "Blue", iconId = "ic_hero_blue",
        maxHp = 60, baseAttack = 8,
        weaponIds = listOf("wpn_blue_hammer", "wpn_blue_pike", "wpn_blue_undertow"),
        offhandIds = listOf("off_tower_shield", "off_banner", "off_spiked_shield", "off_medkit", "off_detonator"),
        defaultWeaponId = "wpn_blue_hammer", defaultOffhandId = "off_tower_shield",
        ultimateId = "ult_blue"
    )

    val VIOLET = HeroDef(
        id = "hero_violet", name = "Violet", iconId = "ic_hero_violet",
        maxHp = 40, baseAttack = 10,
        weaponIds = listOf("wpn_violet_reaper", "wpn_violet_fang", "wpn_violet_needle"),
        offhandIds = listOf("off_detonator", "off_cleanser", "off_banner", "off_medkit", "off_tower_shield"),
        defaultWeaponId = "wpn_violet_reaper", defaultOffhandId = "off_detonator",
        ultimateId = "ult_violet"
    )

    // Grayscale defectors: beaten as bosses, then playable. Not in ROSTER —
    // the UI adds them as their battles fall.
    val ASH = HeroDef(
        id = "hero_ash", name = "Ash", iconId = "ic_hero_ash",
        maxHp = 50, baseAttack = 9,
        weaponIds = listOf("wpn_ash_cinder", "wpn_ash_smoke", "wpn_ash_veil"),
        offhandIds = listOf("off_detonator", "off_cleanser", "off_spiked_shield", "off_medkit", "off_tower_shield"),
        defaultWeaponId = "wpn_ash_cinder", defaultOffhandId = "off_detonator",
        ultimateId = "ult_ash"
    )

    val SILVER = HeroDef(
        id = "hero_silver", name = "Silver", iconId = "ic_hero_silver",
        maxHp = 70, baseAttack = 10,
        weaponIds = listOf("wpn_silver_edge", "wpn_silver_lash", "wpn_silver_spike"),
        offhandIds = listOf("off_tower_shield", "off_spiked_shield", "off_banner", "off_detonator", "off_cleanser"),
        defaultWeaponId = "wpn_silver_edge", defaultOffhandId = "off_tower_shield",
        ultimateId = "ult_silver"
    )

    val ROSTER = listOf(RED, ORANGE, YELLOW, GREEN, BLUE, VIOLET)

    val DEFAULT_PARTY_IDS = setOf("hero_red", "hero_green", "hero_blue")

    const val ASH_ID = "hero_ash"
    const val SILVER_ID = "hero_silver"

    // Campaign order: Ash falls first, Silver last. Gray never joins.
    val UNLOCKABLES = listOf(ASH, SILVER)
    val UNLOCKABLE_IDS = UNLOCKABLES.map { it.id }

    fun loadoutFor(heroId: String): Loadout =
        defaultLoadout((ROSTER + UNLOCKABLES).first { it.id == heroId })

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

fun Loadout.toUnit(level: Int = 1): CombatUnit {
    val hp = hero.maxHp + Progression.bonusHp(level)
    return CombatUnit(
        id = hero.id,
        name = hero.name,
        iconId = hero.iconId,
        maxHp = hp,
        hp = hp,
        team = Team.PLAYER,
        baseAttack = hero.baseAttack + Progression.bonusAttack(level) + weapon.attackBonus,
        abilities = buildAbilities(weapon, offhand, extra = listOf(Ultimates.REGISTRY.getValue(hero.ultimateId))),
        ultimateId = hero.ultimateId
    )
}

/**
 * The starter battle sequence: composition difficulty ramps, culminating in
 * a mixed line with an elite behind trash.
 */
object Battles {

    // The 25-level campaign as composition codes. Codes: g grunt, st stinger,
    // sh shaman, br brute, hx hexer, ASH and SILVER the bosses. Difficulty
    // ramps by composition; heroes ramp by levels and unlocked gear.
    private val compositions = listOf(
        "g g g",                //  1: learn to tap
        "g st st g",            //  2: poison shows up
        "sh g g sh",            //  3: healers make you pick targets
        "hx st st",             //  4: first telegraph
        "g g g g",              //  5: volume
        "br g sh st",           //  6: the Brute behind a line
        "st st sh sh",          //  7: sustain wall
        "br hx st",             //  8: two telegraphs at once
        "g g st st sh",         //  9: the long line
        "br sh sh hx",          // 10: elites behind healers
        "hx hx g",              // 11: doom on two clocks
        "st ASH st",            // 12: ASH, the smothering boss
        "br br",                // 13: twin walls
        "hx sh sh g",           // 14: protected doom
        "g g g g g",            // 15: the horde
        "br hx sh",             // 16: full spread
        "st st st hx",          // 17: poison rain under a clock
        "br br st st",          // 18: walls and stingers
        "hx hx sh",             // 19: double doom, healed
        "br sh br",             // 20: the vice
        "hx br hx",             // 21: three clocks
        "br br sh sh",          // 22: the long grind
        "hx hx hx",             // 23: every clock at once
        "sh br hx sh",          // 24: the last gauntlet
        "sh SILVER sh"          // 25: SILVER, the finale
    )

    private fun build(code: String, slot: Int): CombatUnit {
        val id = "enemy_$slot"
        return when (code) {
            "g" -> Enemies.grunt(id)
            "st" -> Enemies.stinger(id)
            "sh" -> Enemies.shaman(id)
            "br" -> Enemies.brute(id)
            "hx" -> Enemies.hexer(id)
            "ASH" -> Enemies.ash(id)
            "SILVER" -> Enemies.silver(id)
            else -> error("unknown enemy code $code")
        }
    }

    val count = compositions.size

    // Clearing these battles turns their boss to your side.
    val unlocks: Map<Int, String> = mapOf(
        11 to Party.ASH_ID,
        24 to Party.SILVER_ID
    )

    fun create(index: Int, party: List<Loadout>, heroLevels: Map<String, Int> = emptyMap()): BattleState {
        val players = party.map { it.toUnit(heroLevels[it.hero.id] ?: 1) }
        val safe = index.coerceIn(0, compositions.lastIndex)
        val enemies = compositions[safe].split(" ")
            .mapIndexed { slot, code -> build(code, slot + 1) }
        return BattleState(units = players + enemies)
    }
}
