package com.elendheim.fewsbox.engine.status

data class StatusDef(
    val id: String,
    val iconId: String,
    val kind: StatusKind,
    val timing: StatusTiming,
    val magnitude: Int = 0,          // meaning depends on timing/passive
    val passive: PassiveEffect? = null,  // set when timing == PASSIVE_MODIFIER
    val decaysOnTick: Boolean = false    // Burn loses a stack per tick, Poison doesn't
)

enum class StatusKind { DEBUFF, BUFF }

enum class StatusTiming {
    TICK_START_OF_TURN,   // Burn, Poison: magnitude * stacks damage at owner's turn start
    ON_OWNER_ACTION,      // Bleed-likes: trigger when the afflicted unit acts
    PASSIVE_MODIFIER,     // Weaken / Vulnerable: modify damage while present
    ON_APPLY_ONLY         // Stun: consumed to skip a turn
}

/** What a passive status actually modifies. Percentages come from magnitude. */
enum class PassiveEffect {
    DAMAGE_DEALT_DOWN,    // Weaken: owner deals magnitude% less
    DAMAGE_DEALT_UP,      // War Cry-likes: owner deals magnitude% more
    DAMAGE_DEALT_UP_FLAT, // Spilled: owner deals +magnitude flat per hit
    DAMAGE_TAKEN_UP,      // Vulnerable: owner takes magnitude% more
    DAMAGE_TAKEN_DOWN,    // Protector-likes: owner takes magnitude% less
    THORNS,               // owner strikes back: magnitude * stacks flat damage per hit taken
    DODGE,                // magnitude% chance to take nothing from a hit
    COUNTER,              // owner swings back at attackers off-turn, no action cost
    REFLECT_PERCENT,      // owner returns magnitude% of damage taken to the attacker
    BURN_REFLECT,         // attackers catch fire scaled to the damage they dealt
    HEAL_WHEN_HIT,        // owner instantly heals magnitude% of damage taken
    ON_HIT_APPLY_BURN,    // owner's hits set targets on fire
    ON_HIT_APPLY_VULN,    // owner's hits leave targets vulnerable
    PUNISH_WEAKEN,        // attackers get weakened for daring
    PUNISH_WIND,          // attackers catch the wind for daring
    MISS_CHANCE,          // owner's attacks have magnitude% chance to whiff
    LURE,                 // owner is forced to attack the healthiest opposing unit
    UNTARGETABLE,         // enemies cannot pick the owner as a target
    DEBUFF_IMMUNE,        // new debuffs bounce off
    DEATH_WARD,           // a killing blow leaves the owner at 1 HP, once
    NEGATE_HIT,           // the next hit is ignored entirely, once
    ULT_TICK,             // +magnitude% party ult at the end of each round
    ECHO,                 // owner's attacks repeat at magnitude% damage for free
    THIEF                 // owner's hits shake magnitude fews loose per hit
}
