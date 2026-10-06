package com.fuse9.game

import org.junit.Assert.assertEquals
import org.junit.Test

class ScoringTest {
    @Test
    fun verdicts() {
        val won = Fixtures.game().copy(status = GameStatus.WON, elapsedMillis = 60_000)
        assertEquals(Verdict.CLEAN, Scoring.summarize(won).verdict)
        assertEquals(Verdict.SHARP, Scoring.summarize(won.copy(mistakes = 2)).verdict)
        assertEquals(Verdict.SOLVED, Scoring.summarize(won.copy(mistakes = 2, hintsUsed = 3)).verdict)
        assertEquals(Verdict.UNSOLVED, Scoring.summarize(won.copy(status = GameStatus.LOST)).verdict)
        assertEquals(5, Scoring.summarize(won).marks)
    }
}
