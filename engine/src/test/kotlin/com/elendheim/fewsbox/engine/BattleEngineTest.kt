package com.elendheim.fewsbox.engine

import com.elendheim.fewsbox.data.Battles
import com.elendheim.fewsbox.data.Party
import com.elendheim.fewsbox.data.Statuses
import com.elendheim.fewsbox.engine.ability.Ability
import com.elendheim.fewsbox.engine.ability.Effect
import com.elendheim.fewsbox.engine.ability.Targeting
import com.elendheim.fewsbox.engine.ai.AiProfile
import com.elendheim.fewsbox.engine.ai.WeightedMove
import com.elendheim.fewsbox.engine.event.CombatEvent
import com.elendheim.fewsbox.engine.model.ChargeState
import com.elendheim.fewsbox.engine.model.Team
import com.elendheim.fewsbox.engine.model.TurnPhase
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BattleEngineTest {

    private fun stunAbility() = Ability(
        id = "stunner", iconId = "x", targeting = Targeting.SINGLE_ENEMY,
        effects = listOf(Effect.ApplyStatus("stun", stacks = 1, duration = 2))
    )

    private fun slash() = Ability(
        id = "slash", iconId = "x", targeting = Targeting.SINGLE_ENEMY,
        effects = listOf(Effect.DealDamage(multiplier = 1.0f, canCrit = false))
    )

    private fun slashProfile() = AiProfile(weightedMoves = listOf(WeightedMove("slash", 1)))

    // ------------------------------------------------------------------
    //  Turn structure
    // ------------------------------------------------------------------

    @Test
    fun `ultimate meter fills from dealing and taking damage then gates and resets`() {
        val rec = Recorder()
        val ult = Ability(
            id = "big_one", iconId = "x", targeting = Targeting.SINGLE_ENEMY,
            effects = listOf(Effect.DealDamage(multiplier = 2.0f, canCrit = false))
        )
        val player = unit(
            "p", Team.PLAYER, hp = 200, attack = 10,
            abilities = listOf(plainHit(1.0f), ult), ultimateId = "big_one"
        )
        val enemy = unit("e", Team.ENEMY, hp = 500, attack = 10,
            abilities = listOf(slash()), aiProfile = slashProfile())
        val state = battle(player, enemy)
        val eng = engine(rec)
        eng.startBattle(state)

        // Meter empty: the ultimate is refused.
        assertFalse(eng.playerAction(state, "p", "big_one", listOf("e")))

        // Deal 10 (+20%), take 10 (+30%) per round -> 50, then 100.
        eng.playerAction(state, "p", player.abilities[0].id, listOf("e"))
        eng.finishRound(state)
        assertEquals(50, player.ultCharge)

        eng.playerAction(state, "p", player.abilities[0].id, listOf("e"))
        eng.finishRound(state)
        assertEquals(100, player.ultCharge)

        // Full meter: fires, then resets to zero.
        assertTrue(eng.playerAction(state, "p", "big_one", listOf("e")))
        assertEquals(0, player.ultCharge)
        assertTrue(rec.all<CombatEvent.UltChargeChanged>().isNotEmpty())
    }

    @Test
    fun `action is refused on cooldown or when already acted`() {
        val rec = Recorder()
        val onCd = plainHit(2.0f).copy(cooldown = 2)
        val player = unit("p", Team.PLAYER, abilities = listOf(onCd))
        val enemy = unit("e", Team.ENEMY, hp = 500)
        val state = battle(player, enemy)
        val eng = engine(rec)
        eng.startBattle(state)

        assertTrue(eng.playerAction(state, "p", onCd.id, listOf("e")))
        // Already acted this round
        assertFalse(eng.playerAction(state, "p", onCd.id, listOf("e")))

        eng.finishRound(state)
        // Cooldown still ticking
        assertFalse(eng.playerAction(state, "p", onCd.id, listOf("e")))
        eng.finishRound(state)
        assertTrue(eng.playerAction(state, "p", onCd.id, listOf("e")))
    }

    @Test
    fun `stunned enemy skips exactly one turn`() {
        val rec = Recorder()
        val player = unit("p", Team.PLAYER, hp = 100, abilities = listOf(stunAbility()))
        val enemy = unit("e", Team.ENEMY, hp = 100, attack = 10,
            abilities = listOf(slash()), aiProfile = slashProfile())
        val state = battle(player, enemy)
        val eng = engine(rec)
        eng.startBattle(state)

        eng.playerAction(state, "p", "stunner", listOf("e"))
        eng.finishRound(state)
        assertEquals(100, player.hp) // stunned: no attack
        assertEquals(1, rec.all<CombatEvent.TurnSkipped>().size)

        eng.finishRound(state)
        assertEquals(90, player.hp) // stun was consumed, enemy acts again
    }

    @Test
    fun `burn ticks at turn start then decays and expires`() {
        val rec = Recorder()
        val burnTouch = Ability(
            id = "burn_touch", iconId = "x", targeting = Targeting.SINGLE_ENEMY,
            effects = listOf(Effect.ApplyStatus("burn", stacks = 2, duration = 5))
        )
        val player = unit("p", Team.PLAYER, hp = 500, abilities = listOf(burnTouch))
        val enemy = unit("e", Team.ENEMY, hp = 100, attack = 1,
            abilities = listOf(slash()), aiProfile = slashProfile())
        val state = battle(player, enemy)
        val eng = engine(rec)
        eng.startBattle(state)

        eng.playerAction(state, "p", "burn_touch", listOf("e"))
        eng.finishRound(state)
        // Tick 1: 2 stacks * 3 = 6 damage, then decay to 1 stack
        assertEquals(94, enemy.hp)
        assertEquals(1, enemy.statusStacks("burn"))

        eng.finishRound(state)
        // Tick 2: 1 stack * 3 = 3, decays to 0 and expires
        assertEquals(91, enemy.hp)
        assertEquals(0, enemy.statusStacks("burn"))
        assertTrue(rec.all<CombatEvent.StatusExpired>().any { it.statusId == "burn" })
    }

    @Test
    fun `poison ramps with stacks and expires by duration`() {
        val rec = Recorder()
        val venom = Ability(
            id = "venom", iconId = "x", targeting = Targeting.SINGLE_ENEMY,
            effects = listOf(Effect.ApplyStatus("poison", stacks = 3, duration = 2))
        )
        val player = unit("p", Team.PLAYER, hp = 500, abilities = listOf(venom))
        val enemy = unit("e", Team.ENEMY, hp = 100, attack = 1,
            abilities = listOf(slash()), aiProfile = slashProfile())
        val state = battle(player, enemy)
        val eng = engine(rec)
        eng.startBattle(state)

        eng.playerAction(state, "p", "venom", listOf("e"))
        eng.finishRound(state)
        // 3 stacks * 2 = 6, no stack decay
        assertEquals(94, enemy.hp)
        assertEquals(3, enemy.statusStacks("poison"))

        eng.finishRound(state)
        assertEquals(88, enemy.hp)
        // Duration (2 rounds) is up
        assertEquals(0, enemy.statusStacks("poison"))
    }

    // ------------------------------------------------------------------
    //  Charge telegraphs
    // ------------------------------------------------------------------

    @Test
    fun `elite charges then fires on schedule`() {
        val rec = Recorder()
        val crush = Ability(
            id = "crush", iconId = "x", targeting = Targeting.ALL_ENEMIES,
            effects = listOf(Effect.DealDamage(multiplier = 1.0f, canCrit = false))
        )
        val player = unit("p", Team.PLAYER, hp = 100, abilities = listOf(plainHit(0.1f)))
        val elite = unit("boss", Team.ENEMY, hp = 200, attack = 12,
            abilities = listOf(crush),
            charge = ChargeState(chargingAbilityId = "crush", turnsRequired = 2))
        val state = battle(player, elite)
        val eng = engine(rec)
        eng.startBattle(state)

        eng.finishRound(state) // charge 1/2
        assertEquals(100, player.hp)
        eng.finishRound(state) // charge 2/2 - ready
        assertEquals(100, player.hp)
        eng.finishRound(state) // fires
        assertEquals(88, player.hp)
        assertEquals(1, rec.all<CombatEvent.ChargeFired>().size)
        assertEquals(0, elite.charge!!.turnsElapsed) // wind-up restarts
    }

    @Test
    fun `stunning a charging elite resets the telegraph`() {
        val rec = Recorder()
        val crush = Ability(
            id = "crush", iconId = "x", targeting = Targeting.ALL_ENEMIES,
            effects = listOf(Effect.DealDamage(multiplier = 1.0f, canCrit = false))
        )
        val player = unit("p", Team.PLAYER, hp = 100, abilities = listOf(stunAbility()))
        val elite = unit("boss", Team.ENEMY, hp = 200, attack = 12,
            abilities = listOf(crush),
            charge = ChargeState(chargingAbilityId = "crush", turnsRequired = 2))
        val state = battle(player, elite)
        val eng = engine(rec)
        eng.startBattle(state)

        eng.finishRound(state) // 1/2
        eng.finishRound(state) // 2/2 - would fire next turn
        assertTrue(elite.charge!!.isReady)

        eng.playerAction(state, "p", "stunner", listOf("boss"))
        eng.finishRound(state) // stun consumed, charge reset instead of firing

        assertEquals(100, player.hp)
        assertEquals(0, elite.charge!!.turnsElapsed)
        assertTrue(rec.all<CombatEvent.ChargeFired>().isEmpty())
    }

    // ------------------------------------------------------------------
    //  Win / lose
    // ------------------------------------------------------------------

    @Test
    fun `battle ends the moment the last enemy dies`() {
        val rec = Recorder()
        val player = unit("p", Team.PLAYER, attack = 100, abilities = listOf(plainHit(1.0f)))
        val enemy = unit("e", Team.ENEMY, hp = 10)
        val state = battle(player, enemy)
        val eng = engine(rec)
        eng.startBattle(state)

        eng.playerAction(state, "p", player.abilities[0].id, listOf("e"))

        assertEquals(TurnPhase.BATTLE_OVER, state.phase)
        assertTrue(rec.events.last() is CombatEvent.BattleWon)
        // Nothing further is accepted
        assertFalse(eng.playerAction(state, "p", player.abilities[0].id, listOf("e")))
    }

    @Test
    fun `battle is lost when the party falls in the enemy phase`() {
        val rec = Recorder()
        val player = unit("p", Team.PLAYER, hp = 5, abilities = listOf(plainHit(0.1f)))
        val enemy = unit("e", Team.ENEMY, hp = 500, attack = 50,
            abilities = listOf(slash()), aiProfile = slashProfile())
        val state = battle(player, enemy)
        val eng = engine(rec)
        eng.startBattle(state)

        eng.finishRound(state)

        assertEquals(TurnPhase.BATTLE_OVER, state.phase)
        assertTrue(rec.events.last() is CombatEvent.BattleLost)
    }

    // ------------------------------------------------------------------
    //  Determinism & full battle smoke test
    // ------------------------------------------------------------------

    @Test
    fun `same seed plays out identically`() {
        fun run(): List<String> {
            val rec = Recorder()
            val state = Battles.create(4, Party.defaultParty())
            val eng = BattleEngine(Statuses.REGISTRY, Random(1234), rec.emit)
            eng.startBattle(state)
            var rounds = 0
            while (state.phase != TurnPhase.BATTLE_OVER && rounds < 60) {
                for (p in state.pendingPlayers) {
                    eng.playerAction(state, p.id, p.abilities[0].id, listOf(state.enemies.firstOrNull()?.id ?: ""))
                    if (state.phase == TurnPhase.BATTLE_OVER) break
                }
                if (state.phase != TurnPhase.BATTLE_OVER) eng.finishRound(state)
                rounds++
            }
            return rec.events.map { it.toString() }
        }

        assertEquals(run(), run())
    }

    @Test
    fun `starter battles all run to completion without stalling`() {
        for (battleIndex in 0 until Battles.count) {
            val rec = Recorder()
            val state = Battles.create(battleIndex, Party.defaultParty())
            val eng = BattleEngine(Statuses.REGISTRY, Random(battleIndex), rec.emit)
            eng.startBattle(state)

            var rounds = 0
            while (state.phase != TurnPhase.BATTLE_OVER && rounds < 200) {
                for (p in state.pendingPlayers) {
                    val target = state.enemies.firstOrNull() ?: break
                    val usable = p.abilities.firstOrNull {
                        p.cooldownLeft(it.id) == 0 && (it.id != p.ultimateId || p.ultReady)
                    }
                    if (usable != null) {
                        eng.playerAction(state, p.id, usable.id, listOf(target.id))
                    } else {
                        state.actedThisRound.add(p.id) // nothing usable: pass
                    }
                    if (state.phase == TurnPhase.BATTLE_OVER) break
                }
                if (state.phase != TurnPhase.BATTLE_OVER) eng.finishRound(state)
                rounds++
            }
            assertEquals(TurnPhase.BATTLE_OVER, state.phase, "battle $battleIndex never ended")
        }
    }
}
