package com.elendheim.fewsbox.data

/**
 * Hero growth. Winning battles feeds XP to everyone who fought; hero levels
 * unlock the mixing itself - you start with one signature weapon and two
 * offhands, and every level opens more of the kit. Small stat growth rides
 * along so veterans feel like veterans.
 */
object Progression {

    const val MAX_LEVEL = 5

    // Total XP needed to sit at each level (index 0 = level 1).
    val LEVEL_XP = listOf(0, 60, 150, 300, 500)

    fun levelFor(xp: Int): Int {
        var level = 1
        for (i in LEVEL_XP.indices) {
            if (xp >= LEVEL_XP[i]) level = i + 1
        }
        return level.coerceAtMost(MAX_LEVEL)
    }

    /** XP still needed for the next level, or null at cap. */
    fun xpToNext(xp: Int): Int? {
        val level = levelFor(xp)
        if (level >= MAX_LEVEL) return null
        return LEVEL_XP[level] - xp
    }

    /** Winning battle [battleIndex] pays this much XP to each fighter. */
    fun xpRewardFor(battleIndex: Int): Int = 10 + battleIndex * 2

    // ------------------------------------------------------------------
    //  What a hero level unlocks. Weapon list order IS the unlock order,
    //  same for offhands; defaults sit first so level 1 is always legal.
    // ------------------------------------------------------------------

    /** Weapons available at [level]: one at level 1, all three by level 3. */
    fun unlockedWeapons(hero: HeroDef, level: Int): List<String> =
        hero.weaponIds.take(level.coerceIn(1, hero.weaponIds.size))

    /** Offhands available at [level]: two at level 1, one more per level. */
    fun unlockedOffhands(hero: HeroDef, level: Int): List<String> =
        hero.offhandIds.take((level + 1).coerceIn(2, hero.offhandIds.size))

    /** The hero level at which weapon slot [index] opens. */
    fun weaponUnlockLevel(index: Int): Int = index + 1

    /** The hero level at which offhand slot [index] opens. */
    fun offhandUnlockLevel(index: Int): Int = (index - 1).coerceAtLeast(1)

    // Stat growth per level above 1: steady HP, ATK on odd levels.
    fun bonusHp(level: Int): Int = 5 * (level - 1)
    fun bonusAttack(level: Int): Int = (level - 1) / 2
}
