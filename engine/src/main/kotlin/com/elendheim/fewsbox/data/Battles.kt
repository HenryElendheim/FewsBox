package com.elendheim.fewsbox.data

import com.elendheim.fewsbox.engine.model.BattleState
import com.elendheim.fewsbox.engine.model.CombatUnit
import com.elendheim.fewsbox.engine.model.Team
import kotlin.math.roundToInt
import kotlin.random.Random

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
        abilities = buildAbilities(weapon, offhand, extra = listOf(Ultimates.forLevel(hero.ultimateId, level))),
        ultimateId = hero.ultimateId
    )
}

/**
 * The campaign: 100 levels of generated encounters with handcrafted openers
 * and set-piece boss fights, then endless mode past the end. Levels can span
 * several stages; the party's HP, statuses and ult meter carry across them.
 *
 * Everything is deterministic per (level, stage) so a level plays the same
 * every attempt — retries are about tactics, not slot-machine rolls.
 */
object Battles {

    const val CAMPAIGN_LEVELS = 100

    // Kept as `count` because the app and tests grew up with that name.
    val count = CAMPAIGN_LEVELS

    // Hand-tuned opening stretch so the first sessions teach one thing at a
    // time. Codes: g grunt, st stinger, sh shaman, hx hexer, br brute.
    private val openers = listOf(
        "g g g",         //  1: learn to tap
        "g st st g",     //  2: poison shows up
        "sh g g sh",     //  3: healers make you pick targets
        "hx st st",      //  4: first telegraph
        "g g g g",       //  5: volume
        "br g sh st",    //  6: the Brute behind a line
        "st st sh sh",   //  7: sustain wall
        "br hx st",      //  8: two telegraphs at once
        "g g st st sh"   //  9: the long line
    )

    // Boss levels (0-based): every 25th level is a set piece, level 100 is Gray.
    const val ASH_BOSS_INDEX = 24
    const val SILVER_BOSS_INDEX = 49
    const val TWIN_BOSS_INDEX = 74
    const val FINAL_BOSS_INDEX = 99

    // Clearing these battles turns their boss to your side. Gray never joins.
    val unlocks: Map<Int, String> = mapOf(
        ASH_BOSS_INDEX to Party.ASH_ID,
        SILVER_BOSS_INDEX to Party.SILVER_ID
    )

    fun isBossLevel(index: Int) =
        index == ASH_BOSS_INDEX || index == SILVER_BOSS_INDEX ||
            index == TWIN_BOSS_INDEX || index == FINAL_BOSS_INDEX

    /**
     * How many stages a level runs. Campaign: every 10th level is a gauntlet
     * (mostly 3 stages, up to 5 late), bosses are single set pieces except
     * Gray's three-stage finale. Endless: gauntlets are semi-rare, mostly 3
     * stages, capped at 8 deep in.
     */
    fun stageCountFor(index: Int): Int {
        if (index == FINAL_BOSS_INDEX) return 3
        if (isBossLevel(index)) return 1
        if (index < CAMPAIGN_LEVELS) {
            return when (index) {
                9, 19, 29, 59, 79 -> 3
                39, 69 -> 4
                89 -> 5
                else -> 1
            }
        }
        // Endless: roughly a quarter of levels turn into gauntlets.
        val rng = Random(index * 7919L)
        if (rng.nextInt(100) >= 25) return 1
        val cap = (5 + (index - CAMPAIGN_LEVELS) / 40).coerceAtMost(8)
        return when (rng.nextInt(10)) {
            in 0..5 -> 3
            6 -> 4
            7 -> 5.coerceAtMost(cap)
            8 -> 6.coerceAtMost(cap)
            else -> cap
        }
    }

    // Enemy growth curves. HP climbs faster than attack so late fights are
    // longer and tenser without turning into one-shot roulette.
    private fun hpScale(index: Int, stage: Int) = 1.0 + index * 0.025 + stage * 0.05
    private fun atkScale(index: Int, stage: Int) = 1.0 + index * 0.012 + stage * 0.02

    private fun scaled(unit: CombatUnit, hpFactor: Double, atkFactor: Double): CombatUnit {
        val hp = maxOf(1, (unit.maxHp * hpFactor).roundToInt())
        return unit.copy(
            maxHp = hp, hp = hp,
            baseAttack = maxOf(1, (unit.baseAttack * atkFactor).roundToInt()),
            statuses = mutableListOf(), cooldowns = mutableMapOf()
        )
    }

    private fun build(code: String, slot: Int): CombatUnit {
        val id = "enemy_$slot"
        return when (code) {
            "g" -> Enemies.grunt(id)
            "st" -> Enemies.stinger(id)
            "sh" -> Enemies.shaman(id)
            "br" -> Enemies.brute(id)
            "hx" -> Enemies.hexer(id)
            else -> error("unknown enemy code $code")
        }
    }

    private fun bossStage(index: Int, stage: Int): List<CombatUnit>? = when {
        index == ASH_BOSS_INDEX -> listOf(
            scaled(Enemies.stinger("enemy_1"), 1.6, 1.2),
            scaled(Enemies.ash("enemy_2"), 1.3, 1.1),
            scaled(Enemies.stinger("enemy_3"), 1.6, 1.2)
        )
        index == SILVER_BOSS_INDEX -> listOf(
            scaled(Enemies.shaman("enemy_1"), 2.2, 1.5),
            scaled(Enemies.silver("enemy_2"), 1.5, 1.2),
            scaled(Enemies.shaman("enemy_3"), 2.2, 1.5)
        )
        index == TWIN_BOSS_INDEX -> listOf(
            scaled(Enemies.ash("enemy_1"), 2.0, 1.4),
            scaled(Enemies.silver("enemy_2"), 1.8, 1.3)
        )
        index == FINAL_BOSS_INDEX -> when (stage) {
            0 -> listOf(
                scaled(Enemies.grunt("enemy_1"), 3.0, 2.0),
                Enemies.gray("enemy_2", phase = 1),
                scaled(Enemies.grunt("enemy_3"), 3.0, 2.0)
            )
            1 -> listOf(
                Enemies.gray("enemy_1", phase = 2),
                scaled(Enemies.hexer("enemy_2"), 1.6, 1.3)
            )
            else -> listOf(Enemies.gray("enemy_1", phase = 3))
        }
        else -> null
    }

    private fun generated(index: Int, stage: Int): List<CombatUnit> {
        val rng = Random(index * 7919L + stage * 131L)
        val size = 3 + rng.nextInt(3)
        val maxElites = when {
            index < 5 -> 0
            index < 20 -> 1
            else -> 2
        }
        val eliteChance = (10 + index / 2).coerceAtMost(40)
        var elites = 0
        val hpF = hpScale(index, stage)
        val atkF = atkScale(index, stage)
        return (1..size).map { slot ->
            val id = "enemy_$slot"
            val rollElite = elites < maxElites && rng.nextInt(100) < eliteChance
            val unit = if (rollElite) {
                elites++
                if (index >= 7 && rng.nextBoolean()) Enemies.hexer(id) else Enemies.brute(id)
            } else {
                when (rng.nextInt(3)) {
                    0 -> Enemies.grunt(id)
                    1 -> Enemies.stinger(id)
                    else -> Enemies.shaman(id)
                }
            }
            // Elites already come in big; grow them at a gentler rate.
            if (rollElite) scaled(unit, 1.0 + (hpF - 1.0) * 0.6, atkF)
            else scaled(unit, hpF, atkF)
        }
    }

    fun enemiesFor(index: Int, stage: Int): List<CombatUnit> {
        bossStage(index, stage)?.let { return it }
        if (index < openers.size) {
            return openers[index].split(" ").mapIndexed { slot, code -> build(code, slot + 1) }
        }
        return generated(index, stage)
    }

    /**
     * Build one stage of a level. On stage two and later the survivors walk
     * in as they left the last fight — same HP, shields, statuses and ult
     * meter — so gauntlets are about pacing your resources.
     */
    fun createStage(
        index: Int,
        stage: Int,
        party: List<Loadout>,
        heroLevels: Map<String, Int> = emptyMap(),
        carriedPlayers: List<CombatUnit>? = null,
        carriedUltCharge: Int = 0
    ): BattleState {
        val players = carriedPlayers?.filter { it.isAlive }
            ?: party.map { it.toUnit(heroLevels[it.hero.id] ?: 1) }
        return BattleState(
            units = players + enemiesFor(index, stage),
            partyUltCharge = carriedUltCharge
        )
    }

    fun create(index: Int, party: List<Loadout>, heroLevels: Map<String, Int> = emptyMap()): BattleState =
        createStage(index, 0, party, heroLevels)
}
