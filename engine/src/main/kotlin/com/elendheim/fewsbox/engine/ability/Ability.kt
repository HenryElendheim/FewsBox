package com.elendheim.fewsbox.engine.ability

/**
 * An ability is a targeting rule plus a list of composable effects.
 * Content is data, not code: new combos come from stacking effects and
 * statuses, never from new engine branches.
 */
data class Ability(
    val id: String,
    val iconId: String,
    val targeting: Targeting,
    val cooldown: Int = 0,           // turns before reusable, 0 = every turn
    val effects: List<Effect>        // resolved in order
)

enum class Targeting {
    SINGLE_ENEMY,
    HIGHEST_HP_ENEMY,       // auto-aims at the healthiest enemy
    ALL_ENEMIES,
    RANDOM_ENEMY,
    RANDOM_ENEMIES_MULTI,   // multi-hit spread across random valid targets
    SELF,
    SINGLE_ALLY,
    ALL_ALLIES,
    ADJACENT_ENEMIES        // cleave: target plus its neighbors
}
