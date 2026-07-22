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
        // RED weapons
        "ic_wpn_red_sword" to R.drawable.art_wpn_red_sword,
        "ic_wpn_red_maul" to R.drawable.art_wpn_red_maul,
        "ic_wpn_red_cleaver" to R.drawable.art_wpn_red_cleaver,
        "ic_wpn_red_axe" to R.drawable.art_wpn_red_axe,
        "ic_wpn_red_pulse" to R.drawable.art_wpn_red_pulse,
        // RED offhands
        "ic_off_red_buckler" to R.drawable.art_off_red_buckler,
        "ic_off_red_vial" to R.drawable.art_off_red_vial,
        "ic_off_red_protector" to R.drawable.art_off_red_protector,
        "ic_off_red_pest" to R.drawable.art_off_red_pest,
        "ic_off_red_warcry" to R.drawable.art_off_red_warcry,
        // Ability icons share their item's art.
        "ic_def_red_buckler" to R.drawable.art_off_red_buckler,
        "ic_def_red_vial" to R.drawable.art_off_red_vial,
        "ic_def_red_protector" to R.drawable.art_off_red_protector,
        "ic_def_red_pest" to R.drawable.art_off_red_pest,
        "ic_def_red_warcry" to R.drawable.art_off_red_warcry
    )

    operator fun get(iconId: String?): Int? = iconId?.let { map[it] }
}
