package com.elendheim.fewsbox.ui

import com.elendheim.fewsbox.data.Statuses
import com.elendheim.fewsbox.engine.ability.Ability
import com.elendheim.fewsbox.engine.ability.Condition
import com.elendheim.fewsbox.engine.ability.Effect
import com.elendheim.fewsbox.engine.ability.Resolver
import com.elendheim.fewsbox.engine.ability.ScalingSource
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
        // RED
        "wpn_red_sword" to "Sword",
        "wpn_red_maul" to "Crimson Maul",
        "wpn_red_cleaver" to "Cleaver",
        "wpn_red_axe" to "Axe",
        "wpn_red_pulse" to "The Pulse Cutter",
        "off_red_buckler" to "Jagged Buckler",
        "off_red_vial" to "Adrenaline Vial",
        "off_red_protector" to "Protector",
        "off_red_pest" to "Annoying Pest",
        "off_red_warcry" to "War Cry",
        // ORANGE
        "wpn_orange_ember" to "Ember Blade",
        "wpn_orange_torches" to "Twin Torches",
        "wpn_orange_whip" to "Flame Whip",
        "wpn_orange_fan" to "Cinder Fan",
        "wpn_orange_blaze" to "The Blazeborn Blade",
        "off_orange_embergain" to "Ember Gain",
        "off_orange_matchstick" to "Matchstick",
        "off_orange_smokescreen" to "Smokescreen",
        "off_orange_anger" to "Anger Management",
        "off_orange_skies" to "Scorching Skies",
        // YELLOW
        "wpn_yellow_siphon" to "Siphon",
        "wpn_yellow_bell" to "Ringing Bell",
        "wpn_yellow_lance" to "Dawn Lance",
        "wpn_yellow_lifeline" to "Lifeline",
        "wpn_yellow_karma" to "Karma",
        "off_yellow_medkit" to "Medkit",
        "off_yellow_light" to "Guardian Light",
        "off_yellow_overflow" to "Overflow",
        "off_yellow_kisses" to "Mommy Kisses",
        "off_yellow_sunrise" to "Sunrise",
        // GREEN
        "wpn_green_fan" to "Leaf Fan",
        "wpn_green_volley" to "Volley",
        "wpn_green_scythe" to "Reaper's Scythe",
        "wpn_green_tangle" to "Tanglewood Staff",
        "wpn_green_blast" to "The Last Blast",
        "off_green_quickstep" to "Quickstep",
        "off_green_sender" to "Return To Sender",
        "off_green_mirror" to "Mirror Image",
        "off_green_boon" to "Windrunner's Boon",
        "off_green_bark" to "Bark Skin",
        // BLUE
        "wpn_blue_hammer" to "Tide Hammer",
        "wpn_blue_anchor" to "Anchor",
        "wpn_blue_pike" to "Pike",
        "wpn_blue_undertow" to "Undertow",
        "wpn_blue_breakwater" to "The Breakwater",
        "off_blue_tower" to "Tower Shield",
        "off_blue_spilled" to "Spilled",
        "off_blue_wall" to "Shield Wall",
        "off_blue_current" to "Deep Current",
        "off_blue_weight" to "Counterweight",
        // VIOLET
        "wpn_violet_needle" to "Night Needle",
        "wpn_violet_fang" to "Fang",
        "wpn_violet_reaper" to "Dread Reaper",
        "wpn_violet_gravebind" to "Gravebind",
        "wpn_violet_nightfall" to "Nightfall",
        "off_violet_cloak" to "Shadow Cloak",
        "off_violet_payback" to "Payback Charm",
        "off_violet_omen" to "Omen",
        "off_violet_lastlaugh" to "Last Laugh",
        "off_violet_thief" to "Thief",
        // DEFECTORS
        "wpn_ash_cinder" to "Cinderfall",
        "wpn_ash_smoke" to "Smokebite",
        "wpn_ash_veil" to "Grey Veil",
        "wpn_silver_edge" to "Storm Edge",
        "wpn_silver_lash" to "Squall Lash",
        "wpn_silver_spike" to "Static Spike",
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
        "bleed" to "Bleed",
        "stun" to "Stun",
        "lure" to "Lured",
        "weaken" to "Weaken",
        "sunder" to "Sunder",
        "dull" to "Dull",
        "sap" to "Sap",
        "vulnerable" to "Vulnerable",
        "taunt" to "Taunt",
        "thorns" to "Thorns",
        "war_cry" to "War Cry",
        "rally" to "Rally",
        "guard" to "Guard",
        "pest_guard" to "Pest Guard",
        "spite" to "Spite",
        "keen" to "Keen",
        "dodge" to "Smoke Veil",
        "anger" to "Anger",
        "counter" to "Counter",
        "ignite" to "Ignite",
        "omen" to "Omen",
        "fire_shield" to "Fire Shield",
        "kiss" to "Mommy's Kiss",
        "immunity" to "Immunity",
        "reflect" to "Return To Sender",
        "echo" to "Mirror Image",
        "cloak" to "Cloaked",
        "payback" to "Payback",
        "ward" to "Death Ward",
        "bubble" to "Bubble",
        "thief" to "Thief's Mark",
        "regen" to "Regen"
    )

    private val statusBlurbs = mapOf(
        "burn" to "takes 3 damage per stack at the start of its turn, fades 1 stack per turn",
        "poison" to "takes 2 damage per stack at the start of its turn, holds until it expires",
        "scorch" to "burns for a flat 5 per stack at the start of its turn and never fades early",
        "bleed" to "takes 2 damage per stack at the start of its turn; stacks never fade early",
        "stun" to "skips one turn per stack; stunning a charging elite resets the wind-up",
        "lure" to "forced to attack the healthiest opposing unit",
        "weaken" to "deals 30% less damage",
        "sunder" to "deals 25% less damage",
        "dull" to "deals 20% less damage",
        "sap" to "deals 10% less damage",
        "vulnerable" to "takes 15% more damage",
        "taunt" to "enemies are forced to attack this unit",
        "thorns" to "strikes back at attackers for 3 damage per stack per hit",
        "war_cry" to "deals 25% more damage",
        "rally" to "deals 10% more damage",
        "guard" to "takes 30% less damage",
        "pest_guard" to "takes 15% less damage",
        "spite" to "deals 5% more damage",
        "keen" to "deals +5 damage per hit",
        "dodge" to "35% chance to take nothing from an incoming attack",
        "anger" to "adds 10% to the party ult meter at the end of each round",
        "counter" to "strikes attackers back instantly without spending a turn",
        "ignite" to "this unit's attacks set targets burning",
        "omen" to "this unit's attacks leave targets vulnerable",
        "fire_shield" to "attackers catch fire scaled to the damage they dealt",
        "kiss" to "instantly heals 30% of any damage taken",
        "immunity" to "new debuffs bounce off",
        "reflect" to "returns 80% of damage taken to the attacker",
        "echo" to "attacks repeat once at 50% damage, for free",
        "cloak" to "cannot be picked as a target by enemies",
        "payback" to "attackers are dulled for daring",
        "ward" to "a killing blow leaves this unit at 1 HP instead, once",
        "bubble" to "the next hit is ignored completely",
        "thief" to "every hit shakes 1 fews loose from the target",
        "regen" to "heals 6 per stack at the start of its turn"
    )

    private val weaponBlurbs = mapOf(
        "wpn_red_sword" to "A clean cut that leaves the target bleeding.",
        "wpn_red_maul" to "One massive swing that sometimes rings skulls.",
        "wpn_red_cleaver" to "Three chops, each opening the wound wider.",
        "wpn_red_axe" to "A heavy blow that saps the target's strength.",
        "wpn_red_pulse" to "A blade fed by the party's stored ult charge.",
        "wpn_orange_ember" to "A hit that sets the target burning.",
        "wpn_orange_torches" to "Two quick brands, both leaving fire behind.",
        "wpn_orange_whip" to "Three lashes across the target and its neighbors; loners eat all three.",
        "wpn_orange_fan" to "Barely a hit, but the fire it fans is serious - worse if they already burn.",
        "wpn_orange_blaze" to "A heavy blade that punishes anything already on fire.",
        "wpn_yellow_siphon" to "A hit that feeds half its damage back to Yellow.",
        "wpn_yellow_bell" to "A ring that heals the whole team for half the damage dealt.",
        "wpn_yellow_lance" to "A clean, hard-hitting thrust. No tricks.",
        "wpn_yellow_lifeline" to "A strike that mends whoever needs it most.",
        "wpn_yellow_karma" to "Grows stronger for every point of healing given this battle.",
        "wpn_green_fan" to "Two quick cuts that lure the target toward your healthiest hero.",
        "wpn_green_volley" to "Three shots on the target, spillover on the neighbors, everyone exposed.",
        "wpn_green_scythe" to "A reap that refunds the turn if it kills.",
        "wpn_green_tangle" to "Strips every enemy buff and rakes the whole line.",
        "wpn_green_blast" to "One huge blast that rolls over the line, fading as it goes.",
        "wpn_blue_hammer" to "A crushing blow that builds Blue's shield.",
        "wpn_blue_anchor" to "The heaviest thing Blue owns. Thrown.",
        "wpn_blue_pike" to "A thrust that hits far harder from behind a shield.",
        "wpn_blue_undertow" to "A wave over everyone that saps and sometimes stuns.",
        "wpn_blue_breakwater" to "A slam that throws Blue's whole shield at someone.",
        "wpn_violet_needle" to "A prick with a very real chance of putting someone down.",
        "wpn_violet_fang" to "Two bites, crueler against the already-cursed.",
        "wpn_violet_reaper" to "A reap that doubles against stunned targets.",
        "wpn_violet_gravebind" to "Three hits that gamble on a stun and drink from the stunned.",
        "wpn_violet_nightfall" to "A heavy strike that devours every debuff for extra damage.",
        "wpn_ash_cinder" to "A smoldering hit that leaves the target burning.",
        "wpn_ash_smoke" to "Two choking strikes that leave poison behind.",
        "wpn_ash_veil" to "A clean strike from inside the smoke.",
        "wpn_silver_edge" to "Three flashing cuts on one target.",
        "wpn_silver_lash" to "Three strikes scattered like weather.",
        "wpn_silver_spike" to "A charged hit that saps strength."
    )

    private val offhandBlurbs = mapOf(
        "off_red_buckler" to "A spiked shield for any ally that bites back at attackers.",
        "off_red_vial" to "Below half health it grants an extra turn; otherwise it just heals.",
        "off_red_protector" to "The whole team takes 30% less and deals 10% more for a turn.",
        "off_red_pest" to "Paint the target on any ally: everything attacks them, and they shrug off 15%.",
        "off_red_warcry" to "The whole team deals 25% more damage for 3 turns.",
        "off_orange_embergain" to "The ally's attacks set targets burning for 2 turns.",
        "off_orange_matchstick" to "A fire shield: attackers catch fire scaled to the damage they deal.",
        "off_orange_smokescreen" to "The whole team gets a 35% chance to dodge attacks for 2 turns.",
        "off_orange_anger" to "The ally gets angry: +10% ult meter at the end of each round, 3 rounds.",
        "off_orange_skies" to "The ally counterattacks anyone who hits them for 3 turns, free.",
        "off_yellow_medkit" to "Heal any ally for a flat 20.",
        "off_yellow_light" to "The ally regenerates 6 at the start of each turn for 3 turns.",
        "off_yellow_overflow" to "Heal the whole team 8; healing past full becomes shields.",
        "off_yellow_kisses" to "A healing shield: 30% of any damage taken heals back instantly.",
        "off_yellow_sunrise" to "Cleanse an ally's debuffs and make them immune to new ones.",
        "off_green_quickstep" to "The ally immediately gains an extra turn.",
        "off_green_sender" to "For a turn, the ally returns 80% of damage taken to the sender.",
        "off_green_mirror" to "The ally's attacks echo once at half damage for 3 turns.",
        "off_green_boon" to "The ally's cooldowns tick down and they get a small shield.",
        "off_green_bark" to "Heals the whole team 6, and the chosen ally 8.",
        "off_blue_tower" to "A big flat shield for any ally.",
        "off_blue_spilled" to "A small shield; a healthy ally also hits +5 harder for 2 turns.",
        "off_blue_wall" to "A shield of 6 for every ally.",
        "off_blue_current" to "Wash an ally's debuffs away with a small shield.",
        "off_blue_weight" to "Reads the room: shield 20 if the ally is hurting, 8 if they're fine.",
        "off_violet_cloak" to "The ally cannot be targeted for a turn; attacks go elsewhere.",
        "off_violet_payback" to "Anyone who hits the ally is dulled for their trouble.",
        "off_violet_omen" to "The ally's attacks leave targets vulnerable for 2 turns.",
        "off_violet_lastlaugh" to "A hurt ally survives a killing blow at 1 HP; a healthy one hits 5% harder.",
        "off_violet_thief" to "The ally's hits shake 1 fews loose from enemies, every hit."
    )

    fun name(id: String): String =
        names[id]
            ?: (if (id.startsWith("atk_")) names["wpn_" + id.removePrefix("atk_")] else null)
            ?: (if (id.startsWith("def_")) names["off_" + id.removePrefix("def_")] else null)
            ?: id

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

        is Effect.ApplyStatus -> {
            val roll = if (effect.chance < 1f) "${(effect.chance * 100).roundToInt()}% chance: applies"
            else "Applies"
            "$roll ${effect.stacks} ${name(effect.statusId)} for ${effect.duration} turns" +
                (statusBlurbs[effect.statusId]?.let { " (target $it)" } ?: "")
        }

        is Effect.ConsumeStatus ->
            "Consumes all ${name(effect.statusId)} on the target; per stack: " +
                describeEffect(effect.perStackEffect, attack).replaceFirstChar { it.lowercase() }

        is Effect.ConsumeAllDebuffs ->
            "Devours every debuff on the target for ${effect.flatPerDebuff} extra damage each"

        is Effect.ScalingStrike -> {
            val base = (attack * effect.multiplier).roundToInt()
            val source = when (effect.scaling) {
                ScalingSource.ULT_PERCENT -> "+1 per 1% of stored ult charge"
                ScalingSource.OWN_SHIELD -> "+1 per point of this hero's shield"
                ScalingSource.HEALING_DONE -> "+1 per 15 healing given this battle"
            }
            "Hits for $base damage, $source"
        }

        is Effect.CascadeDamage -> {
            val main = (attack * effect.multiplier).roundToInt()
            val drop = (effect.falloff * 100).roundToInt()
            "Blasts the target for $main damage; every other enemy takes $drop% less than the one before"
        }

        is Effect.TeamLifesteal ->
            "Heals every ally for ${(effect.fraction * 100).roundToInt()}% of the damage dealt"

        is Effect.HealLowestAlly ->
            "Heals the most wounded ally for ${(effect.fraction * 100).roundToInt()}% of the damage dealt"

        is Effect.HealAllAllies ->
            "Heals every ally ${effect.amount}" +
                if (effect.overflowToShield) "; healing past full becomes shield" else ""

        is Effect.ShieldSelf -> "This hero gains ${effect.amount} shield"

        Effect.DispelBuffs -> "Strips every buff off the target"

        is Effect.ReduceCooldowns -> "The target's cooldowns tick down ${effect.amount}"

        is Effect.Conditional -> {
            val main = "${describeCondition(effect.condition)}: " +
                describeEffect(effect.then, attack).replaceFirstChar { it.lowercase() }
            effect.otherwise?.let {
                "$main; otherwise " + describeEffect(it, attack).replaceFirstChar { c -> c.lowercase() }
            } ?: main
        }
    }

    private fun describeCondition(condition: Condition): String = when (condition) {
        is Condition.TargetHasStatus -> "If the target has ${name(condition.statusId)}"
        is Condition.TargetBelowHp -> "If the target is below ${(condition.fraction * 100).roundToInt()}% health"
        is Condition.TargetAtOrAboveHp -> "If the target is at or above ${(condition.fraction * 100).roundToInt()}% health"
        Condition.TargetIsShielded -> "If the target has a shield"
        Condition.TargetHasAnyDebuff -> "If the target has any debuff"
        Condition.SelfBelowHalfHp -> "If this unit is below half health"
        Condition.SelfIsShielded -> "If this hero has a shield"
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
