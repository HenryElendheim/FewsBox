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
    DAMAGE_TAKEN_UP,      // Vulnerable: owner takes magnitude% more
    DAMAGE_TAKEN_DOWN,    // Fortify-likes, for later
    THORNS                // owner strikes back: magnitude * stacks flat damage per hit taken
}
