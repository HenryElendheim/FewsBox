package com.elendheim.fewsbox.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Player-facing settings, held as observable state so every screen reacts
 * the moment a toggle flips. Persisted alongside the save.
 */
object Prefs {
    var reduceMotion by mutableStateOf(false)   // no screen shake, no lunges
    var bigNumbers by mutableStateOf(false)     // larger combat numbers
    var highContrast by mutableStateOf(false)   // brighter text on dark panels
    var slowEnemies by mutableStateOf(false)    // longer beats between enemy turns
    var tutorialDone by mutableStateOf(false)
    var shopHintSeen by mutableStateOf(false)

    private const val PREFS = "fewsbox_save"

    fun load(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        reduceMotion = p.getBoolean("pref_reduce_motion", false)
        bigNumbers = p.getBoolean("pref_big_numbers", false)
        highContrast = p.getBoolean("pref_high_contrast", false)
        slowEnemies = p.getBoolean("pref_slow_enemies", false)
        tutorialDone = p.getBoolean("pref_tutorial_done", false)
        shopHintSeen = p.getBoolean("pref_shop_hint_seen", false)
    }

    fun save(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("pref_reduce_motion", reduceMotion)
            .putBoolean("pref_big_numbers", bigNumbers)
            .putBoolean("pref_high_contrast", highContrast)
            .putBoolean("pref_slow_enemies", slowEnemies)
            .putBoolean("pref_tutorial_done", tutorialDone)
            .putBoolean("pref_shop_hint_seen", shopHintSeen)
            .apply()
    }
}
