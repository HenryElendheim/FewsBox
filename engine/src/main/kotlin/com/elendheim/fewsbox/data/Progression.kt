package com.elendheim.fewsbox.data

/**
 * Hero growth. Winning battles feeds XP to everyone who fought; levels are
 * pure stat growth — health and damage, capped at 50. The kit itself is
 * never level-gated: weapons and offhands are bought with fews in the shop,
 * and every piece is a sidegrade, so gear is customization, not power.
 */
object Progression {

    const val MAX_LEVEL = 50

    /** Total XP needed to SIT at [level]: a gentle quadratic ramp. */
    fun xpForLevel(level: Int): Int {
        val n = level.coerceIn(1, MAX_LEVEL) - 1
        return 15 * n * (n + 1)  // level 2 at 30 XP, 10 at 1350, 50 at 36750
    }

    fun levelFor(xp: Int): Int {
        var level = 1
        while (level < MAX_LEVEL && xp >= xpForLevel(level + 1)) level++
        return level
    }

    /** XP still needed for the next level, or null at cap. */
    fun xpToNext(xp: Int): Int? {
        val level = levelFor(xp)
        if (level >= MAX_LEVEL) return null
        return xpForLevel(level + 1) - xp
    }

    /** Fraction of the way from the current level to the next, for XP bars. */
    fun levelProgress(xp: Int): Float {
        val level = levelFor(xp)
        if (level >= MAX_LEVEL) return 1f
        val floor = xpForLevel(level)
        val ceil = xpForLevel(level + 1)
        return ((xp - floor).toFloat() / (ceil - floor)).coerceIn(0f, 1f)
    }

    // Stat growth per level above 1: steady HP, ATK every third level.
    fun bonusHp(level: Int): Int = 2 * (level - 1)
    fun bonusAttack(level: Int): Int = (level - 1) / 3
}
