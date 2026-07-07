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

    val ALL = listOf(BURN, POISON, STUN, WEAKEN, VULNERABLE, TAUNT)
    val REGISTRY: Map<String, StatusDef> = ALL.associateBy { it.id }
}

// ----------------------------------------------------------------------------
//  WEAPONS (6) — each grants one offensive ability
// ----------------------------------------------------------------------------

object Weapons {

    // 1. CLEAVER — single hard hit. The reliable baseline.
    val CLEAVER = Weapon(
        id = "wpn_cleaver",
        iconId = "ic_wpn_cleaver",
        attackBonus = 2,
        grantedAbility = Ability(
            id = "atk_cleave",
            iconId = "ic_atk_cleave",
            targeting = Targeting.SINGLE_ENEMY,
            cost = 2,
            effects = listOf(
                Effect.DealDamage(multiplier = 1.6f, hits = 1, canCrit = true)
            )
        )
    )

    // 2. FAN BLADES — 3 hits spread across random enemies. Trash-mob clearer.
    val FAN_BLADES = Weapon(
        id = "wpn_fan_blades",
        iconId = "ic_wpn_fan",
        grantedAbility = Ability(
            id = "atk_fan",
            iconId = "ic_atk_fan",
            targeting = Targeting.RANDOM_ENEMIES_MULTI,
            cost = 3,
            effects = listOf(
                Effect.DealDamage(multiplier = 0.7f, hits = 3, canCrit = true)
            )
        )
    )

    // 3. PIERCER — 3 hits all on one target. Shreds a single elite.
    val PIERCER = Weapon(
        id = "wpn_piercer",
        iconId = "ic_wpn_piercer",
        grantedAbility = Ability(
            id = "atk_pierce",
            iconId = "ic_atk_pierce",
            targeting = Targeting.SINGLE_ENEMY,
            cost = 3,
            effects = listOf(
                Effect.DealDamage(multiplier = 0.65f, hits = 3, canCrit = true)
            )
        )
    )

    // 4. EMBER BLADE — moderate hit + applies Burn. Sets up detonation combos.
    val EMBER_BLADE = Weapon(
        id = "wpn_ember_blade",
        iconId = "ic_wpn_ember",
        grantedAbility = Ability(
            id = "atk_ember",
            iconId = "ic_atk_ember",
            targeting = Targeting.SINGLE_ENEMY,
            cost = 2,
            effects = listOf(
                Effect.DealDamage(multiplier = 1.0f, hits = 1, canCrit = true),
                Effect.ApplyStatus(statusId = "burn", stacks = 2, duration = 3)
            )
        )
    )

    // 5. REAPER — execute: big bonus below 30% HP. The finisher.
    val REAPER = Weapon(
        id = "wpn_reaper",
        iconId = "ic_wpn_reaper",
        attackBonus = 1,
        grantedAbility = Ability(
            id = "atk_reap",
            iconId = "ic_atk_reap",
            targeting = Targeting.SINGLE_ENEMY,
            cost = 2,
            effects = listOf(
                Effect.ExecuteDamage(
                    multiplier = 1.0f,
                    hpThreshold = 0.30f,
                    bonusMultiplier = 1.5f
                )
            )
        )
    )

    // 6. LEECH — lifesteal. Sustain through offense.
    val LEECH = Weapon(
        id = "wpn_leech",
        iconId = "ic_wpn_leech",
        grantedAbility = Ability(
            id = "atk_leech",
            iconId = "ic_atk_leech",
            targeting = Targeting.SINGLE_ENEMY,
            cost = 2,
            effects = listOf(
                Effect.DealDamage(multiplier = 1.2f, hits = 1, canCrit = true),
                Effect.Lifesteal(fraction = 0.5f)
            )
        )
    )

    val ALL = listOf(CLEAVER, FAN_BLADES, PIERCER, EMBER_BLADE, REAPER, LEECH)
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
            cost = 2,
            effects = listOf(
                Effect.GainShield(amount = 12)
            )
        )
    )

    // 2. SPIKED SHIELD — lighter shield, also for any ally. Thorns later.
    val SPIKED_SHIELD = Offhand(
        id = "off_spiked_shield",
        iconId = "ic_off_spiked",
        grantedAbility = Ability(
            id = "def_spiked",
            iconId = "ic_def_spiked",
            targeting = Targeting.SINGLE_ALLY,
            cost = 2,
            effects = listOf(
                Effect.GainShield(amount = 8)
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
            cost = 2,
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
            cost = 2,
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
            cost = 2,
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
            cost = 2,
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

    val ALL = listOf(BASIC_SLASH, SMALL_GUARD, SMALL_HEAL, VENOM_SPIT, CRUSHING_BLOW, DOOM_BOLT)
    val REGISTRY: Map<String, Ability> = ALL.associateBy { it.id }
}
