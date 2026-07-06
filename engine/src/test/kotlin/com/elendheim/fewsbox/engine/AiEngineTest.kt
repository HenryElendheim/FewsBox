package com.elendheim.fewsbox.engine

import com.elendheim.fewsbox.data.Enemies
import com.elendheim.fewsbox.engine.ai.AiEngine
import com.elendheim.fewsbox.engine.model.Team
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AiEngineTest {

    @Test
    fun `weighted pick respects weights over many rolls`() {
        val weights = mapOf("a" to 8, "b" to 2)
        val rng = Random(99)
        val picks = (1..1000).map { AiEngine.weightedPick(weights, rng) }
        val aShare = picks.count { it == "a" } / 1000.0
        // 80% expected; wide tolerance, this is a sanity check not statistics
        assertTrue(aShare in 0.72..0.88, "a picked $aShare of the time")
    }

    @Test
    fun `heal nudge shifts odds when low but never guarantees`() {
        val shaman = Enemies.shaman("e1").apply { hp = 4 } // below 50%
        val player = unit("p", Team.PLAYER)
        val state = battle(shaman, player)

        val rng = Random(5)
        val picks = (1..1000).map { AiEngine.chooseAction(state, shaman, rng)!!.first.id }
        val healShare = picks.count { it == "small_heal" } / 1000.0

        // Base odds were 40% heal; the nudge (+8 on top of 6/4) pushes it to
        // 12/18 ~ 67%. It must be clearly favored yet still not exclusive.
        assertTrue(healShare > 0.55, "heal picked $healShare")
        assertTrue(picks.any { it == "basic_slash" }, "nudge must not hard-force")
    }

    @Test
    fun `ai only suggests moves the unit actually has`() {
        val grunt = Enemies.grunt("e1")
        val player = unit("p", Team.PLAYER)
        val state = battle(grunt, player)

        repeat(50) {
            val action = AiEngine.chooseAction(state, grunt, Random(it))
            assertNotNull(action)
            assertTrue(grunt.abilities.any { a -> a.id == action.first.id })
        }
    }

    @Test
    fun `same seed same choices`() {
        val grunt = Enemies.grunt("e1")
        val player = unit("p", Team.PLAYER)
        val state = battle(grunt, player)

        val first = (1..20).map { AiEngine.chooseAction(state, grunt, Random(7))!!.first.id }
        val second = (1..20).map { AiEngine.chooseAction(state, grunt, Random(7))!!.first.id }
        assertEquals(first, second)
    }
}
