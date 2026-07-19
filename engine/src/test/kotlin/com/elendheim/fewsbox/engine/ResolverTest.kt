package com.elendheim.fewsbox.engine

import com.elendheim.fewsbox.data.Party
import com.elendheim.fewsbox.data.Statuses
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
        r.addStatus(target, "vulnerable", 1, 2)   // +25% in
        r.resolve(state, attacker, plainHit(1.0f), listOf("t"))

        // 10 * 0.7 * 1.25 = 8.75 -> 9
        assertEquals(9, rec.all<CombatEvent.DamageDealt>().single().amount)
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
        for (loadout in Party.rosterDefaults()) {
            val unit = loadout.toUnit()
            assertEquals(3, unit.abilities.size, "${loadout.hero.name} kit size")
            assertTrue(
                unit.abilities.any { it.id == loadout.hero.ultimateId },
                "${loadout.hero.name} missing ultimate"
            )
            assertEquals(loadout.hero.ultimateId, unit.ultimateId, "${loadout.hero.name} meter not wired")
            assertEquals(3, loadout.hero.weaponIds.size, "${loadout.hero.name} weapon count")
            // Signature weapons: nobody shares an arsenal.
            for (other in Party.ROSTER) {
                if (other.id == loadout.hero.id) continue
                assertTrue(
                    loadout.hero.weaponIds.none { it in other.weaponIds },
                    "${loadout.hero.name} shares weapons with ${other.name}"
                )
            }
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
