package com.elendheim.fewsbox.ui

import androidx.compose.ui.graphics.Color
import com.elendheim.fewsbox.ui.theme.Accent
import com.elendheim.fewsbox.ui.theme.EnergyGold
import com.elendheim.fewsbox.ui.theme.HpGreen
import com.elendheim.fewsbox.ui.theme.HpLow
import com.elendheim.fewsbox.ui.theme.ShieldBlue
import com.elendheim.fewsbox.ui.theme.DangerRed

/**
 * Placeholder icon system: a short glyph on a tinted chip per iconId. Real
 * art replaces this lookup later — nothing else changes, because the engine
 * only ever hands the UI an iconId string.
 */
data class IconSpec(val glyph: String, val tint: Color)

private val Violet = Color(0xFFB68CFF)
private val Amber = Color(0xFFE8A33D)
private val Teal = Color(0xFF4ECDC4)
private val TealDim = Color(0xFF6FA8A3)
private val OrangeHero = Color(0xFFE8823D)
private val SilverHero = Color(0xFFF7F7F7)

object GameIcons {

    private val map = mapOf(
        // Enemies are grayscale, always. Color belongs to the heroes.
        "ic_enemy_grunt" to IconSpec("GR", Color(0xFFB0B0B0)),
        "ic_enemy_stinger" to IconSpec("ST", Color(0xFFD6D6D6)),
        "ic_enemy_shaman" to IconSpec("SH", Color(0xFF8A8A8A)),
        "ic_enemy_brute" to IconSpec("BR", Color(0xFFEDEDED)),
        "ic_enemy_hexer" to IconSpec("HX", Color(0xFF6E6E6E)),
        "ic_enemy_silver" to IconSpec("SV", Color(0xFFF7F7F7)),
        "ic_atk_storm" to IconSpec("SS", Color(0xFFF7F7F7)),

        // Signature weapons (one entry serves weapon chip + ability button)
        "ic_wpn_red_maul" to IconSpec("MA", DangerRed),
        "ic_wpn_red_twin" to IconSpec("TW", DangerRed),
        "ic_wpn_red_guillotine" to IconSpec("GU", DangerRed),
        "ic_wpn_orange_brand" to IconSpec("BR", OrangeHero),
        "ic_wpn_orange_whip" to IconSpec("WH", OrangeHero),
        "ic_wpn_orange_fan" to IconSpec("FN", OrangeHero),
        "ic_wpn_yellow_siphon" to IconSpec("SI", EnergyGold),
        "ic_wpn_yellow_lance" to IconSpec("LA", EnergyGold),
        "ic_wpn_yellow_bell" to IconSpec("BE", EnergyGold),
        "ic_wpn_green_fan" to IconSpec("FN", HpGreen),
        "ic_wpn_green_volley" to IconSpec("VO", HpGreen),
        "ic_wpn_green_scythe" to IconSpec("SC", HpGreen),
        "ic_wpn_blue_hammer" to IconSpec("HA", ShieldBlue),
        "ic_wpn_blue_pike" to IconSpec("PK", ShieldBlue),
        "ic_wpn_blue_undertow" to IconSpec("UN", ShieldBlue),
        "ic_wpn_violet_reaper" to IconSpec("RE", Violet),
        "ic_wpn_violet_fang" to IconSpec("FG", Violet),
        "ic_wpn_violet_needle" to IconSpec("NE", Violet),
        "ic_wpn_silver_edge" to IconSpec("ED", SilverHero),
        "ic_wpn_silver_lash" to IconSpec("LS", SilverHero),
        "ic_wpn_silver_spike" to IconSpec("SP", SilverHero),

        // Offhands and their abilities
        "ic_off_tower" to IconSpec("TW", ShieldBlue),
        "ic_off_spiked" to IconSpec("SP", ShieldBlue),
        "ic_off_medkit" to IconSpec("MD", HpGreen),
        "ic_off_banner" to IconSpec("BN", EnergyGold),
        "ic_off_detonator" to IconSpec("DT", Amber),
        "ic_off_cleanser" to IconSpec("CN", Teal),
        "ic_def_tower" to IconSpec("TW", ShieldBlue),
        "ic_def_spiked" to IconSpec("SP", ShieldBlue),
        "ic_def_medkit" to IconSpec("MD", HpGreen),
        "ic_def_banner" to IconSpec("BN", EnergyGold),
        "ic_def_detonate" to IconSpec("DT", Amber),
        "ic_def_cleanse" to IconSpec("CN", Teal),

        // Ultimates, tinted to their hero
        "ic_ult_red" to IconSpec("UL", Color(0xFFE5484D)),
        "ic_ult_orange" to IconSpec("UL", Color(0xFFE8823D)),
        "ic_ult_yellow" to IconSpec("UL", Color(0xFFFFD166)),
        "ic_ult_green" to IconSpec("UL", Color(0xFF6FCF97)),
        "ic_ult_blue" to IconSpec("UL", Color(0xFF4AA3FF)),
        "ic_ult_violet" to IconSpec("UL", Color(0xFFB68CFF)),
        "ic_ult_silver" to IconSpec("UL", Color(0xFFF7F7F7)),

        // Statuses
        "ic_status_burn" to IconSpec("B", Amber),
        "ic_status_poison" to IconSpec("P", HpGreen),
        "ic_status_stun" to IconSpec("Z", EnergyGold),
        "ic_status_weaken" to IconSpec("W", TealDim),
        "ic_status_vulnerable" to IconSpec("V", DangerRed),
        "ic_status_taunt" to IconSpec("T", EnergyGold),
        "ic_status_thorns" to IconSpec("TH", ShieldBlue),

        // Enemy move icons (shown nowhere yet, defined so lookups never miss)
        "ic_atk_basic" to IconSpec("AT", DangerRed),
        "ic_def_small" to IconSpec("GD", ShieldBlue),
        "ic_def_heal_s" to IconSpec("HL", HpGreen),
        "ic_atk_venom" to IconSpec("VN", HpGreen),
        "ic_atk_crush" to IconSpec("CR", DangerRed),
        "ic_atk_doom" to IconSpec("DM", Violet)
    )

    operator fun get(iconId: String): IconSpec = map[iconId] ?: IconSpec("?", Accent)

    /**
     * Heroes render as solid color blocks, not glyph chips — the color IS
     * the character. Enemies keep lettered chips, strictly grayscale.
     */
    private val heroColors = mapOf(
        "ic_hero_red" to Color(0xFFE5484D),
        "ic_hero_orange" to Color(0xFFE8823D),
        "ic_hero_yellow" to Color(0xFFFFD166),
        "ic_hero_green" to Color(0xFF6FCF97),
        "ic_hero_blue" to Color(0xFF4AA3FF),
        "ic_hero_violet" to Color(0xFFB68CFF),
        // The defector. Grayscale on your side of the field.
        "ic_hero_silver" to Color(0xFFF7F7F7)
    )

    fun heroColor(iconId: String): Color? = heroColors[iconId]
}
