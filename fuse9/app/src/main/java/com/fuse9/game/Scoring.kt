package com.fuse9.game

/** The single word a finished board earns. */
enum class Verdict(val word: String, val line: String) {
    CLEAN("Clean", "No strikes, no hints."),
    SHARP("Sharp", "Almost spotless."),
    SOLVED("Solved", "Every seal found."),
    UNSOLVED("Unsealed", "The board held this time."),
}

data class Summary(
    val verdict: Verdict,
    val millis: Long,
    val mistakes: Int,
    val hints: Int,
    val sealsDefused: Int,
    val ripples: Int,
    val bestStreak: Int,
    /** 0..100 — first-try accuracy of committed moves. */
    val accuracy: Int,
    /** 1..5 marks, combining accuracy, hints and pace for the tier. */
    val marks: Int,
)

object Scoring {
    /** Par time per difficulty in seconds; pace only nudges the marks, never the verdict. */
    private val PAR = mapOf(
        com.fuse9.puzzle.Difficulty.EASY to 360,
        com.fuse9.puzzle.Difficulty.MEDIUM to 600,
        com.fuse9.puzzle.Difficulty.HARD to 900,
        com.fuse9.puzzle.Difficulty.EXPERT to 1320,
        com.fuse9.puzzle.Difficulty.MASTER to 1800,
    )

    fun summarize(state: GameState): Summary {
        val s = state.stats
        val good = s.placements + s.sealsDefused
        val bad = s.wrongDigits + s.sealHits + s.wrongSeals
        val accuracy = if (good + bad == 0) 100 else (100 * good) / (good + bad)
        val verdict = when {
            state.status != GameStatus.WON -> Verdict.UNSOLVED
            state.mistakes == 0 && state.hintsUsed == 0 -> Verdict.CLEAN
            state.mistakes <= 2 && state.hintsUsed <= 1 -> Verdict.SHARP
            else -> Verdict.SOLVED
        }
        val par = (PAR[state.puzzle.difficulty] ?: 900) * 1000L
        var marks = when (verdict) {
            Verdict.CLEAN -> 4
            Verdict.SHARP -> 3
            Verdict.SOLVED -> 2
            Verdict.UNSOLVED -> 0
        }
        if (verdict != Verdict.UNSOLVED && state.elapsedMillis <= par) marks++
        if (verdict == Verdict.SOLVED && accuracy < 70) marks--
        return Summary(
            verdict, state.elapsedMillis, state.mistakes, state.hintsUsed, s.sealsDefused,
            s.ripples, s.bestStreak, accuracy, marks.coerceIn(0, 5),
        )
    }
}
