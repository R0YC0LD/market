package com.fuse9.game

import com.fuse9.puzzle.Difficulty
import com.fuse9.puzzle.PuzzleGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HintEngineTest {
    private val engine = MoveEngine()

    @Test
    fun followingHintsSolvesEveryTierWithoutStrikes() {
        for (d in Difficulty.entries) {
            var s = GameState.new(PuzzleGenerator.generate(55, d), GameMode.CLASSIC)
            var guard = 0
            while (s.status == GameStatus.PLAYING && guard++ < 200) {
                val hint = HintEngine.find(s)
                assertNotNull("$d stuck at move $guard", hint)
                hint!!
                // A hint must always be correct.
                assertEquals(s.truth.isSeal[hint.cell], hint.action == HintAction.SEAL)
                assertTrue(hint.reasons.isNotEmpty())
                assertTrue(hint.region.isNotEmpty())
                s = if (hint.action == HintAction.SEAL) engine.play(s, Action.SealAt(hint.cell)).state
                else engine.play(s, Action.Select(hint.cell), Action.Digit(hint.digit)).state
            }
            assertEquals("$d", GameStatus.WON, s.status)
            assertEquals(0, s.mistakes)
        }
    }

    @Test
    fun hintPrefersCellsNearSelection() {
        val s = Fixtures.game().copy(selected = 80)
        val hint = HintEngine.find(s)!!
        val far = HintEngine.find(s.copy(selected = 0))!!
        assertTrue(hint.cell >= 0 && far.cell >= 0)
    }
}
