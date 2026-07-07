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

object GameIcons {

    private val map = mapOf(
        // Enemies are grayscale, always. Color belongs to the heroes.
        "ic_enemy_grunt" to IconSpec("GR", Color(0xFFB0B0B0)),
        "ic_enemy_stinger" to IconSpec("ST", Color(0xFFD6D6D6)),
        "ic_enemy_shaman" to IconSpec("SH", Color(0xFF8A8A8A)),
        "ic_enemy_brute" to IconSpec("BR", Color(0xFFEDEDED)),
        "ic_enemy_hexer" to IconSpec("HX", Color(0xFF6E6E6E)),

        // Weapon abilities
        "ic_atk_cleave" to IconSpec("CL", Accent),
        "ic_atk_fan" to IconSpec("FN", Accent),
        "ic_atk_pierce" to IconSpec("PI", Accent),
        "ic_atk_ember" to IconSpec("EM", Amber),
        "ic_atk_reap" to IconSpec("RP", Violet),
        "ic_atk_leech" to IconSpec("LC", DangerRed),

        // Offhand abilities
        "ic_def_tower" to IconSpec("TW", ShieldBlue),
        "ic_def_spiked" to IconSpec("SP", ShieldBlue),
        "ic_def_medkit" to IconSpec("MD", HpGreen),
        "ic_def_banner" to IconSpec("BN", EnergyGold),
        "ic_def_detonate" to IconSpec("DT", Amber),
        "ic_def_cleanse" to IconSpec("CN", Teal),

        // Weapons / offhands themselves (loadout screen)
        "ic_wpn_cleaver" to IconSpec("CL", Accent),
        "ic_wpn_fan" to IconSpec("FN", Accent),
        "ic_wpn_piercer" to IconSpec("PI", Accent),
        "ic_wpn_ember" to IconSpec("EM", Amber),
        "ic_wpn_reaper" to IconSpec("RP", Violet),
        "ic_wpn_leech" to IconSpec("LC", DangerRed),
        "ic_off_tower" to IconSpec("TW", ShieldBlue),
        "ic_off_spiked" to IconSpec("SP", ShieldBlue),
        "ic_off_medkit" to IconSpec("MD", HpGreen),
        "ic_off_banner" to IconSpec("BN", EnergyGold),
        "ic_off_detonator" to IconSpec("DT", Amber),
        "ic_off_cleanser" to IconSpec("CN", Teal),

        // Statuses
        "ic_status_burn" to IconSpec("B", Amber),
        "ic_status_poison" to IconSpec("P", HpGreen),
        "ic_status_stun" to IconSpec("Z", EnergyGold),
        "ic_status_weaken" to IconSpec("W", TealDim),
        "ic_status_vulnerable" to IconSpec("V", DangerRed),
        "ic_status_taunt" to IconSpec("T", EnergyGold),

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
        "ic_hero_violet" to Color(0xFFB68CFF)
    )

    fun heroColor(iconId: String): Color? = heroColors[iconId]
}
