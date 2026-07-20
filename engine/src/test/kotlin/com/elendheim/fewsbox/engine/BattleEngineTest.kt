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
    fun `party ultimate meter fills, gates, and is a bonus action for acted heroes`() {
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

        // One attack landed (+5%) and one 10-damage hit taken (+3%) per
        // round: 80 tenths, then 160.
        eng.playerAction(state, "p", player.abilities[0].id, listOf("e"))
        eng.finishRound(state)
        assertEquals(80, state.partyUltCharge)

        eng.playerAction(state, "p", player.abilities[0].id, listOf("e"))
        eng.finishRound(state)
        assertEquals(160, state.partyUltCharge)

        // With a full meter: attack first (turn spent), then the ultimate
        // still fires - it rides the party meter, not the hero's turn.
        state.partyUltCharge = 1000
        assertTrue(eng.playerAction(state, "p", player.abilities[0].id, listOf("e")))
        assertFalse(eng.playerAction(state, "p", player.abilities[0].id, listOf("e")))
        assertTrue(eng.playerAction(state, "p", "big_one", listOf("e")))
        assertEquals(0, state.partyUltCharge)
        assertTrue(rec.all<CombatEvent.UltChargeChanged>().isNotEmpty())
    }

    @Test
    fun `a hit past half max health pays the big meter bonus`() {
        val rec = Recorder()
        val player = unit("p", Team.PLAYER, hp = 100, attack = 10, abilities = listOf(plainHit(1.0f)))
        val enemy = unit("e", Team.ENEMY, hp = 500, attack = 60,
            abilities = listOf(slash()), aiProfile = slashProfile())
        val state = battle(player, enemy)
        val eng = engine(rec)
        eng.startBattle(state)

        // The player sits the round out; the enemy lands 60 on a 100 HP
        // hero, which is worth 15% instead of 3%.
        state.spendAction("p")
        eng.finishRound(state)
        assertEquals(150, state.partyUltCharge)
    }

    @Test
    fun `extra actions let a hero act again in the same round`() {
        val rec = Recorder()
        val player = unit("p", Team.PLAYER, abilities = listOf(plainHit(0.5f)))
        val enemy = unit("e", Team.ENEMY, hp = 500)
        val state = battle(player, enemy)
        val eng = engine(rec)
        eng.startBattle(state)

        assertTrue(eng.playerAction(state, "p", player.abilities[0].id, listOf("e")))
        assertFalse(eng.playerAction(state, "p", player.abilities[0].id, listOf("e")))

        // Green's ultimate does this via Effect.GrantExtraActions.
        state.extraActions["p"] = 2
        assertTrue(eng.playerAction(state, "p", player.abilities[0].id, listOf("e")))
        assertTrue(eng.playerAction(state, "p", player.abilities[0].id, listOf("e")))
        assertFalse(eng.playerAction(state, "p", player.abilities[0].id, listOf("e")))

        // Extras vanish with the round.
        eng.finishRound(state)
        assertTrue(eng.playerAction(state, "p", player.abilities[0].id, listOf("e")))
        assertFalse(eng.playerAction(state, "p", player.abilities[0].id, listOf("e")))
    }

    @Test
    fun `two stun stacks skip two turns`() {
        val rec = Recorder()
        val doubleStun = Ability(
            id = "terror", iconId = "x", targeting = Targeting.SINGLE_ENEMY,
            effects = listOf(Effect.ApplyStatus("stun", stacks = 2, duration = 2))
        )
        val player = unit("p", Team.PLAYER, hp = 200, abilities = listOf(doubleStun))
        val enemy = unit("e", Team.ENEMY, hp = 100, attack = 10,
            abilities = listOf(slash()), aiProfile = slashProfile())
        val state = battle(player, enemy)
        val eng = engine(rec)
        eng.startBattle(state)

        eng.playerAction(state, "p", "terror", listOf("e"))
        eng.finishRound(state)
        assertEquals(200, player.hp)
        eng.finishRound(state)
        assertEquals(200, player.hp)
        assertEquals(2, rec.all<CombatEvent.TurnSkipped>().size)

        eng.finishRound(state)
        assertEquals(190, player.hp) // both stacks spent, enemy swings again
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
    fun `every campaign level and stage runs to completion without stalling`() {
        for (level in 0 until Battles.count) {
            for (stage in 0 until Battles.stageCountFor(level)) {
                val rec = Recorder()
                val state = Battles.createStage(level, stage, Party.defaultParty())
                val eng = BattleEngine(Statuses.REGISTRY, Random(level * 100L + stage), rec.emit)
                eng.startBattle(state)

                var rounds = 0
                while (state.phase != TurnPhase.BATTLE_OVER && rounds < 200) {
                    for (p in state.pendingPlayers) {
                        val target = state.enemies.firstOrNull() ?: break
                        val usable = p.abilities.firstOrNull {
                            p.cooldownLeft(it.id) == 0 && (it.id != p.ultimateId || state.partyUltReady)
                        }
                        if (usable != null) {
                            eng.playerAction(state, p.id, usable.id, listOf(target.id))
                        } else {
                            state.spendAction(p.id) // nothing usable: pass
                        }
                        if (state.phase == TurnPhase.BATTLE_OVER) break
                    }
                    if (state.phase != TurnPhase.BATTLE_OVER) eng.finishRound(state)
                    rounds++
                }
                assertEquals(TurnPhase.BATTLE_OVER, state.phase, "level $level stage $stage never ended")
            }
        }
    }

    @Test
    fun `stage counts follow the campaign plan and stay deterministic in endless`() {
        // Bosses: three set pieces are single fights, the finale runs three.
        assertEquals(1, Battles.stageCountFor(Battles.ASH_BOSS_INDEX))
        assertEquals(1, Battles.stageCountFor(Battles.SILVER_BOSS_INDEX))
        assertEquals(1, Battles.stageCountFor(Battles.TWIN_BOSS_INDEX))
        assertEquals(3, Battles.stageCountFor(Battles.FINAL_BOSS_INDEX))

        // Every 10th level is a gauntlet of 3 to 5 stages; the rest are single.
        for (level in 0 until Battles.count) {
            val stages = Battles.stageCountFor(level)
            if (Battles.isBossLevel(level)) continue
            if ((level + 1) % 10 == 0) {
                assertTrue(stages in 3..5, "level $level gauntlet has $stages stages")
            } else {
                assertEquals(1, stages, "level $level should be a single fight")
            }
        }

        // Endless: same level always rolls the same count, capped at 8.
        for (level in 100 until 300) {
            val stages = Battles.stageCountFor(level)
            assertEquals(stages, Battles.stageCountFor(level))
            assertTrue(stages == 1 || stages in 3..8, "endless $level rolled $stages stages")
        }
    }

    @Test
    fun `later stages inherit survivors and the ult meter`() {
        val party = Party.defaultParty()
        val first = Battles.createStage(9, 0, party)
        first.partyUltCharge = 300
        val hero = first.players.first()
        hero.hp = 17
        hero.damageDealtTotal = 42

        val second = Battles.createStage(
            9, 1, party,
            carriedPlayers = first.players,
            carriedUltCharge = first.partyUltCharge
        )
        assertEquals(300, second.partyUltCharge)
        val carried = second.players.first { it.id == hero.id }
        assertEquals(17, carried.hp)
        assertEquals(42, carried.damageDealtTotal)
        assertTrue(second.enemies.isNotEmpty())

        // Fresh enemies each stage, same enemies for the same stage.
        val rerolled = Battles.createStage(9, 1, party)
        assertEquals(
            second.enemies.map { it.iconId to it.maxHp },
            rerolled.enemies.map { it.iconId to it.maxHp }
        )
    }

    @Test
    fun `the finale is gray in every stage and gray never defects`() {
        for (stage in 0 until 3) {
            val state = Battles.createStage(Battles.FINAL_BOSS_INDEX, stage, Party.defaultParty())
            assertTrue(
                state.enemies.any { it.name == "Gray" },
                "final boss stage $stage is missing Gray"
            )
        }
        assertTrue(Battles.unlocks.none { it.key == Battles.FINAL_BOSS_INDEX })
    }
}
