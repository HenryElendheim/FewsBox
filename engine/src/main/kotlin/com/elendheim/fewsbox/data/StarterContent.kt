package com.elendheim.fewsbox.data

import com.elendheim.fewsbox.engine.ability.Ability
import com.elendheim.fewsbox.engine.ability.Condition
import com.elendheim.fewsbox.engine.ability.Effect
import com.elendheim.fewsbox.engine.ability.Resolver
import com.elendheim.fewsbox.engine.ability.ScalingSource
import com.elendheim.fewsbox.engine.ability.Targeting
import com.elendheim.fewsbox.engine.status.PassiveEffect
import com.elendheim.fewsbox.engine.status.StatusDef
import com.elendheim.fewsbox.engine.status.StatusKind
import com.elendheim.fewsbox.engine.status.StatusTiming

// ============================================================================
//  Game content. Every hero owns five weapons and five offhands, all
//  sidegrades: customization, never a power ladder. Offhands only ever
//  touch your own team. Content is data; the engine never changes for it.
// ============================================================================

object Statuses {

    private fun dot(id: String, magnitude: Int, decays: Boolean = false) = StatusDef(
        id = id, iconId = "ic_status_$id", kind = StatusKind.DEBUFF,
        timing = StatusTiming.TICK_START_OF_TURN, magnitude = magnitude, decaysOnTick = decays
    )

    private fun debuff(id: String, passive: PassiveEffect, magnitude: Int = 0) = StatusDef(
        id = id, iconId = "ic_status_$id", kind = StatusKind.DEBUFF,
        timing = StatusTiming.PASSIVE_MODIFIER, magnitude = magnitude, passive = passive
    )

    private fun buff(id: String, passive: PassiveEffect, magnitude: Int = 0) = StatusDef(
        id = id, iconId = "ic_status_$id", kind = StatusKind.BUFF,
        timing = StatusTiming.PASSIVE_MODIFIER, magnitude = magnitude, passive = passive
    )

    // --- Damage over time ---
    val BURN = dot("burn", magnitude = 3, decays = true)
    val POISON = dot("poison", magnitude = 2)
    val SCORCH = dot("scorch", magnitude = 5)
    val BLEED = dot("bleed", magnitude = 2)          // Red's mark: stacks never fade early

    // --- Control ---
    val STUN = StatusDef(
        id = "stun", iconId = "ic_status_stun",
        kind = StatusKind.DEBUFF, timing = StatusTiming.ON_APPLY_ONLY
    )
    val LURE = debuff("lure", PassiveEffect.LURE)    // forced onto the healthiest foe

    // --- Output down (three strengths, three sources) ---
    val WEAKEN = debuff("weaken", PassiveEffect.DAMAGE_DEALT_DOWN, 30)   // boss-grade
    val SUNDER = debuff("sunder", PassiveEffect.DAMAGE_DEALT_DOWN, 25)   // Red's Axe
    val DULL = debuff("dull", PassiveEffect.DAMAGE_DEALT_DOWN, 20)       // Payback Charm
    val SAP = debuff("sap", PassiveEffect.DAMAGE_DEALT_DOWN, 10)         // Undertow

    // --- Taken up ---
    val VULNERABLE = debuff("vulnerable", PassiveEffect.DAMAGE_TAKEN_UP, 15)

    // --- Team buffs ---
    val TAUNT = buff(Resolver.TAUNT_STATUS_ID, PassiveEffect.DAMAGE_TAKEN_DOWN, 0)
    val THORNS = buff("thorns", PassiveEffect.THORNS, 3)
    val WAR_CRY = buff("war_cry", PassiveEffect.DAMAGE_DEALT_UP, 25)
    val RALLY = buff("rally", PassiveEffect.DAMAGE_DEALT_UP, 10)         // Protector's edge
    val GUARD = buff("guard", PassiveEffect.DAMAGE_TAKEN_DOWN, 30)       // Protector's wall
    val PEST_GUARD = buff("pest_guard", PassiveEffect.DAMAGE_TAKEN_DOWN, 15)
    val SPITE = buff("spite", PassiveEffect.DAMAGE_DEALT_UP, 5)          // Last Laugh, healthy
    val KEEN = buff("keen", PassiveEffect.DAMAGE_DEALT_UP_FLAT, 5)       // Spilled's edge
    val DODGE = buff("dodge", PassiveEffect.DODGE, 35)                   // Smokescreen
    val WIND_SHIELD = buff("wind_shield", PassiveEffect.PUNISH_WIND)     // Windrunner's Boon
    val WIND = debuff("wind", PassiveEffect.MISS_CHANCE, 50)             // half the swings whiff
    val ANGER = buff("anger", PassiveEffect.ULT_TICK, 10)                // Anger Management
    val COUNTER = buff("counter", PassiveEffect.COUNTER)                 // Scorching Skies
    val IGNITE = buff("ignite", PassiveEffect.ON_HIT_APPLY_BURN)         // Ember Gain
    val OMEN = buff("omen", PassiveEffect.ON_HIT_APPLY_VULN)
    val FIRE_SHIELD = buff("fire_shield", PassiveEffect.BURN_REFLECT, 20) // Matchstick
    val KISS = buff("kiss", PassiveEffect.HEAL_WHEN_HIT, 30)             // Mommy Kisses
    val IMMUNITY = buff("immunity", PassiveEffect.DEBUFF_IMMUNE)         // Sunrise
    val REFLECT = buff("reflect", PassiveEffect.REFLECT_PERCENT, 80)     // Return To Sender
    val ECHO = buff("echo", PassiveEffect.ECHO, 50)                      // Mirror Image
    val CLOAK = buff("cloak", PassiveEffect.UNTARGETABLE)                // Shadow Cloak
    val PAYBACK = buff("payback", PassiveEffect.PUNISH_WEAKEN)
    val WARD = buff("ward", PassiveEffect.DEATH_WARD)                    // Last Laugh, hurt
    val BUBBLE = buff("bubble", PassiveEffect.NEGATE_HIT)                // Blue's ultimate
    val THIEF = buff("thief", PassiveEffect.THIEF, 1)                    // fews per hit

    // Regen is a BUFF that ticks: the engine heals instead of hurts.
    val REGEN = StatusDef(
        id = "regen", iconId = "ic_status_regen", kind = StatusKind.BUFF,
        timing = StatusTiming.TICK_START_OF_TURN, magnitude = 6
    )

    val ALL = listOf(
        BURN, POISON, SCORCH, BLEED, STUN, LURE,
        WEAKEN, SUNDER, DULL, SAP, VULNERABLE,
        TAUNT, THORNS, WAR_CRY, RALLY, GUARD, PEST_GUARD, SPITE, KEEN,
        DODGE, WIND_SHIELD, WIND, ANGER, COUNTER, IGNITE, OMEN, FIRE_SHIELD, KISS, IMMUNITY,
        REFLECT, ECHO, CLOAK, PAYBACK, WARD, BUBBLE, THIEF, REGEN
    )
    val REGISTRY: Map<String, StatusDef> = ALL.associateBy { it.id }
}

// ----------------------------------------------------------------------------
//  WEAPONS — five per hero. Starters are simple; the fifth is always the
//  scaling finale. Defectors keep the kit they fought you with.
// ----------------------------------------------------------------------------

object Weapons {

    private fun weapon(id: String, bonus: Int = 0, ability: Ability) =
        Weapon(id = id, iconId = "ic_$id", attackBonus = bonus, grantedAbility = ability)

    private fun atk(id: String, targeting: Targeting, vararg effects: Effect) =
        Ability(id = "atk_$id", iconId = "ic_wpn_$id", targeting = targeting, effects = effects.toList())

    // --- RED: bleed, concussion, and the meter-sword ---
    val RED_SWORD = weapon("wpn_red_sword",
        ability = atk("red_sword", Targeting.SINGLE_ENEMY,
            Effect.DealDamage(multiplier = 1.1f, hits = 1, canCrit = true),
            Effect.ApplyStatus(statusId = "bleed", stacks = 2, duration = 3)))
    val RED_MAUL = weapon("wpn_red_maul", bonus = 2,
        ability = atk("red_maul", Targeting.SINGLE_ENEMY,
            Effect.DealDamage(multiplier = 1.8f, hits = 1, canCrit = true),
            Effect.ApplyStatus(statusId = "stun", stacks = 1, duration = 1, chance = 0.25f)))
    val RED_CLEAVER = weapon("wpn_red_cleaver",
        ability = atk("red_cleaver", Targeting.SINGLE_ENEMY,
            Effect.DealDamage(multiplier = 0.6f, hits = 3, canCrit = true),
            Effect.ApplyStatus(statusId = "bleed", stacks = 2, duration = 2)))
    val RED_AXE = weapon("wpn_red_axe",
        ability = atk("red_axe", Targeting.SINGLE_ENEMY,
            Effect.DealDamage(multiplier = 1.2f, hits = 1, canCrit = true),
            Effect.ApplyStatus(statusId = "sunder", stacks = 1, duration = 3)))
    val RED_PULSE = weapon("wpn_red_pulse",
        ability = atk("red_pulse", Targeting.SINGLE_ENEMY,
            Effect.ScalingStrike(multiplier = 1.0f, scaling = ScalingSource.ULT_PERCENT)))

    // --- ORANGE: everything stacks fire, the finale profits off it ---
    val ORANGE_EMBER = weapon("wpn_orange_ember",
        ability = atk("orange_ember", Targeting.SINGLE_ENEMY,
            Effect.DealDamage(multiplier = 1.0f, hits = 1, canCrit = true),
            Effect.ApplyStatus(statusId = "burn", stacks = 2, duration = 3)))
    val ORANGE_TORCHES = weapon("wpn_orange_torches",
        ability = atk("orange_torches", Targeting.SINGLE_ENEMY,
            Effect.DealDamage(multiplier = 0.7f, hits = 2, canCrit = true),
            Effect.ApplyStatus(statusId = "burn", stacks = 2, duration = 3)))
    val ORANGE_WHIP = weapon("wpn_orange_whip",
        ability = atk("orange_whip", Targeting.WHIP_ADJACENT,
            Effect.DealDamage(multiplier = 0.55f, hits = 1, canCrit = true),
            Effect.ApplyStatus(statusId = "burn", stacks = 1, duration = 2)))
    val ORANGE_FAN = weapon("wpn_orange_fan",
        ability = atk("orange_fan", Targeting.SINGLE_ENEMY,
            Effect.Conditional(
                condition = Condition.TargetHasStatus("burn"),
                then = Effect.DealDamage(multiplier = 0.7f, hits = 1, canCrit = false)
            ),
            Effect.DealDamage(multiplier = 0.5f, hits = 1, canCrit = false),
            Effect.ApplyStatus(statusId = "burn", stacks = 3, duration = 3)))
    val ORANGE_BLAZE = weapon("wpn_orange_blaze", bonus = 1,
        ability = atk("orange_blaze", Targeting.SINGLE_ENEMY,
            Effect.Conditional(
                condition = Condition.TargetHasStatus("burn"),
                then = Effect.DealDamage(multiplier = 0.75f, hits = 1, canCrit = false)
            ),
            Effect.DealDamage(multiplier = 1.5f, hits = 1, canCrit = false),
            Effect.ApplyStatus(statusId = "burn", stacks = 1, duration = 3)))

    // --- YELLOW: every swing leaks value back to the team ---
    val YELLOW_SIPHON = weapon("wpn_yellow_siphon",
        ability = atk("yellow_siphon", Targeting.SINGLE_ENEMY,
            Effect.DealDamage(multiplier = 1.0f, hits = 1, canCrit = true),
            Effect.Lifesteal(fraction = 0.5f)))
    val YELLOW_BELL = weapon("wpn_yellow_bell",
        ability = atk("yellow_bell", Targeting.SINGLE_ENEMY,
            Effect.DealDamage(multiplier = 0.9f, hits = 1, canCrit = true),
            Effect.TeamLifesteal(fraction = 0.5f)))
    val YELLOW_LANCE = weapon("wpn_yellow_lance", bonus = 1,
        ability = atk("yellow_lance", Targeting.SINGLE_ENEMY,
            Effect.DealDamage(multiplier = 1.4f, hits = 1, canCrit = true)))
    val YELLOW_LIFELINE = weapon("wpn_yellow_lifeline",
        ability = atk("yellow_lifeline", Targeting.SINGLE_ENEMY,
            Effect.DealDamage(multiplier = 1.1f, hits = 1, canCrit = true),
            Effect.HealLowestAlly(fraction = 0.4f)))
    val YELLOW_KARMA = weapon("wpn_yellow_karma",
        ability = atk("yellow_karma", Targeting.SINGLE_ENEMY,
            Effect.ScalingStrike(multiplier = 1.0f, scaling = ScalingSource.HEALING_DONE)))

    // --- GREEN: the turn economy bends around him ---
    val GREEN_FAN = weapon("wpn_green_fan",
        ability = atk("green_fan", Targeting.SINGLE_ENEMY,
            Effect.DealDamage(multiplier = 0.65f, hits = 2, canCrit = true),
            Effect.ApplyStatus(statusId = "lure", stacks = 1, duration = 2)))
    val GREEN_VOLLEY = weapon("wpn_green_volley",
        ability = atk("green_volley", Targeting.VOLLEY_ADJACENT,
            Effect.DealDamage(multiplier = 0.5f, hits = 3, canCrit = true),
            Effect.ApplyStatus(statusId = "vulnerable", stacks = 1, duration = 2)))
    val GREEN_SCYTHE = weapon("wpn_green_scythe",
        ability = atk("green_scythe", Targeting.SINGLE_ENEMY,
            Effect.DealDamage(multiplier = 1.2f, hits = 1, canCrit = true, extraActionOnKill = true)))
    val GREEN_TANGLE = weapon("wpn_green_tangle",
        ability = atk("green_tangle", Targeting.ALL_ENEMIES,
            Effect.DispelBuffs,
            Effect.DealDamage(multiplier = 0.55f, hits = 1, canCrit = false)))
    val GREEN_BLAST = weapon("wpn_green_blast", bonus = 1,
        ability = atk("green_blast", Targeting.SINGLE_ENEMY,
            Effect.CascadeDamage(multiplier = 2.0f, falloff = 0.4f)))

    // --- BLUE: hits that hold, holds that hit ---
    val BLUE_HAMMER = weapon("wpn_blue_hammer",
        ability = atk("blue_hammer", Targeting.SINGLE_ENEMY,
            Effect.DealDamage(multiplier = 1.0f, hits = 1, canCrit = true),
            Effect.ShieldSelf(amount = 4)))
    val BLUE_ANCHOR = weapon("wpn_blue_anchor", bonus = 1,
        ability = atk("blue_anchor", Targeting.SINGLE_ENEMY,
            Effect.DealDamage(multiplier = 1.7f, hits = 1, canCrit = true)))
    val BLUE_PIKE = weapon("wpn_blue_pike",
        ability = atk("blue_pike", Targeting.SINGLE_ENEMY,
            Effect.DealDamage(multiplier = 1.0f, hits = 1, canCrit = true),
            Effect.Conditional(
                condition = Condition.SelfIsShielded,
                then = Effect.DealDamage(multiplier = 0.4f, hits = 1, canCrit = false)
            )))
    val BLUE_UNDERTOW = weapon("wpn_blue_undertow",
        ability = atk("blue_undertow", Targeting.ALL_ENEMIES,
            Effect.DealDamage(multiplier = 0.5f, hits = 1, canCrit = false),
            Effect.ApplyStatus(statusId = "sap", stacks = 1, duration = 1),
            Effect.ApplyStatus(statusId = "stun", stacks = 1, duration = 1, chance = 0.10f)))
    val BLUE_BREAKWATER = weapon("wpn_blue_breakwater",
        ability = atk("blue_breakwater", Targeting.SINGLE_ENEMY,
            Effect.ScalingStrike(multiplier = 1.0f, scaling = ScalingSource.OWN_SHIELD)))

    // --- VIOLET: control first, cruelty second ---
    val VIOLET_NEEDLE = weapon("wpn_violet_needle",
        ability = atk("violet_needle", Targeting.SINGLE_ENEMY,
            Effect.DealDamage(multiplier = 1.0f, hits = 1, canCrit = true),
            Effect.ApplyStatus(statusId = "stun", stacks = 2, duration = 2, chance = 0.35f)))
    val VIOLET_FANG = weapon("wpn_violet_fang",
        ability = atk("violet_fang", Targeting.SINGLE_ENEMY,
            Effect.DealDamage(multiplier = 0.7f, hits = 2, canCrit = true),
            Effect.Conditional(
                condition = Condition.TargetHasAnyDebuff,
                then = Effect.DealDamage(multiplier = 0.7f, hits = 1, canCrit = false)
            )))
    val VIOLET_REAPER = weapon("wpn_violet_reaper", bonus = 1,
        ability = atk("violet_reaper", Targeting.SINGLE_ENEMY,
            Effect.DealDamage(multiplier = 1.2f, hits = 1, canCrit = true),
            Effect.Conditional(
                condition = Condition.TargetHasStatus("stun"),
                then = Effect.DealDamage(multiplier = 1.2f, hits = 1, canCrit = false)
            )))
    val VIOLET_GRAVEBIND = weapon("wpn_violet_gravebind",
        ability = atk("violet_gravebind", Targeting.SINGLE_ENEMY,
            Effect.DealDamage(multiplier = 0.55f, hits = 3, canCrit = true),
            Effect.Conditional(
                condition = Condition.TargetHasStatus("stun"),
                then = Effect.Lifesteal(fraction = 0.15f)
            ),
            Effect.ApplyStatus(statusId = "stun", stacks = 1, duration = 1, chance = 0.10f)))
    val VIOLET_NIGHTFALL = weapon("wpn_violet_nightfall",
        ability = atk("violet_nightfall", Targeting.SINGLE_ENEMY,
            Effect.DealDamage(multiplier = 1.4f, hits = 1, canCrit = false),
            Effect.ConsumeAllDebuffs(flatPerDebuff = 5)))

    // --- ASH: everything smolders (the kit he fought you with) ---
    val ASH_CINDER = weapon("wpn_ash_cinder",
        ability = atk("ash_cinder", Targeting.SINGLE_ENEMY,
            Effect.DealDamage(multiplier = 0.9f, hits = 1, canCrit = true),
            Effect.ApplyStatus(statusId = "burn", stacks = 2, duration = 3)))
    val ASH_SMOKE = weapon("wpn_ash_smoke",
        ability = atk("ash_smoke", Targeting.SINGLE_ENEMY,
            Effect.DealDamage(multiplier = 0.65f, hits = 2, canCrit = true),
            Effect.ApplyStatus(statusId = "poison", stacks = 1, duration = 3)))
    val ASH_VEIL = weapon("wpn_ash_veil", bonus = 1,
        ability = atk("ash_veil", Targeting.SINGLE_ENEMY,
            Effect.DealDamage(multiplier = 1.3f, hits = 1, canCrit = true)))

    // --- SILVER: the storm made personal ---
    val SILVER_EDGE = weapon("wpn_silver_edge",
        ability = atk("silver_edge", Targeting.SINGLE_ENEMY,
            Effect.DealDamage(multiplier = 0.65f, hits = 3, canCrit = true)))
    val SILVER_LASH = weapon("wpn_silver_lash",
        ability = atk("silver_lash", Targeting.RANDOM_ENEMIES_MULTI,
            Effect.DealDamage(multiplier = 0.6f, hits = 3, canCrit = true)))
    val SILVER_SPIKE = weapon("wpn_silver_spike",
        ability = atk("silver_spike", Targeting.SINGLE_ENEMY,
            Effect.DealDamage(multiplier = 1.2f, hits = 1, canCrit = false),
            Effect.ApplyStatus(statusId = "weaken", stacks = 1, duration = 2)))

    val ALL = listOf(
        RED_SWORD, RED_MAUL, RED_CLEAVER, RED_AXE, RED_PULSE,
        ORANGE_EMBER, ORANGE_TORCHES, ORANGE_WHIP, ORANGE_FAN, ORANGE_BLAZE,
        YELLOW_SIPHON, YELLOW_BELL, YELLOW_LANCE, YELLOW_LIFELINE, YELLOW_KARMA,
        GREEN_FAN, GREEN_VOLLEY, GREEN_SCYTHE, GREEN_TANGLE, GREEN_BLAST,
        BLUE_HAMMER, BLUE_ANCHOR, BLUE_PIKE, BLUE_UNDERTOW, BLUE_BREAKWATER,
        VIOLET_NEEDLE, VIOLET_FANG, VIOLET_REAPER, VIOLET_GRAVEBIND, VIOLET_NIGHTFALL,
        ASH_CINDER, ASH_SMOKE, ASH_VEIL,
        SILVER_EDGE, SILVER_LASH, SILVER_SPIKE
    )
    val REGISTRY: Map<String, Weapon> = ALL.associateBy { it.id }
}

// ----------------------------------------------------------------------------
//  OFFHANDS — five per hero, all team-only: drag onto any teammate. What a
//  hero's weapons start, their offhands convert.
// ----------------------------------------------------------------------------

object Offhands {

    private fun off(id: String, targeting: Targeting, vararg effects: Effect): Offhand {
        val suffix = id.removePrefix("off_")
        return Offhand(
            id = id, iconId = "ic_$id",
            grantedAbility = Ability(
                id = "def_$suffix", iconId = "ic_def_$suffix",
                targeting = targeting, effects = effects.toList()
            )
        )
    }

    // --- RED ---
    val RED_BUCKLER = off("off_red_buckler", Targeting.SINGLE_ALLY,
        Effect.GainShield(amount = 8),
        Effect.ApplyStatus(statusId = "thorns", stacks = 1, duration = 2))
    val RED_VIAL = off("off_red_vial", Targeting.SINGLE_ALLY,
        Effect.Conditional(
            condition = Condition.TargetBelowHp(0.5f),
            then = Effect.GrantExtraActions(count = 1),
            otherwise = Effect.Heal(amount = 8)
        ))
    val RED_PROTECTOR = off("off_red_protector", Targeting.ALL_ALLIES,
        Effect.ApplyStatus(statusId = "guard", stacks = 1, duration = 1),
        Effect.ApplyStatus(statusId = "rally", stacks = 1, duration = 1))
    val RED_PEST = off("off_red_pest", Targeting.SINGLE_ALLY,
        Effect.Taunt(turns = 3),
        Effect.ApplyStatus(statusId = "pest_guard", stacks = 1, duration = 3))
    val RED_WARCRY = off("off_red_warcry", Targeting.ALL_ALLIES,
        Effect.ApplyStatus(statusId = "war_cry", stacks = 1, duration = 3))

    // --- ORANGE ---
    val ORANGE_EMBERGAIN = off("off_orange_embergain", Targeting.SINGLE_ALLY,
        Effect.ApplyStatus(statusId = "ignite", stacks = 1, duration = 2))
    val ORANGE_MATCHSTICK = off("off_orange_matchstick", Targeting.SINGLE_ALLY,
        Effect.ApplyStatus(statusId = "fire_shield", stacks = 1, duration = 3))
    val ORANGE_SMOKESCREEN = off("off_orange_smokescreen", Targeting.ALL_ALLIES,
        Effect.ApplyStatus(statusId = "dodge", stacks = 1, duration = 2))
    val ORANGE_ANGER = off("off_orange_anger", Targeting.SINGLE_ALLY,
        Effect.ApplyStatus(statusId = "anger", stacks = 1, duration = 3))
    val ORANGE_SKIES = off("off_orange_skies", Targeting.SINGLE_ALLY,
        Effect.ApplyStatus(statusId = "counter", stacks = 1, duration = 3))

    // --- YELLOW ---
    val YELLOW_MEDKIT = off("off_yellow_medkit", Targeting.SINGLE_ALLY,
        Effect.Heal(amount = 20))
    val YELLOW_LIGHT = off("off_yellow_light", Targeting.SINGLE_ALLY,
        Effect.ApplyStatus(statusId = "regen", stacks = 1, duration = 3))
    val YELLOW_OVERFLOW = off("off_yellow_overflow", Targeting.SELF,
        Effect.HealAllAllies(amount = 8, overflowToShield = true))
    val YELLOW_KISSES = off("off_yellow_kisses", Targeting.SINGLE_ALLY,
        Effect.ApplyStatus(statusId = "kiss", stacks = 1, duration = 2))
    val YELLOW_SUNRISE = off("off_yellow_sunrise", Targeting.SINGLE_ALLY,
        Effect.Cleanse,
        Effect.ApplyStatus(statusId = "immunity", stacks = 1, duration = 2))

    // --- GREEN ---
    val GREEN_QUICKSTEP = off("off_green_quickstep", Targeting.SINGLE_ALLY,
        Effect.GrantExtraActions(count = 1))
    val GREEN_SENDER = off("off_green_sender", Targeting.SINGLE_ALLY,
        Effect.ApplyStatus(statusId = "reflect", stacks = 1, duration = 1))
    val GREEN_MIRROR = off("off_green_mirror", Targeting.SINGLE_ALLY,
        Effect.ApplyStatus(statusId = "echo", stacks = 1, duration = 3))
    val GREEN_BOON = off("off_green_boon", Targeting.SINGLE_ALLY,
        Effect.ApplyStatus(statusId = "wind_shield", stacks = 1, duration = 2))
    val GREEN_BARK = off("off_green_bark", Targeting.SINGLE_ALLY,
        Effect.Heal(amount = 2),
        Effect.HealAllAllies(amount = 6))

    // --- BLUE ---
    val BLUE_TOWER = off("off_blue_tower", Targeting.SINGLE_ALLY,
        Effect.GainShield(amount = 15))
    val BLUE_SPILLED = off("off_blue_spilled", Targeting.SINGLE_ALLY,
        Effect.GainShield(amount = 5),
        Effect.Conditional(
            condition = Condition.TargetAtOrAboveHp(0.8f),
            then = Effect.ApplyStatus(statusId = "keen", stacks = 1, duration = 2)
        ))
    val BLUE_WALL = off("off_blue_wall", Targeting.ALL_ALLIES,
        Effect.GainShield(amount = 6))
    val BLUE_CURRENT = off("off_blue_current", Targeting.SINGLE_ALLY,
        Effect.Cleanse,
        Effect.GainShield(amount = 3))
    val BLUE_WEIGHT = off("off_blue_weight", Targeting.SINGLE_ALLY,
        Effect.Conditional(
            condition = Condition.TargetBelowHp(0.5f),
            then = Effect.GainShield(amount = 20),
            otherwise = Effect.GainShield(amount = 8)
        ))

    // --- VIOLET ---
    val VIOLET_CLOAK = off("off_violet_cloak", Targeting.SINGLE_ALLY,
        Effect.ApplyStatus(statusId = "cloak", stacks = 1, duration = 1))
    val VIOLET_PAYBACK = off("off_violet_payback", Targeting.SINGLE_ALLY,
        Effect.ApplyStatus(statusId = "payback", stacks = 1, duration = 2))
    val VIOLET_OMEN = off("off_violet_omen", Targeting.SINGLE_ALLY,
        Effect.ApplyStatus(statusId = "omen", stacks = 1, duration = 2))
    val VIOLET_LASTLAUGH = off("off_violet_lastlaugh", Targeting.SINGLE_ALLY,
        Effect.Conditional(
            condition = Condition.TargetBelowHp(0.5f),
            then = Effect.ApplyStatus(statusId = "ward", stacks = 1, duration = 2),
            otherwise = Effect.ApplyStatus(statusId = "spite", stacks = 1, duration = 2)
        ))
    val VIOLET_THIEF = off("off_violet_thief", Targeting.SINGLE_ALLY,
        Effect.ApplyStatus(statusId = "thief", stacks = 1, duration = 3))

    val ALL = listOf(
        RED_BUCKLER, RED_VIAL, RED_PROTECTOR, RED_PEST, RED_WARCRY,
        ORANGE_EMBERGAIN, ORANGE_MATCHSTICK, ORANGE_SMOKESCREEN, ORANGE_ANGER, ORANGE_SKIES,
        YELLOW_MEDKIT, YELLOW_LIGHT, YELLOW_OVERFLOW, YELLOW_KISSES, YELLOW_SUNRISE,
        GREEN_QUICKSTEP, GREEN_SENDER, GREEN_MIRROR, GREEN_BOON, GREEN_BARK,
        BLUE_TOWER, BLUE_SPILLED, BLUE_WALL, BLUE_CURRENT, BLUE_WEIGHT,
        VIOLET_CLOAK, VIOLET_PAYBACK, VIOLET_OMEN, VIOLET_LASTLAUGH, VIOLET_THIEF
    )
    val REGISTRY: Map<String, Offhand> = ALL.associateBy { it.id }
}

// ----------------------------------------------------------------------------
//  ULTIMATES (1 per hero) — fired by dragging the full party meter
// ----------------------------------------------------------------------------

object Ultimates {

    // Ultimates are built per hero level so numbers can grow. Red's is the
    // scaling one for now; the rest hold their values at every level.
    fun forLevel(id: String, level: Int): Ability = when (id) {

        // RED - one colossal, exact hit. No dice. Grows with his level.
        "ult_red" -> Ability(
            id = "ult_red", iconId = "ic_ult_red",
            targeting = Targeting.HIGHEST_HP_ENEMY,
            effects = listOf(Effect.DealFlatDamage(amount = 50 + (level - 1)))
        )

        // ORANGE - 20 exact damage to everyone and a scorch that burns a
        // flat 10 per turn for 4 turns without fading.
        "ult_orange" -> Ability(
            id = "ult_orange", iconId = "ic_ult_orange",
            targeting = Targeting.ALL_ENEMIES,
            effects = listOf(
                Effect.DealFlatDamage(amount = 20),
                Effect.ApplyStatus(statusId = "scorch", stacks = 2, duration = 4)
            )
        )

        // YELLOW - the whole party back to 80% of max from wherever they are.
        "ult_yellow" -> Ability(
            id = "ult_yellow", iconId = "ic_ult_yellow",
            targeting = Targeting.ALL_ALLIES,
            effects = listOf(Effect.HealPercent(fraction = 0.8f))
        )

        // GREEN - time itself: everyone gets two extra turns this round.
        "ult_green" -> Ability(
            id = "ult_green", iconId = "ic_ult_green",
            targeting = Targeting.ALL_ALLIES,
            effects = listOf(Effect.GrantExtraActions(count = 2))
        )

        // BLUE - Bubble: the whole party ignores the next hit coming for them.
        "ult_blue" -> Ability(
            id = "ult_blue", iconId = "ic_ult_blue",
            targeting = Targeting.ALL_ALLIES,
            effects = listOf(Effect.ApplyStatus(statusId = "bubble", stacks = 1, duration = 3))
        )

        // VIOLET - 10 exact damage and the whole enemy line frozen two turns.
        "ult_violet" -> Ability(
            id = "ult_violet", iconId = "ic_ult_violet",
            targeting = Targeting.ALL_ENEMIES,
            effects = listOf(
                Effect.DealFlatDamage(amount = 10),
                Effect.ApplyStatus(statusId = "stun", stacks = 2, duration = 2)
            )
        )

        // ASH - no burst at all: everything just starts dying slowly.
        "ult_ash" -> Ability(
            id = "ult_ash", iconId = "ic_ult_ash",
            targeting = Targeting.ALL_ENEMIES,
            effects = listOf(
                Effect.ApplyStatus(statusId = "burn", stacks = 2, duration = 3),
                Effect.ApplyStatus(statusId = "poison", stacks = 2, duration = 3)
            )
        )

        // SILVER - the boss's storm, in your hands once earned.
        "ult_silver" -> Ability(
            id = "ult_silver", iconId = "ic_ult_silver",
            targeting = Targeting.ALL_ENEMIES,
            effects = listOf(
                Effect.DealDamage(multiplier = 0.9f, hits = 1, canCrit = false),
                Effect.ApplyStatus(statusId = "weaken", stacks = 1, duration = 2)
            )
        )

        else -> error("unknown ultimate $id")
    }

    val IDS = listOf(
        "ult_red", "ult_orange", "ult_yellow", "ult_green",
        "ult_blue", "ult_violet", "ult_ash", "ult_silver"
    )
    val REGISTRY: Map<String, Ability> = IDS.associateWith { forLevel(it, 1) }
}

// ----------------------------------------------------------------------------
//  ENEMY MOVE ABILITIES
// ----------------------------------------------------------------------------

object EnemyAbilities {

    val BASIC_SLASH = Ability(
        id = "basic_slash",
        iconId = "ic_atk_basic",
        // "enemy" is from the acting unit's perspective — a player unit here.
        targeting = Targeting.SINGLE_ENEMY,
        effects = listOf(Effect.DealDamage(multiplier = 1.0f, hits = 1, canCrit = false))
    )

    val SMALL_GUARD = Ability(
        id = "small_guard",
        iconId = "ic_def_small",
        targeting = Targeting.SELF,
        effects = listOf(Effect.GainShield(amount = 6))
    )

    val SMALL_HEAL = Ability(
        id = "small_heal",
        iconId = "ic_def_heal_s",
        targeting = Targeting.SELF,
        effects = listOf(Effect.Heal(amount = 8))
    )

    val VENOM_SPIT = Ability(
        id = "venom_spit",
        iconId = "ic_atk_venom",
        targeting = Targeting.SINGLE_ENEMY,
        effects = listOf(
            Effect.DealDamage(multiplier = 0.6f, hits = 1, canCrit = false),
            Effect.ApplyStatus(statusId = "poison", stacks = 1, duration = 3)
        )
    )

    // The Brute's charged payoff — hits the whole player line. Scary on purpose.
    val CRUSHING_BLOW = Ability(
        id = "crushing_blow",
        iconId = "ic_atk_crush",
        targeting = Targeting.ALL_ENEMIES,
        effects = listOf(Effect.DealDamage(multiplier = 1.4f, hits = 1, canCrit = false))
    )

    // The Hexer's charged payoff — heavy single hit that also Weakens.
    val DOOM_BOLT = Ability(
        id = "doom_bolt",
        iconId = "ic_atk_doom",
        targeting = Targeting.SINGLE_ENEMY,
        effects = listOf(
            Effect.DealDamage(multiplier = 2.2f, hits = 1, canCrit = false),
            Effect.ApplyStatus(statusId = "weaken", stacks = 1, duration = 2)
        )
    )

    // Ash's kit: smother everything in slow death, guard when pressed.
    val ASH_CLOUD = Ability(
        id = "ash_cloud",
        iconId = "ic_atk_ashcloud",
        targeting = Targeting.ALL_ENEMIES,
        effects = listOf(
            Effect.DealDamage(multiplier = 0.5f, hits = 1, canCrit = false),
            Effect.ApplyStatus(statusId = "burn", stacks = 1, duration = 2)
        )
    )

    val CINDER_SPIT = Ability(
        id = "cinder_spit",
        iconId = "ic_atk_cinder",
        targeting = Targeting.SINGLE_ENEMY,
        effects = listOf(
            Effect.DealDamage(multiplier = 1.0f, hits = 1, canCrit = false),
            Effect.ApplyStatus(statusId = "burn", stacks = 2, duration = 3)
        )
    )

    val EMBER_GUARD = Ability(
        id = "ember_guard",
        iconId = "ic_def_ember",
        targeting = Targeting.SELF,
        effects = listOf(Effect.GainShield(amount = 10))
    )

    // Gray's charged payoff for the final fight: the whole party hit and
    // hollowed out. Hard, not cheap - it telegraphs like everything else.
    val NULL_WAVE = Ability(
        id = "null_wave",
        iconId = "ic_atk_null",
        targeting = Targeting.ALL_ENEMIES,
        effects = listOf(
            Effect.DealDamage(multiplier = 1.2f, hits = 1, canCrit = false),
            Effect.ApplyStatus(statusId = "weaken", stacks = 1, duration = 2)
        )
    )

    // Silver's charged payoff — the whole party takes a hit and fights on
    // weakened. The boss teaches you to respect the ring.
    val SILVER_STORM = Ability(
        id = "silver_storm",
        iconId = "ic_atk_storm",
        targeting = Targeting.ALL_ENEMIES,
        effects = listOf(
            Effect.DealDamage(multiplier = 1.1f, hits = 1, canCrit = false),
            Effect.ApplyStatus(statusId = "weaken", stacks = 1, duration = 2)
        )
    )

    val ALL = listOf(
        BASIC_SLASH, SMALL_GUARD, SMALL_HEAL, VENOM_SPIT,
        CRUSHING_BLOW, DOOM_BOLT, ASH_CLOUD, CINDER_SPIT, EMBER_GUARD, SILVER_STORM, NULL_WAVE
    )
    val REGISTRY: Map<String, Ability> = ALL.associateBy { it.id }
}
