package com.elendheim.fewsbox.ui

import android.content.Context
import com.elendheim.fewsbox.data.Battles
import com.elendheim.fewsbox.data.Loadout
import com.elendheim.fewsbox.data.Offhands
import com.elendheim.fewsbox.data.Party
import com.elendheim.fewsbox.data.Progression
import com.elendheim.fewsbox.data.Weapons

/**
 * Persistence: level progress, best stars, party pick, hero XP, unlocked
 * heroes and every hero's equipment. Everything loaded is validated against
 * current content and current hero levels, so stale saves fall back to
 * legal defaults instead of crashing or smuggling locked gear.
 */
object SaveStore {

    private const val PREFS = "fewsbox_save"
    private const val KEY_LEGACY_BATTLE = "battle_index"
    private const val KEY_MAX_UNLOCKED = "max_unlocked"
    private const val KEY_SELECTED = "selected_level"
    private const val KEY_PARTY = "party_ids"
    private const val KEY_UNLOCKED = "unlocked_heroes"
    private const val KEY_ENDLESS_BEST = "endless_best"
    private const val KEY_FEWS = "fews"
    private const val KEY_OWNED_GEAR = "owned_gear"
    private const val KEY_CONSUMABLES = "consumables"

    // Workshop consumable ids; counts persist as "id:count" strings.
    val CONSUMABLE_IDS = listOf("con_spark", "con_bandage", "con_ironskin")

    data class SaveData(
        val maxUnlocked: Int,
        val selectedLevel: Int,
        val bestStars: Map<Int, Int>,
        val heroXp: Map<String, Int>,
        val selectedIds: Set<String>,
        val roster: List<Loadout>,
        val unlockedIds: Set<String>,
        val endlessBest: Int,
        val fews: Int,
        val ownedGear: Set<String>,
        val consumables: Map<String, Int>
    )

    fun load(context: Context): SaveData {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        // Old saves stored a single campaign position; treat it as progress.
        val legacy = prefs.getInt(KEY_LEGACY_BATTLE, 0)
        val maxUnlocked = prefs.getInt(KEY_MAX_UNLOCKED, legacy).coerceIn(0, Battles.count - 1)
        val selectedLevel = prefs.getInt(KEY_SELECTED, maxUnlocked).coerceIn(0, maxUnlocked)

        val bestStars = buildMap {
            for (i in 0 until Battles.count) {
                val stars = prefs.getInt("level_$i.stars", 0)
                if (stars > 0) put(i, stars.coerceIn(1, 3))
            }
        }

        // Heroes come from the save, plus whatever the player's campaign
        // position has already earned (covers saves from older versions).
        val earned = Battles.unlocks
            .filter { (index, _) -> index < maxUnlocked || (bestStars[index] ?: 0) > 0 }
            .values.toSet()
        val unlocked = ((prefs.getStringSet(KEY_UNLOCKED, null) ?: emptySet()) + earned)
            .filter { it in Party.UNLOCKABLE_IDS }
            .toSet()

        val allHeroes = Party.ROSTER + Party.UNLOCKABLES
        val heroXp = allHeroes.associate { it.id to prefs.getInt("${it.id}.xp", 0).coerceAtLeast(0) }

        val baseRoster = Party.rosterDefaults() +
            Party.UNLOCKABLE_IDS.filter { it in unlocked }.map { Party.loadoutFor(it) }

        // Gear legality is ownership now: starters are free, the rest is
        // bought in the shop with fews.
        val ownedGear = (prefs.getStringSet(KEY_OWNED_GEAR, null) ?: emptySet())
            .filter { it in Weapons.REGISTRY || it in Offhands.REGISTRY }
            .toSet()

        val roster = baseRoster.map { loadout ->
            val hero = loadout.hero
            fun owns(id: String) =
                id == hero.defaultWeaponId || id == hero.defaultOffhandId || id in ownedGear
            val weaponId = prefs.getString("${hero.id}.weapon", null)
            val offhandId = prefs.getString("${hero.id}.offhand", null)
            loadout.copy(
                weapon = weaponId?.takeIf { it in hero.weaponIds && owns(it) }
                    ?.let { Weapons.REGISTRY.getValue(it) } ?: loadout.weapon,
                offhand = offhandId?.takeIf { it in hero.offhandIds && owns(it) }
                    ?.let { Offhands.REGISTRY.getValue(it) } ?: loadout.offhand
            )
        }

        val fews = prefs.getInt(KEY_FEWS, 0).coerceAtLeast(0)
        val consumables = (prefs.getStringSet(KEY_CONSUMABLES, null) ?: emptySet())
            .mapNotNull { entry ->
                val parts = entry.split(":")
                val id = parts.getOrNull(0) ?: return@mapNotNull null
                val count = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
                if (id in CONSUMABLE_IDS) id to count.coerceIn(0, 99) else null
            }.toMap()

        val saved = prefs.getStringSet(KEY_PARTY, null) ?: Party.DEFAULT_PARTY_IDS
        val selected = saved
            .filter { id -> roster.any { it.hero.id == id } }
            .take(Party.MAX_SIZE)
            .toSet()
            .ifEmpty { Party.DEFAULT_PARTY_IDS }

        val endlessBest = prefs.getInt(KEY_ENDLESS_BEST, 0).coerceAtLeast(0)

        return SaveData(
            maxUnlocked, selectedLevel, bestStars, heroXp, selected, roster,
            unlocked, endlessBest, fews, ownedGear, consumables
        )
    }

    fun save(
        context: Context,
        maxUnlocked: Int,
        selectedLevel: Int,
        bestStars: Map<Int, Int>,
        heroXp: Map<String, Int>,
        selectedIds: Set<String>,
        roster: List<Loadout>,
        unlockedIds: Set<String>,
        endlessBest: Int,
        fews: Int,
        ownedGear: Set<String>,
        consumables: Map<String, Int>
    ) {
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        editor.putInt(KEY_MAX_UNLOCKED, maxUnlocked)
        editor.putInt(KEY_SELECTED, selectedLevel)
        editor.putInt(KEY_ENDLESS_BEST, endlessBest)
        editor.putInt(KEY_FEWS, fews)
        editor.putStringSet(KEY_OWNED_GEAR, ownedGear)
        editor.putStringSet(KEY_CONSUMABLES, consumables.map { "${it.key}:${it.value}" }.toSet())
        editor.putStringSet(KEY_PARTY, selectedIds)
        editor.putStringSet(KEY_UNLOCKED, unlockedIds)
        for ((level, stars) in bestStars) editor.putInt("level_$level.stars", stars)
        for ((heroId, xp) in heroXp) editor.putInt("$heroId.xp", xp)
        for (loadout in roster) {
            editor.putString("${loadout.hero.id}.weapon", loadout.weapon.id)
            editor.putString("${loadout.hero.id}.offhand", loadout.offhand.id)
        }
        editor.apply()
    }
}
