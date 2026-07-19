package com.elendheim.fewsbox.data

import com.elendheim.fewsbox.engine.ability.Ability
import com.elendheim.fewsbox.engine.ability.Effect
import com.elendheim.fewsbox.engine.ability.Resolver
import com.elendheim.fewsbox.engine.ability.Targeting
import com.elendheim.fewsbox.engine.status.PassiveEffect
import com.elendheim.fewsbox.engine.status.StatusDef
import com.elendheim.fewsbox.engine.status.StatusKind
import com.elendheim.fewsbox.engine.status.StatusTiming

// ============================================================================
//  Starter content for the vertical slice. Every value here is a starting
//  point — tune freely. Content is data; the engine never changes for it.
// ============================================================================

object Statuses {

    // Damage over time. Ticks at the afflicted unit's turn start, decays one
    // stack per tick.
    val BURN = StatusDef(
        id = "burn",
        iconId = "ic_status_burn",
        kind = StatusKind.DEBUFF,
        timing = StatusTiming.TICK_START_OF_TURN,
        magnitude = 3,
        decaysOnTick = true
    )

    // Ramping damage over time: total tick = magnitude * stacks, stacks do
    // not decay — duration handles expiry, so stacking it up really hurts.
    val POISON = StatusDef(
        id = "poison",
        iconId = "ic_status_poison",
        kind = StatusKind.DEBUFF,
        timing = StatusTiming.TICK_START_OF_TURN,
        magnitude = 2
    )

    // Skip the next action. Consumed when the unit would act. Stunning a
    // charging elite also resets its wind-up.
    val STUN = StatusDef(
        id = "stun",
        iconId = "ic_status_stun",
        kind = StatusKind.DEBUFF,
        timing = StatusTiming.ON_APPLY_ONLY
    )

    // Afflicted unit deals 30% less damage while present.
    val WEAKEN = StatusDef(
        id = "weaken",
        iconId = "ic_status_weaken",
        kind = StatusKind.DEBUFF,
        timing = StatusTiming.PASSIVE_MODIFIER,
        magnitude = 30,
        passive = PassiveEffect.DAMAGE_DEALT_DOWN
    )

    // Afflicted unit takes 25% more damage while present.
    val VULNERABLE = StatusDef(
        id = "vulnerable",
        iconId = "ic_status_vulnerable",
        kind = StatusKind.DEBUFF,
        timing = StatusTiming.PASSIVE_MODIFIER,
        magnitude = 25,
        passive = PassiveEffect.DAMAGE_TAKEN_UP
    )

    // Built-in: the Banner's taunt rides on the status system like
    // everything else. BUFF kind keeps Cleanse from stripping it.
    val TAUNT = StatusDef(
        id = Resolver.TAUNT_STATUS_ID,
        iconId = "ic_status_taunt",
        kind = StatusKind.BUFF,
        timing = StatusTiming.PASSIVE_MODIFIER
    )

    // Strikes back at attackers: 3 flat damage per stack per hit taken.
    val THORNS = StatusDef(
        id = "thorns",
        iconId = "ic_status_thorns",
        kind = StatusKind.BUFF,
        timing = StatusTiming.PASSIVE_MODIFIER,
        magnitude = 3,
        passive = PassiveEffect.THORNS
    )

    val ALL = listOf(BURN, POISON, STUN, WEAKEN, VULNERABLE, TAUNT, THORNS)
    val REGISTRY: Map<String, StatusDef> = ALL.associateBy { it.id }
}

// ----------------------------------------------------------------------------
//  WEAPONS — three signature weapons per hero. No shared pool: every color
//  has its own arsenal, so kits read as personality, not shopping.
// ----------------------------------------------------------------------------

object Weapons {

    private fun weapon(id: String, bonus: Int = 0, ability: Ability) =
        Weapon(id = id, iconId = "ic_$id", attackBonus = bonus, grantedAbility = ability)

    private fun atk(id: String, targeting: Targeting, vararg effects: Effect) =
        Ability(id = "atk_$id", iconId = "ic_wpn_$id", targeting = targeting, effects = effects.toList())

    // --- RED: brutal single-target ---
    val RED_MAUL = weapon("wpn_red_maul", bonus = 2,
        ability = atk("red_maul", Targeting.SINGLE_ENEMY,
            Effect.DealDamage(multiplier = 1.8f, hits = 1, canCrit = true)))
    val RED_TWIN = weapon("wpn_red_twin",
        ability = atk("red_twin", Targeting.SINGLE_ENEMY,
            Effect.DealDamage(multiplier = 0.95f, hits = 2, canCrit = true)))
    val RED_GUILLOTINE = weapon("wpn_red_guillotine", bonus = 1,
        ability = atk("red_guillotine", Targeting.SINGLE_ENEMY,
            Effect.ExecuteDamage(multiplier = 1.0f, hpThreshold = 0.35f, bonusMultiplier = 1.4f)))

    // --- ORANGE: everything burns ---
    val ORANGE_BRAND = weapon("wpn_orange_brand",
        ability = atk("orange_brand", Targeting.SINGLE_ENEMY,
            Effect.DealDamage(multiplier = 1.0f, hits = 1, canCrit = true),
            Effect.ApplyStatus(statusId = "burn", stacks = 2, duration = 3)))
    val ORANGE_WHIP = weapon("wpn_orange_whip",
        ability = atk("orange_whip", Targeting.SINGLE_ENEMY,
            Effect.DealDamage(multiplier = 0.6f, hits = 2, canCrit = true),
            Effect.ApplyStatus(statusId = "burn", stacks = 1, duration = 3)))
    val ORANGE_FAN = weapon("wpn_orange_fan",
        ability = atk("orange_fan", Targeting.RANDOM_ENEMIES_MULTI,
            Effect.DealDamage(multiplier = 0.55f, hits = 3, canCrit = true)))

    // --- YELLOW: sustain and setup ---
    val YELLOW_SIPHON = weapon("wpn_yellow_siphon",
        ability = atk("yellow_siphon", Targeting.SINGLE_ENEMY,
            Effect.DealDamage(multiplier = 1.1f, hits = 1, canCrit = true),
            Effect.Lifesteal(fraction = 0.6f)))
    val YELLOW_LANCE = weapon("wpn_yellow_lance", bonus = 1,
        ability = atk("yellow_lance", Targeting.SINGLE_ENEMY,
            Effect.DealDamage(multiplier = 1.4f, hits = 1, canCrit = true)))
    val YELLOW_BELL = weapon("wpn_yellow_bell",
        ability = atk("yellow_bell", Targeting.SINGLE_ENEMY,
            Effect.DealDamage(multiplier = 0.9f, hits = 1, canCrit = false),
            Effect.ApplyStatus(statusId = "weaken", stacks = 1, duration = 2)))

    // --- GREEN: many blades ---
    val GREEN_FAN = weapon("wpn_green_fan",
        ability = atk("green_fan", Targeting.RANDOM_ENEMIES_MULTI,
            Effect.DealDamage(multiplier = 0.7f, hits = 3, canCrit = true)))
    val GREEN_VOLLEY = weapon("wpn_green_volley",
        ability = atk("green_volley", Targeting.RANDOM_ENEMIES_MULTI,
            Effect.DealDamage(multiplier = 0.5f, hits = 4, canCrit = true)))
    val GREEN_SCYTHE = weapon("wpn_green_scythe",
        ability = atk("green_scythe", Targeting.ADJACENT_ENEMIES,
            Effect.DealDamage(multiplier = 0.8f, hits = 1, canCrit = true)))

    // --- BLUE: steady pressure ---
    val BLUE_HAMMER = weapon("wpn_blue_hammer", bonus = 1,
        ability = atk("blue_hammer", Targeting.SINGLE_ENEMY,
            Effect.DealDamage(multiplier = 1.5f, hits = 1, canCrit = true)))
    val BLUE_PIKE = weapon("wpn_blue_pike",
        ability = atk("blue_pike", Targeting.SINGLE_ENEMY,
            Effect.DealDamage(multiplier = 0.6f, hits = 3, canCrit = true)))
    val BLUE_UNDERTOW = weapon("wpn_blue_undertow",
        ability = atk("blue_undertow", Targeting.SINGLE_ENEMY,
            Effect.DealDamage(multiplier = 0.75f, hits = 2, canCrit = true)))

    // --- VIOLET: death and hexes ---
    val VIOLET_REAPER = weapon("wpn_violet_reaper", bonus = 1,
        ability = atk("violet_reaper", Targeting.SINGLE_ENEMY,
            Effect.ExecuteDamage(multiplier = 1.0f, hpThreshold = 0.30f, bonusMultiplier = 1.5f)))
    val VIOLET_FANG = weapon("wpn_violet_fang",
        ability = atk("violet_fang", Targeting.SINGLE_ENEMY,
            Effect.DealDamage(multiplier = 1.3f, hits = 1, canCrit = true)))
    val VIOLET_NEEDLE = weapon("wpn_violet_needle",
        ability = atk("violet_needle", Targeting.SINGLE_ENEMY,
            Effect.DealDamage(multiplier = 0.7f, hits = 1, canCrit = true),
            Effect.ApplyStatus(statusId = "vulnerable", stacks = 1, duration = 2)))

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
        RED_MAUL, RED_TWIN, RED_GUILLOTINE,
        ORANGE_BRAND, ORANGE_WHIP, ORANGE_FAN,
        YELLOW_SIPHON, YELLOW_LANCE, YELLOW_BELL,
        GREEN_FAN, GREEN_VOLLEY, GREEN_SCYTHE,
        BLUE_HAMMER, BLUE_PIKE, BLUE_UNDERTOW,
        VIOLET_REAPER, VIOLET_FANG, VIOLET_NEEDLE,
        SILVER_EDGE, SILVER_LASH, SILVER_SPIKE
    )
    val REGISTRY: Map<String, Weapon> = ALL.associateBy { it.id }
}

// ----------------------------------------------------------------------------
//  OFFHANDS (6) — each grants one defensive / utility ability
// ----------------------------------------------------------------------------

object Offhands {

    // 1. TOWER SHIELD — large flat shield, handed to any ally (like Medkit).
    val TOWER_SHIELD = Offhand(
        id = "off_tower_shield",
        iconId = "ic_off_tower",
        grantedAbility = Ability(
            id = "def_tower",
            iconId = "ic_def_tower",
            targeting = Targeting.SINGLE_ALLY,
            effects = listOf(
                Effect.GainShield(amount = 12)
            )
        )
    )

    // 2. SPIKED SHIELD — lighter shield for any ally, and it bites back.
    val SPIKED_SHIELD = Offhand(
        id = "off_spiked_shield",
        iconId = "ic_off_spiked",
        grantedAbility = Ability(
            id = "def_spiked",
            iconId = "ic_def_spiked",
            targeting = Targeting.SINGLE_ALLY,
            effects = listOf(
                Effect.GainShield(amount = 8),
                Effect.ApplyStatus(statusId = "thorns", stacks = 1, duration = 2)
            )
        )
    )

    // 3. MEDKIT — direct heal to an ally.
    val MEDKIT = Offhand(
        id = "off_medkit",
        iconId = "ic_off_medkit",
        grantedAbility = Ability(
            id = "def_medkit",
            iconId = "ic_def_medkit",
            targeting = Targeting.SINGLE_ALLY,
            effects = listOf(
                Effect.Heal(amount = 14)
            )
        )
    )

    // 4. BANNER — taunt onto self + small shield. Protect the squishies.
    val BANNER = Offhand(
        id = "off_banner",
        iconId = "ic_off_banner",
        grantedAbility = Ability(
            id = "def_banner",
            iconId = "ic_def_banner",
            targeting = Targeting.SELF,
            effects = listOf(
                Effect.Taunt(turns = 1),
                Effect.GainShield(amount = 6)
            )
        )
    )

    // 5. DETONATOR — consume Burn on the target for burst. Ember Blade's payoff.
    val DETONATOR = Offhand(
        id = "off_detonator",
        iconId = "ic_off_detonator",
        grantedAbility = Ability(
            id = "def_detonate",
            iconId = "ic_def_detonate",
            targeting = Targeting.SINGLE_ENEMY,
            effects = listOf(
                Effect.ConsumeStatus(
                    statusId = "burn",
                    perStackEffect = Effect.DealDamage(multiplier = 0.8f, hits = 1, canCrit = false)
                )
            )
        )
    )

    // 6. CLEANSER — strip debuffs from an ally + small shield.
    val CLEANSER = Offhand(
        id = "off_cleanser",
        iconId = "ic_off_cleanser",
        grantedAbility = Ability(
            id = "def_cleanse",
            iconId = "ic_def_cleanse",
            targeting = Targeting.SINGLE_ALLY,
            effects = listOf(
                Effect.Cleanse,
                Effect.GainShield(amount = 5)
            )
        )
    )

    val ALL = listOf(TOWER_SHIELD, SPIKED_SHIELD, MEDKIT, BANNER, DETONATOR, CLEANSER)
    val REGISTRY: Map<String, Offhand> = ALL.associateBy { it.id }
}

// ----------------------------------------------------------------------------
//  ULTIMATES (1 per hero) — big cooldown-gated signature moves
// ----------------------------------------------------------------------------

object Ultimates {

    // RED — one enormous hit. Simple, honest, terrifying.
    val BERSERK = Ability(
        id = "ult_red",
        iconId = "ic_ult_red",
        targeting = Targeting.SINGLE_ENEMY,
        effects = listOf(
            Effect.DealDamage(multiplier = 2.4f, hits = 1, canCrit = true)
        )
    )

    // ORANGE — set the whole enemy line burning.
    val INFERNO = Ability(
        id = "ult_orange",
        iconId = "ic_ult_orange",
        targeting = Targeting.ALL_ENEMIES,
        effects = listOf(
            Effect.DealDamage(multiplier = 0.5f, hits = 1, canCrit = false),
            Effect.ApplyStatus(statusId = "burn", stacks = 2, duration = 3)
        )
    )

    // YELLOW — heal everyone.
    val SUNBURST = Ability(
        id = "ult_yellow",
        iconId = "ic_ult_yellow",
        targeting = Targeting.ALL_ALLIES,
        effects = listOf(
            Effect.Heal(amount = 10)
        )
    )

    // GREEN — five blades everywhere.
    val RAZOR_STORM = Ability(
        id = "ult_green",
        iconId = "ic_ult_green",
        targeting = Targeting.RANDOM_ENEMIES_MULTI,
        effects = listOf(
            Effect.DealDamage(multiplier = 0.6f, hits = 5, canCrit = true)
        )
    )

    // BLUE — shield the whole party.
    val PHALANX = Ability(
        id = "ult_blue",
        iconId = "ic_ult_blue",
        targeting = Targeting.ALL_ALLIES,
        effects = listOf(
            Effect.GainShield(amount = 8)
        )
    )

    // VIOLET — a hit that stuns. The reliable Lockdown button, gated hard.
    val TERROR = Ability(
        id = "ult_violet",
        iconId = "ic_ult_violet",
        targeting = Targeting.SINGLE_ENEMY,
        effects = listOf(
            Effect.DealDamage(multiplier = 0.8f, hits = 1, canCrit = false),
            Effect.ApplyStatus(statusId = "stun", stacks = 1, duration = 1)
        )
    )

    // SILVER — the boss's storm, in your hands once earned. Slightly tuned
    // down from the enemy version.
    val TEMPEST = Ability(
        id = "ult_silver",
        iconId = "ic_ult_silver",
        targeting = Targeting.ALL_ENEMIES,
        effects = listOf(
            Effect.DealDamage(multiplier = 0.9f, hits = 1, canCrit = false),
            Effect.ApplyStatus(statusId = "weaken", stacks = 1, duration = 2)
        )
    )

    val ALL = listOf(BERSERK, INFERNO, SUNBURST, RAZOR_STORM, PHALANX, TERROR, TEMPEST)
    val REGISTRY: Map<String, Ability> = ALL.associateBy { it.id }
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

    // Silver's charged payoff — the whole party takes a hit and fights on
    // weakened. The first boss teaches you to respect the ring.
    val SILVER_STORM = Ability(
        id = "silver_storm",
        iconId = "ic_atk_storm",
        targeting = Targeting.ALL_ENEMIES,
        effects = listOf(
            Effect.DealDamage(multiplier = 1.1f, hits = 1, canCrit = false),
            Effect.ApplyStatus(statusId = "weaken", stacks = 1, duration = 2)
        )
    )

    val ALL = listOf(BASIC_SLASH, SMALL_GUARD, SMALL_HEAL, VENOM_SPIT, CRUSHING_BLOW, DOOM_BOLT, SILVER_STORM)
    val REGISTRY: Map<String, Ability> = ALL.associateBy { it.id }
}
