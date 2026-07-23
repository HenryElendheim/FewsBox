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
private val AshHero = Color(0xFFC9C9C9)

object GameIcons {

    private val map = mapOf(
        // Enemies are grayscale, always. Color belongs to the heroes.
        "ic_enemy_grunt" to IconSpec("GR", Color(0xFFB0B0B0)),
        "ic_enemy_stinger" to IconSpec("ST", Color(0xFFD6D6D6)),
        "ic_enemy_shaman" to IconSpec("SH", Color(0xFF8A8A8A)),
        "ic_enemy_brute" to IconSpec("BR", Color(0xFFEDEDED)),
        "ic_enemy_hexer" to IconSpec("HX", Color(0xFF6E6E6E)),
        "ic_enemy_silver" to IconSpec("SV", Color(0xFFF7F7F7)),
        "ic_enemy_ash" to IconSpec("AS", Color(0xFFC9C9C9)),
        "ic_enemy_gray" to IconSpec("GR", Color(0xFF4A4A4A)),
        "ic_atk_null" to IconSpec("NW", Color(0xFF4A4A4A)),
        "ic_atk_ashcloud" to IconSpec("AC", Color(0xFFC9C9C9)),
        "ic_atk_cinder" to IconSpec("CS", Color(0xFFC9C9C9)),
        "ic_def_ember" to IconSpec("EG", Color(0xFFC9C9C9)),
        "ic_atk_storm" to IconSpec("SS", Color(0xFFF7F7F7)),

        // Signature weapons (one entry serves weapon chip + ability button)
        "ic_wpn_red_sword" to IconSpec("SW", DangerRed),
        "ic_wpn_red_maul" to IconSpec("MA", DangerRed),
        "ic_wpn_red_cleaver" to IconSpec("CL", DangerRed),
        "ic_wpn_red_axe" to IconSpec("AX", DangerRed),
        "ic_wpn_red_pulse" to IconSpec("PC", DangerRed),
        "ic_wpn_orange_ember" to IconSpec("EB", OrangeHero),
        "ic_wpn_orange_torches" to IconSpec("TT", OrangeHero),
        "ic_wpn_orange_whip" to IconSpec("WH", OrangeHero),
        "ic_wpn_orange_fan" to IconSpec("FN", OrangeHero),
        "ic_wpn_orange_blaze" to IconSpec("BZ", OrangeHero),
        "ic_wpn_yellow_siphon" to IconSpec("SI", EnergyGold),
        "ic_wpn_yellow_bell" to IconSpec("BE", EnergyGold),
        "ic_wpn_yellow_lance" to IconSpec("LA", EnergyGold),
        "ic_wpn_yellow_lifeline" to IconSpec("LL", EnergyGold),
        "ic_wpn_yellow_karma" to IconSpec("KA", EnergyGold),
        "ic_wpn_green_fan" to IconSpec("FN", HpGreen),
        "ic_wpn_green_volley" to IconSpec("VO", HpGreen),
        "ic_wpn_green_scythe" to IconSpec("SC", HpGreen),
        "ic_wpn_green_tangle" to IconSpec("TG", HpGreen),
        "ic_wpn_green_blast" to IconSpec("LB", HpGreen),
        "ic_wpn_blue_hammer" to IconSpec("HA", ShieldBlue),
        "ic_wpn_blue_anchor" to IconSpec("AN", ShieldBlue),
        "ic_wpn_blue_pike" to IconSpec("PK", ShieldBlue),
        "ic_wpn_blue_undertow" to IconSpec("UN", ShieldBlue),
        "ic_wpn_blue_breakwater" to IconSpec("BW", ShieldBlue),
        "ic_wpn_violet_needle" to IconSpec("NE", Violet),
        "ic_wpn_violet_fang" to IconSpec("FG", Violet),
        "ic_wpn_violet_reaper" to IconSpec("RE", Violet),
        "ic_wpn_violet_gravebind" to IconSpec("GB", Violet),
        "ic_wpn_violet_nightfall" to IconSpec("NF", Violet),
        "ic_wpn_ash_cinder" to IconSpec("CF", AshHero),
        "ic_wpn_ash_smoke" to IconSpec("SM", AshHero),
        "ic_wpn_ash_veil" to IconSpec("GV", AshHero),
        "ic_wpn_silver_edge" to IconSpec("ED", SilverHero),
        "ic_wpn_silver_lash" to IconSpec("LS", SilverHero),
        "ic_wpn_silver_spike" to IconSpec("SP", SilverHero),

        // Offhands and their abilities
        "ic_off_red_buckler" to IconSpec("JB", DangerRed),
        "ic_off_red_vial" to IconSpec("AV", DangerRed),
        "ic_off_red_protector" to IconSpec("PR", DangerRed),
        "ic_off_red_pest" to IconSpec("AP", DangerRed),
        "ic_off_red_warcry" to IconSpec("WC", DangerRed),
        "ic_off_orange_embergain" to IconSpec("EG", OrangeHero),
        "ic_off_orange_matchstick" to IconSpec("MS", OrangeHero),
        "ic_off_orange_smokescreen" to IconSpec("SS", OrangeHero),
        "ic_off_orange_anger" to IconSpec("AM", OrangeHero),
        "ic_off_orange_skies" to IconSpec("SK", OrangeHero),
        "ic_off_yellow_medkit" to IconSpec("MD", EnergyGold),
        "ic_off_yellow_light" to IconSpec("GL", EnergyGold),
        "ic_off_yellow_overflow" to IconSpec("OV", EnergyGold),
        "ic_off_yellow_kisses" to IconSpec("MK", EnergyGold),
        "ic_off_yellow_sunrise" to IconSpec("SR", EnergyGold),
        "ic_off_green_quickstep" to IconSpec("QS", HpGreen),
        "ic_off_green_sender" to IconSpec("RS", HpGreen),
        "ic_off_green_mirror" to IconSpec("MI", HpGreen),
        "ic_off_green_boon" to IconSpec("WB", HpGreen),
        "ic_off_green_bark" to IconSpec("BK", HpGreen),
        "ic_off_blue_tower" to IconSpec("TW", ShieldBlue),
        "ic_off_blue_spilled" to IconSpec("SP", ShieldBlue),
        "ic_off_blue_wall" to IconSpec("WL", ShieldBlue),
        "ic_off_blue_current" to IconSpec("DC", ShieldBlue),
        "ic_off_blue_weight" to IconSpec("CW", ShieldBlue),
        "ic_off_violet_cloak" to IconSpec("SC", Violet),
        "ic_off_violet_payback" to IconSpec("PB", Violet),
        "ic_off_violet_omen" to IconSpec("OM", Violet),
        "ic_off_violet_lastlaugh" to IconSpec("LL", Violet),
        "ic_off_violet_thief" to IconSpec("TF", Violet),

        // Ultimates, tinted to their hero
        "ic_ult_red" to IconSpec("UL", Color(0xFFE5484D)),
        "ic_ult_orange" to IconSpec("UL", Color(0xFFE8823D)),
        "ic_ult_yellow" to IconSpec("UL", Color(0xFFFFD166)),
        "ic_ult_green" to IconSpec("UL", Color(0xFF6FCF97)),
        "ic_ult_blue" to IconSpec("UL", Color(0xFF4AA3FF)),
        "ic_ult_violet" to IconSpec("UL", Color(0xFFB68CFF)),
        "ic_ult_ash" to IconSpec("UL", Color(0xFFC9C9C9)),
        "ic_ult_silver" to IconSpec("UL", Color(0xFFF7F7F7)),

        // Statuses
        "ic_status_burn" to IconSpec("B", Amber),
        "ic_status_poison" to IconSpec("P", HpGreen),
        "ic_status_scorch" to IconSpec("SC", Amber),
        "ic_status_bleed" to IconSpec("BL", DangerRed),
        "ic_status_stun" to IconSpec("Z", EnergyGold),
        "ic_status_lure" to IconSpec("LU", HpGreen),
        "ic_status_weaken" to IconSpec("W", TealDim),
        "ic_status_sunder" to IconSpec("SU", TealDim),
        "ic_status_dull" to IconSpec("DL", TealDim),
        "ic_status_sap" to IconSpec("SA", TealDim),
        "ic_status_vulnerable" to IconSpec("V", DangerRed),
        "ic_status_taunt" to IconSpec("T", EnergyGold),
        "ic_status_thorns" to IconSpec("TH", ShieldBlue),
        "ic_status_war_cry" to IconSpec("WC", DangerRed),
        "ic_status_rally" to IconSpec("RA", DangerRed),
        "ic_status_guard" to IconSpec("GD", ShieldBlue),
        "ic_status_pest_guard" to IconSpec("PG", ShieldBlue),
        "ic_status_spite" to IconSpec("SP", Violet),
        "ic_status_keen" to IconSpec("KN", ShieldBlue),
        "ic_status_dodge" to IconSpec("DG", TealDim),
        "ic_status_wind_shield" to IconSpec("WS", HpGreen),
        "ic_status_wind" to IconSpec("WI", TealDim),
        "ic_status_anger" to IconSpec("AN", OrangeHero),
        "ic_status_counter" to IconSpec("CO", OrangeHero),
        "ic_status_ignite" to IconSpec("IG", OrangeHero),
        "ic_status_omen" to IconSpec("OM", Violet),
        "ic_status_fire_shield" to IconSpec("FS", OrangeHero),
        "ic_status_kiss" to IconSpec("MK", EnergyGold),
        "ic_status_immunity" to IconSpec("IM", EnergyGold),
        "ic_status_reflect" to IconSpec("RS", HpGreen),
        "ic_status_echo" to IconSpec("MI", HpGreen),
        "ic_status_cloak" to IconSpec("CL", Violet),
        "ic_status_payback" to IconSpec("PB", Violet),
        "ic_status_ward" to IconSpec("WD", Violet),
        "ic_status_bubble" to IconSpec("BU", ShieldBlue),
        "ic_status_thief" to IconSpec("TF", EnergyGold),
        "ic_status_regen" to IconSpec("RG", HpGreen),

        // Enemy move icons (shown nowhere yet, defined so lookups never miss)
        "ic_atk_basic" to IconSpec("AT", DangerRed),
        "ic_def_small" to IconSpec("GD", ShieldBlue),
        "ic_def_heal_s" to IconSpec("HL", HpGreen),
        "ic_atk_venom" to IconSpec("VN", HpGreen),
        "ic_atk_crush" to IconSpec("CR", DangerRed),
        "ic_atk_doom" to IconSpec("DM", Violet)
    )

    operator fun get(iconId: String): IconSpec =
        map[iconId]
            ?: map[iconId.replace("ic_def_", "ic_off_")]  // ability icons share offhand chips
            ?: IconSpec("?", Accent)

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
        // The defectors. Grayscale on your side of the field.
        "ic_hero_ash" to Color(0xFFC9C9C9),
        "ic_hero_silver" to Color(0xFFF7F7F7)
    )

    fun heroColor(iconId: String): Color? = heroColors[iconId]
}
