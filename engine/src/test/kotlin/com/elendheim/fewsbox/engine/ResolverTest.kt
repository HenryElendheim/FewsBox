package com.elendheim.fewsbox.engine

import com.elendheim.fewsbox.data.Offhands
import com.elendheim.fewsbox.data.Party
import com.elendheim.fewsbox.data.Statuses
import com.elendheim.fewsbox.data.Weapons
import com.elendheim.fewsbox.data.toUnit
import com.elendheim.fewsbox.engine.ability.Ability
import com.elendheim.fewsbox.engine.ability.Condition
import com.elendheim.fewsbox.engine.ability.Effect
import com.elendheim.fewsbox.engine.ability.Resolver
import com.elendheim.fewsbox.engine.ability.Targeting
import com.elendheim.fewsbox.engine.event.CombatEvent
import com.elendheim.fewsbox.engine.model.Team
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResolverTest {

    private fun resolver(recorder: Recorder, seed: Int = 42) =
        Resolver(Statuses.REGISTRY, Random(seed), recorder.emit)

    // ------------------------------------------------------------------
    //  Shields
    // ------------------------------------------------------------------

    @Test
    fun `shield absorbs damage before hp`() {
        val rec = Recorder()
        val attacker = unit("a", Team.PLAYER, attack = 10)
        val target = unit("t", Team.ENEMY, hp = 50).apply { shield = 6 }
        val state = battle(attacker, target)

        resolver(rec).resolve(state, attacker, plainHit(1.0f), listOf("t"))

        assertEquals(0, target.shield)
        assertEquals(46, target.hp) // 10 dmg: 6 into shield, 4 into hp
        assertTrue(rec.all<CombatEvent.ShieldBroken>().isNotEmpty())
    }

    @Test
    fun `shield fully absorbs small hit`() {
        val rec = Recorder()
        val attacker = unit("a", Team.PLAYER, attack = 5)
        val target = unit("t", Team.ENEMY, hp = 50).apply { shield = 12 }
        val state = battle(attacker, target)

        resolver(rec).resolve(state, attacker, plainHit(1.0f), listOf("t"))

        assertEquals(7, target.shield)
        assertEquals(50, target.hp)
        assertTrue(rec.all<CombatEvent.ShieldBroken>().isEmpty())
    }

    // ------------------------------------------------------------------
    //  The signature combo: Burn + Detonate
    // ------------------------------------------------------------------

    @Test
    fun `burn apply then detonate deals exact per-stack damage`() {
        val rec = Recorder()
        val attacker = unit("a", Team.PLAYER, attack = 10)
        val target = unit("t", Team.ENEMY, hp = 100)
        val state = battle(attacker, target)
        val r = resolver(rec)

        val ember = Ability(
            id = "ember", iconId = "x", targeting = Targeting.SINGLE_ENEMY,
            effects = listOf(
                Effect.DealDamage(multiplier = 1.0f, canCrit = false),
                Effect.ApplyStatus("burn", stacks = 2, duration = 3)
            )
        )
        r.resolve(state, attacker, ember, listOf("t"))
        assertEquals(90, target.hp)
        assertEquals(2, target.statusStacks("burn"))

        val detonate = Ability(
            id = "det", iconId = "x", targeting = Targeting.SINGLE_ENEMY,
            effects = listOf(
                Effect.ConsumeStatus("burn", Effect.DealDamage(multiplier = 0.8f, canCrit = false))
            )
        )
        r.resolve(state, attacker, detonate, listOf("t"))

        // 2 stacks * (10 * 0.8) = 16
        assertEquals(74, target.hp)
        assertEquals(0, target.statusStacks("burn"))
        val consumed = rec.all<CombatEvent.StatusConsumed>().single()
        assertEquals(2, consumed.stacks)
    }

    @Test
    fun `detonate with no burn does nothing`() {
        val rec = Recorder()
        val attacker = unit("a", Team.PLAYER, attack = 10)
        val target = unit("t", Team.ENEMY, hp = 100)
        val state = battle(attacker, target)

        val detonate = Ability(
            id = "det", iconId = "x", targeting = Targeting.SINGLE_ENEMY,
            effects = listOf(
                Effect.ConsumeStatus("burn", Effect.DealDamage(multiplier = 0.8f, canCrit = false))
            )
        )
        resolver(rec).resolve(state, attacker, detonate, listOf("t"))

        assertEquals(100, target.hp)
        assertTrue(rec.all<CombatEvent.StatusConsumed>().isEmpty())
    }

    // ------------------------------------------------------------------
    //  Execute, lifesteal, heal
    // ------------------------------------------------------------------

    @Test
    fun `execute hits harder below threshold`() {
        val rec = Recorder()
        val attacker = unit("a", Team.PLAYER, attack = 10)
        val healthy = unit("h", Team.ENEMY, hp = 100)
        val hurt = unit("w", Team.ENEMY, hp = 100).apply { hp = 20 }
        val state = battle(attacker, healthy, hurt)
        val r = resolver(rec)

        val execute = Ability(
            id = "reap", iconId = "x", targeting = Targeting.SINGLE_ENEMY,
            effects = listOf(Effect.ExecuteDamage(multiplier = 1.0f, hpThreshold = 0.30f, bonusMultiplier = 1.5f))
        )
        r.resolve(state, attacker, execute, listOf("h"))
        r.resolve(state, attacker, execute, listOf("w"))

        // Executes can crit, so derive the expectation from the crit flag.
        val hits = rec.all<CombatEvent.DamageDealt>()
        assertEquals(if (hits[0].isCrit) 15 else 10, hits[0].amount)  // above threshold: 1.0x
        assertEquals(if (hits[1].isCrit) 38 else 25, hits[1].amount)  // below: (1.0 + 1.5)x
    }

    @Test
    fun `lifesteal heals attacker from damage dealt`() {
        val rec = Recorder()
        val attacker = unit("a", Team.PLAYER, hp = 50, attack = 10).apply { hp = 30 }
        val target = unit("t", Team.ENEMY, hp = 100)
        val state = battle(attacker, target)

        val leech = Ability(
            id = "leech", iconId = "x", targeting = Targeting.SINGLE_ENEMY,
            effects = listOf(
                Effect.DealDamage(multiplier = 1.2f, canCrit = false),
                Effect.Lifesteal(fraction = 0.5f)
            )
        )
        resolver(rec).resolve(state, attacker, leech, listOf("t"))

        assertEquals(88, target.hp)  // 12 damage
        assertEquals(36, attacker.hp) // healed 6
    }

    @Test
    fun `heal never overshoots max hp`() {
        val rec = Recorder()
        val healer = unit("a", Team.PLAYER, hp = 50).apply { hp = 45 }
        val state = battle(healer)

        val medkit = Ability(
            id = "med", iconId = "x", targeting = Targeting.SELF,
            effects = listOf(Effect.Heal(amount = 14))
        )
        resolver(rec).resolve(state, healer, medkit, listOf())

        assertEquals(50, healer.hp)
        assertEquals(5, rec.all<CombatEvent.Healed>().single().amount)
    }

    // ------------------------------------------------------------------
    //  Passive modifiers
    // ------------------------------------------------------------------

    @Test
    fun `weaken reduces outgoing and vulnerable raises incoming damage`() {
        val rec = Recorder()
        val attacker = unit("a", Team.PLAYER, attack = 10)
        val target = unit("t", Team.ENEMY, hp = 100)
        val state = battle(attacker, target)
        val r = resolver(rec)

        r.addStatus(attacker, "weaken", 1, 2)     // -30% out
        r.addStatus(target, "vulnerable", 1, 2)   // +15% in
        r.resolve(state, attacker, plainHit(1.0f), listOf("t"))

        // 10 * 0.7 * 1.15 = 8.05 -> 8
        assertEquals(8, rec.all<CombatEvent.DamageDealt>().single().amount)
    }

    // ------------------------------------------------------------------
    //  Taunt, cleanse, conditionals, spread
    // ------------------------------------------------------------------

    @Test
    fun `taunt redirects single-target attacks`() {
        val rec = Recorder()
        val enemy = unit("e", Team.ENEMY, attack = 10)
        val tank = unit("tank", Team.PLAYER, hp = 60)
        val squishy = unit("squishy", Team.PLAYER, hp = 30)
        val state = battle(enemy, tank, squishy)
        val r = resolver(rec)

        r.addStatus(tank, Resolver.TAUNT_STATUS_ID, 1, 1)
        r.resolve(state, enemy, plainHit(1.0f), listOf("squishy"))

        assertEquals(50, tank.hp)     // hit landed here despite targeting squishy
        assertEquals(30, squishy.hp)
    }

    @Test
    fun `cleanse strips debuffs but not buffs`() {
        val rec = Recorder()
        val ally = unit("p", Team.PLAYER)
        val state = battle(ally)
        val r = resolver(rec)

        r.addStatus(ally, "burn", 2, 3)
        r.addStatus(ally, "weaken", 1, 2)
        r.addStatus(ally, Resolver.TAUNT_STATUS_ID, 1, 1) // BUFF kind

        val cleanse = Ability(
            id = "cl", iconId = "x", targeting = Targeting.SELF,
            effects = listOf(Effect.Cleanse)
        )
        r.resolve(state, ally, cleanse, listOf())

        assertFalse(ally.hasStatus("burn"))
        assertFalse(ally.hasStatus("weaken"))
        assertTrue(ally.hasStatus(Resolver.TAUNT_STATUS_ID))
    }

    @Test
    fun `conditional only fires when condition holds`() {
        val rec = Recorder()
        val attacker = unit("a", Team.PLAYER, attack = 10)
        val target = unit("t", Team.ENEMY, hp = 100)
        val state = battle(attacker, target)
        val r = resolver(rec)

        val bonusVsBurning = Ability(
            id = "b", iconId = "x", targeting = Targeting.SINGLE_ENEMY,
            effects = listOf(
                Effect.Conditional(
                    Condition.TargetHasStatus("burn"),
                    Effect.DealDamage(multiplier = 1.0f, canCrit = false)
                )
            )
        )
        r.resolve(state, attacker, bonusVsBurning, listOf("t"))
        assertEquals(100, target.hp) // no burn, no hit

        r.addStatus(target, "burn", 1, 3)
        r.resolve(state, attacker, bonusVsBurning, listOf("t"))
        assertEquals(90, target.hp)
    }

    @Test
    fun `spread multi-hit lands every hit on living enemies`() {
        val rec = Recorder()
        val attacker = unit("a", Team.PLAYER, attack = 10)
        val e1 = unit("e1", Team.ENEMY, hp = 100)
        val e2 = unit("e2", Team.ENEMY, hp = 100)
        val e3 = unit("e3", Team.ENEMY, hp = 100)
        val state = battle(attacker, e1, e2, e3)

        val fan = plainHit(0.7f, hits = 3, targeting = Targeting.RANDOM_ENEMIES_MULTI)
        resolver(rec).resolve(state, attacker, fan, listOf())

        val hits = rec.all<CombatEvent.DamageDealt>()
        assertEquals(3, hits.size)
        assertTrue(hits.all { it.amount == 7 })
        val totalLost = (300 - e1.hp - e2.hp - e3.hp)
        assertEquals(21, totalLost)
    }

    @Test
    fun `thorns strikes back per hit without feeding lifesteal`() {
        val rec = Recorder()
        val attacker = unit("a", Team.PLAYER, hp = 50, attack = 10)
        val target = unit("t", Team.ENEMY, hp = 100)
        val state = battle(attacker, target)
        val r = resolver(rec)

        r.addStatus(target, "thorns", stacks = 2, duration = 2) // 3 dmg per stack
        r.resolve(state, attacker, plainHit(1.0f, hits = 2), listOf("t"))

        assertEquals(80, target.hp)      // two 10s in
        assertEquals(38, attacker.hp)    // two 6s reflected back
        // Reflected hits land on the attacker, unflagged as crits
        val onAttacker = rec.all<CombatEvent.DamageDealt>().filter { it.targetId == "a" }
        assertEquals(listOf(6, 6), onAttacker.map { it.amount })
    }

    @Test
    fun `every hero kit carries its meter-gated ultimate and own weapons`() {
        val rainbow = listOf(Party.RED, Party.ORANGE, Party.YELLOW, Party.GREEN, Party.BLUE, Party.VIOLET)
        for (hero in Party.ROSTER + Party.UNLOCKABLES) {
            val unit = Party.defaultLoadout(hero).toUnit()
            assertEquals(3, unit.abilities.size, "${hero.name} kit size")
            assertTrue(
                unit.abilities.any { it.id == hero.ultimateId },
                "${hero.name} missing ultimate"
            )
            assertEquals(hero.ultimateId, unit.ultimateId, "${hero.name} meter not wired")
        }
        // The six colors each own five weapons and five offhands; nobody
        // shares an arsenal. Defectors keep their smaller boss kits.
        for (hero in rainbow) {
            assertEquals(5, hero.weaponIds.size, "${hero.name} weapon count")
            assertEquals(5, hero.offhandIds.size, "${hero.name} offhand count")
            for (other in rainbow) {
                if (other.id == hero.id) continue
                assertTrue(
                    hero.weaponIds.none { it in other.weaponIds },
                    "${hero.name} shares weapons with ${other.name}"
                )
            }
        }
    }

    @Test
    fun `default gear is part of every hero's own lists`() {
        for (hero in Party.ROSTER + Party.UNLOCKABLES) {
            assertTrue(hero.defaultWeaponId in hero.weaponIds, "${hero.name} default weapon foreign")
            assertTrue(hero.defaultOffhandId in hero.offhandIds, "${hero.name} default offhand foreign")
            // Everything referenced actually exists in the registries.
            for (id in hero.weaponIds) assertTrue(id in Weapons.REGISTRY, "$id missing")
            for (id in hero.offhandIds) assertTrue(id in Offhands.REGISTRY, "$id missing")
        }
    }

    @Test
    fun `hero levels grow stats and xp thresholds climb to fifty`() {
        val p = com.elendheim.fewsbox.data.Progression
        assertEquals(1, p.levelFor(0))
        assertEquals(1, p.levelFor(29))
        assertEquals(2, p.levelFor(30))
        assertEquals(50, p.levelFor(p.xpForLevel(50)))
        assertEquals(50, p.levelFor(999999))
        assertEquals(null, p.xpToNext(999999))

        val fresh = Party.defaultLoadout(Party.RED).toUnit(1)
        val veteran = Party.defaultLoadout(Party.RED).toUnit(50)
        assertEquals(fresh.maxHp + 98, veteran.maxHp)
        assertEquals(fresh.baseAttack + 16, veteran.baseAttack)
    }

    @Test
    fun `boss unlocks point at real battles and real heroes`() {
        for ((battleIndex, heroId) in com.elendheim.fewsbox.data.Battles.unlocks) {
            assertTrue(battleIndex in 0 until com.elendheim.fewsbox.data.Battles.count)
            val loadout = Party.loadoutFor(heroId)
            val unit = loadout.toUnit()
            assertEquals(3, unit.abilities.size, "$heroId kit size")
            assertEquals(loadout.hero.ultimateId, unit.ultimateId)
        }
    }

    @Test
    fun `silver defects with a full kit of its own`() {
        val unit = Party.silverLoadout().toUnit()
        assertEquals(3, unit.abilities.size)
        assertTrue(unit.abilities.any { it.id == "ult_silver" })
        assertEquals("ult_silver", unit.ultimateId)
        assertEquals(3, Party.SILVER.weaponIds.size)
        assertEquals(5, Party.SILVER.offhandIds.size)
    }

    @Test
    fun `kill emits unit died and stops further hits on the corpse`() {
        val rec = Recorder()
        val attacker = unit("a", Team.PLAYER, attack = 10)
        val target = unit("t", Team.ENEMY, hp = 15)
        val state = battle(attacker, target)

        resolver(rec).resolve(state, attacker, plainHit(1.0f, hits = 3), listOf("t"))

        assertEquals(0, target.hp)
        assertEquals(1, rec.all<CombatEvent.UnitDied>().size)
        assertEquals(2, rec.all<CombatEvent.DamageDealt>().size) // third hit never lands
    }
}
