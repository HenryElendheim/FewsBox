package com.elendheim.fewsbox.ui

import android.content.Context
import com.elendheim.fewsbox.data.Battles
import com.elendheim.fewsbox.data.Loadout
import com.elendheim.fewsbox.data.Offhands
import com.elendheim.fewsbox.data.Party
import com.elendheim.fewsbox.data.Weapons

/**
 * Dead-simple persistence: battle progress, party pick, unlocked heroes and
 * every hero's equipment go into SharedPreferences. Everything loaded is
 * validated against current content, so removed or renamed gear falls back
 * to the hero's defaults instead of crashing an old save.
 */
object SaveStore {

    private const val PREFS = "fewsbox_save"
    private const val KEY_BATTLE = "battle_index"
    private const val KEY_PARTY = "party_ids"
    private const val KEY_UNLOCKED = "unlocked_heroes"

    data class SaveData(
        val battleIndex: Int,
        val selectedIds: Set<String>,
        val roster: List<Loadout>,
        val unlockedIds: Set<String>
    )

    fun load(context: Context): SaveData {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        val battleIndex = prefs.getInt(KEY_BATTLE, 0).coerceIn(0, Battles.count - 1)

        val unlocked = (prefs.getStringSet(KEY_UNLOCKED, null) ?: emptySet())
            .filter { it == Party.SILVER_ID }  // only known unlockables survive
            .toSet()

        val baseRoster = Party.rosterDefaults() +
            if (Party.SILVER_ID in unlocked) listOf(Party.silverLoadout()) else emptyList()

        val roster = baseRoster.map { loadout ->
            val weaponId = prefs.getString("${loadout.hero.id}.weapon", null)
            val offhandId = prefs.getString("${loadout.hero.id}.offhand", null)
            loadout.copy(
                weapon = weaponId?.takeIf { it in loadout.hero.weaponIds }
                    ?.let { Weapons.REGISTRY.getValue(it) } ?: loadout.weapon,
                offhand = offhandId?.takeIf { it in loadout.hero.offhandIds }
                    ?.let { Offhands.REGISTRY.getValue(it) } ?: loadout.offhand
            )
        }

        val saved = prefs.getStringSet(KEY_PARTY, null) ?: Party.DEFAULT_PARTY_IDS
        val selected = saved
            .filter { id -> roster.any { it.hero.id == id } }
            .take(Party.MAX_SIZE)
            .toSet()
            .ifEmpty { Party.DEFAULT_PARTY_IDS }

        return SaveData(battleIndex, selected, roster, unlocked)
    }

    fun save(
        context: Context,
        battleIndex: Int,
        selectedIds: Set<String>,
        roster: List<Loadout>,
        unlockedIds: Set<String>
    ) {
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        editor.putInt(KEY_BATTLE, battleIndex)
        editor.putStringSet(KEY_PARTY, selectedIds)
        editor.putStringSet(KEY_UNLOCKED, unlockedIds)
        for (loadout in roster) {
            editor.putString("${loadout.hero.id}.weapon", loadout.weapon.id)
            editor.putString("${loadout.hero.id}.offhand", loadout.offhand.id)
        }
        editor.apply()
    }
}
