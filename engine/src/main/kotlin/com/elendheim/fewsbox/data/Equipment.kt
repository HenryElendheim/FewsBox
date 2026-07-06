package com.elendheim.fewsbox.data

import com.elendheim.fewsbox.engine.ability.Ability

/**
 * Equipment is how the player assembles a unit's kit. A weapon contributes
 * the attack, an offhand contributes the defense/utility. That's it.
 */
data class Weapon(
    val id: String,
    val iconId: String,
    val grantedAbility: Ability,
    val attackBonus: Int = 0
)

data class Offhand(
    val id: String,
    val iconId: String,
    val grantedAbility: Ability
)

fun buildAbilities(weapon: Weapon, offhand: Offhand, extra: List<Ability> = emptyList()): List<Ability> =
    listOf(weapon.grantedAbility, offhand.grantedAbility) + extra
