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
        weaponIds = listOf("wpn_red_sword", "wpn_red_maul", "wpn_red_cleaver", "wpn_red_axe", "wpn_red_pulse"),
        offhandIds = listOf("off_red_buckler", "off_red_vial", "off_red_protector", "off_red_pest", "off_red_warcry"),
        defaultWeaponId = "wpn_red_sword", defaultOffhandId = "off_red_buckler",
        ultimateId = "ult_red"
    )

    val ORANGE = HeroDef(
        id = "hero_orange", name = "Orange", iconId = "ic_hero_orange",
        maxHp = 42, baseAttack = 10,
        weaponIds = listOf("wpn_orange_ember", "wpn_orange_torches", "wpn_orange_whip", "wpn_orange_fan", "wpn_orange_blaze"),
        offhandIds = listOf("off_orange_embergain", "off_orange_matchstick", "off_orange_smokescreen", "off_orange_anger", "off_orange_skies"),
        defaultWeaponId = "wpn_orange_ember", defaultOffhandId = "off_orange_embergain",
        ultimateId = "ult_orange"
    )

    val YELLOW = HeroDef(
        id = "hero_yellow", name = "Yellow", iconId = "ic_hero_yellow",
        maxHp = 48, baseAttack = 8,
        weaponIds = listOf("wpn_yellow_siphon", "wpn_yellow_bell", "wpn_yellow_lance", "wpn_yellow_lifeline", "wpn_yellow_karma"),
        offhandIds = listOf("off_yellow_medkit", "off_yellow_light", "off_yellow_overflow", "off_yellow_kisses", "off_yellow_sunrise"),
        defaultWeaponId = "wpn_yellow_siphon", defaultOffhandId = "off_yellow_medkit",
        ultimateId = "ult_yellow"
    )

    val GREEN = HeroDef(
        id = "hero_green", name = "Green", iconId = "ic_hero_green",
        maxHp = 45, baseAttack = 9,
        weaponIds = listOf("wpn_green_fan", "wpn_green_volley", "wpn_green_scythe", "wpn_green_tangle", "wpn_green_blast"),
        offhandIds = listOf("off_green_quickstep", "off_green_sender", "off_green_mirror", "off_green_boon", "off_green_bark"),
        defaultWeaponId = "wpn_green_fan", defaultOffhandId = "off_green_quickstep",
        ultimateId = "ult_green"
    )

    val BLUE = HeroDef(
        id = "hero_blue", name = "Blue", iconId = "ic_hero_blue",
        maxHp = 60, baseAttack = 8,
        weaponIds = listOf("wpn_blue_hammer", "wpn_blue_anchor", "wpn_blue_pike", "wpn_blue_undertow", "wpn_blue_breakwater"),
        offhandIds = listOf("off_blue_tower", "off_blue_spilled", "off_blue_wall", "off_blue_current", "off_blue_weight"),
        defaultWeaponId = "wpn_blue_hammer", defaultOffhandId = "off_blue_tower",
        ultimateId = "ult_blue"
    )

    val VIOLET = HeroDef(
        id = "hero_violet", name = "Violet", iconId = "ic_hero_violet",
        maxHp = 40, baseAttack = 10,
        weaponIds = listOf("wpn_violet_needle", "wpn_violet_fang", "wpn_violet_reaper", "wpn_violet_gravebind", "wpn_violet_nightfall"),
        offhandIds = listOf("off_violet_cloak", "off_violet_payback", "off_violet_omen", "off_violet_lastlaugh", "off_violet_thief"),
        defaultWeaponId = "wpn_violet_needle", defaultOffhandId = "off_violet_cloak",
        ultimateId = "ult_violet"
    )

    // Grayscale defectors: beaten as bosses, then playable with the kit
    // they fought you with. Their offhands borrow a kindred color's set.
    val ASH = HeroDef(
        id = "hero_ash", name = "Ash", iconId = "ic_hero_ash",
        maxHp = 50, baseAttack = 9,
        weaponIds = listOf("wpn_ash_cinder", "wpn_ash_smoke", "wpn_ash_veil"),
        offhandIds = listOf("off_orange_embergain", "off_orange_matchstick", "off_orange_smokescreen", "off_orange_anger", "off_orange_skies"),
        defaultWeaponId = "wpn_ash_cinder", defaultOffhandId = "off_orange_embergain",
        ultimateId = "ult_ash"
    )

    val SILVER = HeroDef(
        id = "hero_silver", name = "Silver", iconId = "ic_hero_silver",
        maxHp = 70, baseAttack = 10,
        weaponIds = listOf("wpn_silver_edge", "wpn_silver_lash", "wpn_silver_spike"),
        offhandIds = listOf("off_blue_tower", "off_blue_spilled", "off_blue_wall", "off_blue_current", "off_blue_weight"),
        defaultWeaponId = "wpn_silver_edge", defaultOffhandId = "off_blue_tower",
        ultimateId = "ult_silver"
    )

    // You start with Red alone; every other color is earned in order.
    val ROSTER = listOf(RED)

    val DEFAULT_PARTY_IDS = setOf("hero_red")

    const val ORANGE_ID = "hero_orange"
    const val YELLOW_ID = "hero_yellow"
    const val GREEN_ID = "hero_green"
    const val BLUE_ID = "hero_blue"
    const val VIOLET_ID = "hero_violet"
    const val ASH_ID = "hero_ash"
    const val SILVER_ID = "hero_silver"

    // Unlock order: the early campaign hands out the rainbow, the bosses
    // hand out their prisoners and themselves. Gray never joins.
    val UNLOCKABLES = listOf(ORANGE, YELLOW, GREEN, BLUE, VIOLET, ASH, SILVER)
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

    // Hand-tuned opening stretch, sized for a roster that grows as you go:
    // Red fights alone first, and the line thickens as colors join.
    // Codes: g grunt, st stinger, sh shaman, hx hexer, br brute.
    private val openers = listOf(
        "g g",           //  1: Red, alone, learns to drag
        "g g g",         //  2: volume; Orange joins after this
        "g st g",        //  3: poison shows up
        "sh g g",        //  4: healers make you pick targets
        "g st st g",     //  5: the long line; Yellow joins after this
        "hx st st",      //  6: first telegraph
        "g g g g",       //  7: volume again
        "br g sh",       //  8: the Brute behind a line; Green joins after
        "st st sh sh",   //  9: sustain wall
        "br hx st",      // 10: two telegraphs at once
        "g g st st sh",  // 11: the wide line
        "br sh sh hx"    // 12: elites behind healers; Blue joins after
    )

    // Boss levels (0-based). Ash guards Violet's cage at 25, comes back
    // harder at 50 and defects when he falls again. Silver holds 75, then
    // stands with Gray at the end — and joins you once the campaign is won.
    const val WARDEN_BOSS_INDEX = 24
    const val ASH_BOSS_INDEX = 49
    const val SILVER_BOSS_INDEX = 74
    const val FINAL_BOSS_INDEX = 99

    // What beating each set piece frees. Gray never joins.
    val unlocks: Map<Int, String> = mapOf(
        1 to Party.ORANGE_ID,
        4 to Party.YELLOW_ID,
        7 to Party.GREEN_ID,
        11 to Party.BLUE_ID,
        WARDEN_BOSS_INDEX to Party.VIOLET_ID,
        ASH_BOSS_INDEX to Party.ASH_ID,
        FINAL_BOSS_INDEX to Party.SILVER_ID
    )

    fun isBossLevel(index: Int) =
        index == WARDEN_BOSS_INDEX || index == ASH_BOSS_INDEX ||
            index == SILVER_BOSS_INDEX || index == FINAL_BOSS_INDEX

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
        // Ash the warden, holding Violet's cage.
        index == WARDEN_BOSS_INDEX -> listOf(
            scaled(Enemies.stinger("enemy_1"), 1.6, 1.2),
            scaled(Enemies.ash("enemy_2"), 1.3, 1.1),
            scaled(Enemies.stinger("enemy_3"), 1.6, 1.2)
        )
        // Ash again, angrier — beat him twice and he switches sides.
        index == ASH_BOSS_INDEX -> listOf(
            scaled(Enemies.shaman("enemy_1"), 2.2, 1.5),
            scaled(Enemies.ash("enemy_2"), 2.0, 1.4),
            scaled(Enemies.shaman("enemy_3"), 2.2, 1.5)
        )
        index == SILVER_BOSS_INDEX -> listOf(
            scaled(Enemies.shaman("enemy_1"), 2.6, 1.7),
            scaled(Enemies.silver("enemy_2"), 1.9, 1.3),
            scaled(Enemies.shaman("enemy_3"), 2.6, 1.7)
        )
        index == FINAL_BOSS_INDEX -> when (stage) {
            0 -> listOf(
                scaled(Enemies.silver("enemy_1"), 1.6, 1.2),
                Enemies.gray("enemy_2", phase = 1)
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
        if (index < openers.size && stage == 0) {
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
