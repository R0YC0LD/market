package com.fuse9.game

import com.fuse9.puzzle.Difficulty
import com.fuse9.puzzle.PuzzleGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TutorialGuideTest {
    @Test
    fun guideWalksFromLookingToSealsAndPointsAtRealMoves() {
        val engine = MoveEngine()
        var s = GameState.new(PuzzleGenerator.generate(PuzzleSource.TUTORIAL_SEED, Difficulty.EASY), GameMode.ZEN, tutorial = true)
        val guide = TutorialGuide()
        assertNotNull(guide.line(s))
        fun apply(vararg a: Action) { val o = engine.play(s, *a); s = o.state; o.events.forEach(guide::onEvent) }

        val first = (0 until 81).first { s.cells[it].status == CellStatus.GIVEN }
        apply(Action.Select(first))
        assertEquals(TutorialGuide.Step.FIRST_PLACE, guide.step)
        val line = guide.line(s)!!
        assertTrue("first move is pointed at", line.target >= 0)
        assertTrue(!s.truth.isSeal[line.target])
        apply(Action.Select(line.target), Action.Digit(s.truth.solution[line.target]))
        assertEquals(TutorialGuide.Step.MORE, guide.step)

        // Keep following hints until a seal is taught and defused.
        var guard = 0
        while (guide.step == TutorialGuide.Step.MORE && guard++ < 100) {
            val sealLine = guide.line(s)
            if (sealLine != null && sealLine.target >= 0 && s.truth.isSeal[sealLine.target]) {
                apply(Action.SealAt(sealLine.target))
                break
            }
            val h = HintEngine.find(s)!!
            if (h.action == HintAction.SEAL) apply(Action.SealAt(h.cell)) else apply(Action.Select(h.cell), Action.Digit(h.digit))
        }
        assertEquals(TutorialGuide.Step.DIGIT_LINK, guide.step)
        assertEquals(TutorialGuide.Kind.DIGIT_LINK, guide.line(s)!!.kind)
        assertEquals(0, s.mistakes)
    }
}
