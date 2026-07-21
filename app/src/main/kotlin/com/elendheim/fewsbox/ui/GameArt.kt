package com.elendheim.fewsbox.ui

import com.elendheim.fewsbox.R

/**
 * Real art, mapped by the same iconIds the placeholder chips use. Anything
 * listed here renders as an image everywhere; anything missing falls back
 * to its glyph chip. Weapons drawn with the grip in the lower-left also
 * appear in the hero's hand in battle.
 */
object GameArt {

    private val map: Map<String, Int> = mapOf(
        // RED
        "ic_wpn_red_sword" to R.drawable.art_wpn_red_sword,
        "ic_wpn_red_maul" to R.drawable.art_wpn_red_maul,
        "ic_off_red_buckler" to R.drawable.art_off_red_buckler,
        "ic_off_red_vial" to R.drawable.art_off_red_vial,
        // Ability icons share their item's art.
        "ic_def_red_buckler" to R.drawable.art_off_red_buckler,
        "ic_def_red_vial" to R.drawable.art_off_red_vial
    )

    operator fun get(iconId: String?): Int? = iconId?.let { map[it] }
}
