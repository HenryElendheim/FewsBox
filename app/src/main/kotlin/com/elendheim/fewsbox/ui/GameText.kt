package com.elendheim.fewsbox.ui

import com.elendheim.fewsbox.data.Statuses
import com.elendheim.fewsbox.engine.ability.Ability
import com.elendheim.fewsbox.engine.ability.Condition
import com.elendheim.fewsbox.engine.ability.Effect
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
        // Weapons
        "wpn_cleaver" to "Cleaver",
        "wpn_fan_blades" to "Fan Blades",
        "wpn_piercer" to "Piercer",
        "wpn_ember_blade" to "Ember Blade",
        "wpn_reaper" to "Reaper",
        "wpn_leech" to "Leech",
        // Weapon abilities
        "atk_cleave" to "Cleave",
        "atk_fan" to "Fan of Blades",
        "atk_pierce" to "Pierce",
        "atk_ember" to "Ember Strike",
        "atk_reap" to "Reap",
        "atk_leech" to "Leech Strike",
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
        "silver_storm" to "Silver Storm",
        // Ultimates
        "ult_red" to "Berserk",
        "ult_orange" to "Inferno",
        "ult_yellow" to "Sunburst",
        "ult_green" to "Razor Storm",
        "ult_blue" to "Phalanx",
        "ult_violet" to "Terror",
        // Statuses
        "burn" to "Burn",
        "poison" to "Poison",
        "stun" to "Stun",
        "weaken" to "Weaken",
        "vulnerable" to "Vulnerable",
        "taunt" to "Taunt",
        "thorns" to "Thorns"
    )

    private val statusBlurbs = mapOf(
        "burn" to "takes 3 damage per stack at the start of its turn, fades 1 stack per turn",
        "poison" to "takes 2 damage per stack at the start of its turn, holds until it expires",
        "stun" to "skips its next turn; stunning a charging elite resets the wind-up",
        "weaken" to "deals 30% less damage",
        "vulnerable" to "takes 25% more damage",
        "taunt" to "enemies are forced to attack this unit",
        "thorns" to "strikes back at attackers for 3 damage per stack per hit"
    )

    private val weaponBlurbs = mapOf(
        "wpn_cleaver" to "One heavy, reliable hit.",
        "wpn_fan_blades" to "Three hits sprayed across random enemies. Mob clearer.",
        "wpn_piercer" to "Three hits, all on one target. Elite shredder.",
        "wpn_ember_blade" to "A hit that sets the target burning. Sets up Detonator.",
        "wpn_reaper" to "An execute: far harder against low-health targets.",
        "wpn_leech" to "A hit that feeds half its damage back as healing."
    )

    private val offhandBlurbs = mapOf(
        "off_tower_shield" to "A big shield, given to any ally.",
        "off_spiked_shield" to "A lighter shield for any ally that strikes back at attackers.",
        "off_medkit" to "Heal any ally.",
        "off_banner" to "Pull all enemy attacks onto yourself, plus a small shield.",
        "off_detonator" to "Blow up every Burn stack on the target for burst damage.",
        "off_cleanser" to "Strip an ally's debuffs and add a small shield."
    )

    fun name(id: String): String = names[id] ?: id

    fun weaponBlurb(id: String): String = weaponBlurbs[id] ?: ""
    fun offhandBlurb(id: String): String = offhandBlurbs[id] ?: ""

    // ------------------------------------------------------------------
    //  Ability text with real numbers
    // ------------------------------------------------------------------

    fun describeAbility(ability: Ability, attack: Int): List<String> {
        val lines = ability.effects.map { describeEffect(it, attack) }
        val cost = buildString {
            append("Costs ${ability.cost} energy")
            if (ability.cooldown > 0) append(", ${ability.cooldown} turn cooldown")
        }
        return lines + cost
    }

    private fun describeEffect(effect: Effect, attack: Int): String = when (effect) {
        is Effect.DealDamage -> {
            val dmg = (attack * effect.multiplier).roundToInt()
            val base = if (effect.hits > 1) "Hits ${effect.hits} times for about $dmg damage each"
            else "Hits for about $dmg damage"
            if (effect.canCrit) "$base, can crit" else base
        }

        is Effect.ExecuteDamage -> {
            val normal = (attack * effect.multiplier).roundToInt()
            val boosted = (attack * (effect.multiplier + effect.bonusMultiplier)).roundToInt()
            val pct = (effect.hpThreshold * 100).roundToInt()
            "Hits for about $normal damage, or $boosted if the target is below $pct% health"
        }

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

    fun abilityInfo(ability: Ability, attack: Int): InfoContent = InfoContent(
        title = name(ability.id),
        subtitle = "Ability",
        lines = describeAbility(ability, attack)
    )

    fun unitInfo(unit: CombatUnit): InfoContent {
        val lines = mutableListOf<String>()

        for (ability in unit.abilities) {
            lines.add(name(ability.id).uppercase())
            lines.addAll(describeAbility(ability, unit.baseAttack).map { "  $it" })
        }

        if (unit.statuses.isNotEmpty()) {
            lines.add("ACTIVE EFFECTS")
            for (status in unit.statuses) lines.add("  " + statusLine(status))
        }

        unit.charge?.let { charge ->
            val left = charge.turnsRequired - charge.turnsElapsed
            lines.add("CHARGING: ${name(charge.chargingAbilityId).uppercase()}")
            lines.add(
                if (charge.isReady) "  Fires on its next turn"
                else "  Fires in $left more turn" + (if (left > 1) "s" else "") + " unless stunned"
            )
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
