package com.elendheim.fewsbox.ui

import com.elendheim.fewsbox.data.Statuses
import com.elendheim.fewsbox.engine.ability.Ability
import com.elendheim.fewsbox.engine.ability.Condition
import com.elendheim.fewsbox.engine.ability.Effect
import com.elendheim.fewsbox.engine.ability.Resolver
import com.elendheim.fewsbox.engine.model.ActiveStatus
import com.elendheim.fewsbox.engine.model.CombatUnit
import kotlin.math.roundToInt

/** What a long-press shows: a title, a one-line subtitle, then detail lines. */
data class InfoContent(
    val title: String,
    val subtitle: String,
    val lines: List<String>
)

/**
 * Every readable word in the game lives here, on the UI side. The engine
 * stays wordless; this layer turns ids and effect data into sentences, with
 * damage numbers computed from the actual unit's stats so the text never
 * drifts from the truth.
 */
object GameText {

    private val names = mapOf(
        // Signature weapons, three per hero
        "wpn_red_maul" to "Crimson Maul",
        "wpn_red_twin" to "Twin Cleavers",
        "wpn_red_guillotine" to "Guillotine",
        "wpn_orange_brand" to "Firebrand",
        "wpn_orange_whip" to "Flare Whip",
        "wpn_orange_fan" to "Cinder Fan",
        "wpn_yellow_siphon" to "Sun Siphon",
        "wpn_yellow_lance" to "Dawn Lance",
        "wpn_yellow_bell" to "Morning Bell",
        "wpn_green_fan" to "Leaf Fan",
        "wpn_green_volley" to "Needle Volley",
        "wpn_green_scythe" to "Thorn Scythe",
        "wpn_blue_hammer" to "Tide Hammer",
        "wpn_blue_pike" to "Frost Pike",
        "wpn_blue_undertow" to "Undertow",
        "wpn_violet_reaper" to "Night Reaper",
        "wpn_violet_fang" to "Shadow Fang",
        "wpn_violet_needle" to "Hex Needle",
        "wpn_ash_cinder" to "Cinderfall",
        "wpn_ash_smoke" to "Smokebite",
        "wpn_ash_veil" to "Grey Veil",
        "wpn_silver_edge" to "Storm Edge",
        "wpn_silver_lash" to "Squall Lash",
        "wpn_silver_spike" to "Static Spike",
        // Offhands
        "off_tower_shield" to "Tower Shield",
        "off_spiked_shield" to "Spiked Shield",
        "off_medkit" to "Medkit",
        "off_banner" to "Banner",
        "off_detonator" to "Detonator",
        "off_cleanser" to "Cleanser",
        // Offhand abilities
        "def_tower" to "Tower Guard",
        "def_spiked" to "Spiked Guard",
        "def_medkit" to "Patch Up",
        "def_banner" to "Rally",
        "def_detonate" to "Detonate",
        "def_cleanse" to "Cleanse",
        // Enemy abilities
        "basic_slash" to "Slash",
        "small_guard" to "Guard",
        "small_heal" to "Mend",
        "venom_spit" to "Venom Spit",
        "crushing_blow" to "Crushing Blow",
        "doom_bolt" to "Doom Bolt",
        "ash_cloud" to "Ash Cloud",
        "cinder_spit" to "Cinder Spit",
        "ember_guard" to "Ember Guard",
        "silver_storm" to "Silver Storm",
        "null_wave" to "Null Wave",
        // Ultimates
        "ult_red" to "Berserk",
        "ult_orange" to "Inferno",
        "ult_yellow" to "Sunburst",
        "ult_green" to "Razor Storm",
        "ult_blue" to "Phalanx",
        "ult_violet" to "Terror",
        "ult_ash" to "Ashfall",
        "ult_silver" to "Tempest",
        // Statuses
        "burn" to "Burn",
        "poison" to "Poison",
        "scorch" to "Scorch",
        "stun" to "Stun",
        "weaken" to "Weaken",
        "vulnerable" to "Vulnerable",
        "taunt" to "Taunt",
        "thorns" to "Thorns"
    )

    private val statusBlurbs = mapOf(
        "burn" to "takes 3 damage per stack at the start of its turn, fades 1 stack per turn",
        "poison" to "takes 2 damage per stack at the start of its turn, holds until it expires",
        "stun" to "skips one turn per stack; stunning a charging elite resets the wind-up",
        "scorch" to "burns for a flat 5 per stack at the start of its turn and never fades early",
        "weaken" to "deals 30% less damage",
        "vulnerable" to "takes 25% more damage",
        "taunt" to "enemies are forced to attack this unit",
        "thorns" to "strikes back at attackers for 3 damage per stack per hit"
    )

    private val weaponBlurbs = mapOf(
        "wpn_red_maul" to "One massive swing.",
        "wpn_red_twin" to "Two heavy chops on one target.",
        "wpn_red_guillotine" to "An execute: far harder below 35% health.",
        "wpn_orange_brand" to "A hit that sets the target burning.",
        "wpn_orange_whip" to "Two quick lashes that leave a burn.",
        "wpn_orange_fan" to "Three sparks sprayed across random enemies.",
        "wpn_yellow_siphon" to "A hit that feeds most of its damage back as healing.",
        "wpn_yellow_lance" to "A clean, hard-hitting thrust.",
        "wpn_yellow_bell" to "A ring that saps the target's strength.",
        "wpn_green_fan" to "Three blades across random enemies.",
        "wpn_green_volley" to "Four needles, anywhere they land.",
        "wpn_green_scythe" to "A cleave that catches the target's neighbors.",
        "wpn_blue_hammer" to "A slow, crushing blow.",
        "wpn_blue_pike" to "Three rapid jabs on one target.",
        "wpn_blue_undertow" to "Two pulling strikes.",
        "wpn_violet_reaper" to "An execute: far harder below 30% health.",
        "wpn_violet_fang" to "A vicious single bite.",
        "wpn_violet_needle" to "A prick that leaves the target exposed.",
        "wpn_ash_cinder" to "A smoldering hit that leaves the target burning.",
        "wpn_ash_smoke" to "Two choking strikes that leave poison behind.",
        "wpn_ash_veil" to "A clean strike from inside the smoke.",
        "wpn_silver_edge" to "Three flashing cuts on one target.",
        "wpn_silver_lash" to "Three strikes scattered like weather.",
        "wpn_silver_spike" to "A charged hit that saps strength."
    )

    private val offhandBlurbs = mapOf(
        "off_tower_shield" to "A big shield, given to any ally.",
        "off_spiked_shield" to "A lighter shield for any ally that strikes back at attackers.",
        "off_medkit" to "Heal any ally.",
        "off_banner" to "Pull all enemy attacks onto yourself, plus a small shield.",
        "off_detonator" to "Blow up every Burn stack on the target for burst damage.",
        "off_cleanser" to "Strip an ally's debuffs and add a small shield."
    )

    fun name(id: String): String =
        names[id] ?: (if (id.startsWith("atk_")) names["wpn_" + id.removePrefix("atk_")] else null) ?: id

    fun weaponBlurb(id: String): String = weaponBlurbs[id] ?: ""
    fun offhandBlurb(id: String): String = offhandBlurbs[id] ?: ""

    // ------------------------------------------------------------------
    //  Ability text with real numbers
    // ------------------------------------------------------------------

    const val ULT_NOTE = "Fired by dragging the full party meter onto the hero; it does not use up their turn. " +
        "The meter gains 5% per attack, 3% per hit taken, and 15% when one hit costs over half a hero's max HP"

    fun describeAbility(ability: Ability, attack: Int, isUltimate: Boolean = false): List<String> {
        val lines = ability.effects.map { describeEffect(it, attack) }.toMutableList()
        if (ability.cooldown > 0) lines.add("Ready again after ${ability.cooldown} turns")
        if (isUltimate) lines.add(ULT_NOTE)
        return lines
    }

    private fun describeEffect(effect: Effect, attack: Int): String = when (effect) {
        is Effect.DealDamage -> {
            val dmg = (attack * effect.multiplier).roundToInt()
            val crit = (dmg * Resolver.CRIT_MULTIPLIER).roundToInt()
            val base = if (effect.hits > 1) "Hits ${effect.hits} times for $dmg damage each"
            else "Hits for $dmg damage"
            if (effect.canCrit) "$base ($crit on crit)" else base
        }

        is Effect.ExecuteDamage -> {
            val normal = (attack * effect.multiplier).roundToInt()
            val boosted = (attack * (effect.multiplier + effect.bonusMultiplier)).roundToInt()
            val pct = (effect.hpThreshold * 100).roundToInt()
            "Hits for $normal damage, or $boosted below $pct% health"
        }

        is Effect.DealFlatDamage -> "Hits for exactly ${effect.amount} damage, no crits"

        is Effect.HealPercent -> "Restores ${(effect.fraction * 100).roundToInt()}% of max health"

        is Effect.GrantExtraActions -> "Grants ${effect.count} extra turns this round"

        is Effect.Lifesteal -> {
            val pct = (effect.fraction * 100).roundToInt()
            "Heals the attacker for $pct% of the damage dealt"
        }

        is Effect.GainShield -> "Grants ${effect.amount} shield"
        is Effect.Heal -> "Restores ${effect.amount} health"
        is Effect.Taunt -> "Forces enemies to attack this unit for ${effect.turns} turn" +
            if (effect.turns > 1) "s" else ""

        Effect.Cleanse -> "Removes all debuffs"

        is Effect.ApplyStatus ->
            "Applies ${effect.stacks} ${name(effect.statusId)} for ${effect.duration} turns" +
                (statusBlurbs[effect.statusId]?.let { " (target $it)" } ?: "")

        is Effect.ConsumeStatus ->
            "Consumes all ${name(effect.statusId)} on the target; per stack: " +
                describeEffect(effect.perStackEffect, attack).replaceFirstChar { it.lowercase() }

        is Effect.Conditional ->
            "${describeCondition(effect.condition)}: " +
                describeEffect(effect.then, attack).replaceFirstChar { it.lowercase() }
    }

    private fun describeCondition(condition: Condition): String = when (condition) {
        is Condition.TargetHasStatus -> "If the target has ${name(condition.statusId)}"
        is Condition.TargetBelowHp -> "If the target is below ${(condition.fraction * 100).roundToInt()}% health"
        Condition.TargetIsShielded -> "If the target has a shield"
        Condition.SelfBelowHalfHp -> "If this unit is below half health"
    }

    // ------------------------------------------------------------------
    //  Info cards
    // ------------------------------------------------------------------

    fun abilityInfo(ability: Ability, attack: Int, isUltimate: Boolean = false): InfoContent = InfoContent(
        title = name(ability.id),
        subtitle = if (isUltimate) "Ultimate" else "Ability",
        lines = describeAbility(ability, attack, isUltimate)
    )

    fun unitInfo(unit: CombatUnit): InfoContent {
        val lines = mutableListOf<String>()

        for (ability in unit.abilities) {
            val isUlt = ability.id == unit.ultimateId
            lines.add(name(ability.id).uppercase() + if (isUlt) " (ULTIMATE)" else "")
            lines.addAll(describeAbility(ability, unit.baseAttack, isUlt).map { "  $it" })
        }

        unit.charge?.let { charge ->
            val left = charge.turnsRequired - charge.turnsElapsed
            lines.add("CHARGING: ${name(charge.chargingAbilityId).uppercase()}")
            lines.add(
                if (charge.isReady) "  Fires on its next turn"
                else "  Fires in $left more turn" + (if (left > 1) "s" else "") + " unless stunned"
            )
        }

        // Always the last section, so what's affecting this unit right now
        // never gets buried between its moves.
        if (unit.statuses.isNotEmpty()) {
            lines.add("AFFECTING ${unit.name.uppercase()} RIGHT NOW")
            for (status in unit.statuses) lines.add("  " + statusLine(status))
        }

        val shieldNote = if (unit.shield > 0) " · Shield ${unit.shield}" else ""
        return InfoContent(
            title = unit.name,
            subtitle = "HP ${unit.hp}/${unit.maxHp} · ATK ${unit.baseAttack}$shieldNote",
            lines = lines
        )
    }

    fun statusLine(status: ActiveStatus): String {
        val turns = "${status.remainingTurns} turn" + (if (status.remainingTurns != 1) "s" else "") + " left"
        val stacks = if (status.stacks > 1) " x${status.stacks}" else ""
        val blurb = statusBlurbs[status.defId]?.let { " — $it" } ?: ""
        return "${name(status.defId)}$stacks ($turns)$blurb"
    }
}
