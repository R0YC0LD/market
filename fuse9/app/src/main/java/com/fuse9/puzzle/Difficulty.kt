package com.fuse9.puzzle

/**
 * What the player sees is a (localised) name; underneath, each tier is a contract with the generator:
 * which techniques may be required, how sparse the opening board can get, and what the
 * graded score must land in.
 */
enum class Difficulty(
    val maxLevel: Int,
    /** Generator stops removing givens below this many. */
    val minGivens: Int,
    /** Accepted score range (inclusive). */
    val scoreRange: IntRange,
) {
    EASY(1, 34, 0..140),
    MEDIUM(2, 27, 95..200),
    HARD(3, 23, 150..280),
    EXPERT(4, 19, 220..400),
    MASTER(4, 0, 250..10_000);

    val techniques: Set<Technique> get() = Technique.upTo(maxLevel)
}

/** A graded summary of how a puzzle solves. */
data class Rating(
    val score: Int,
    val maxLevel: Int,
    val fusionSteps: Int,
    val waves: Int,
    val tightWaves: Int,
    val steps: Int,
)

object DifficultyAnalyzer {
    /**
     * Score blends technique weight, how often the player is down to one or two available
     * moves (forced-move pressure), the depth of the solve, and how bare the opening is.
     */
    fun rate(report: SolveReport, givens: Int): Rating {
        val techniqueScore = report.usage.entries.sumOf { (t, n) -> t.weight * minOf(n, 12) + if (n > 0) t.weight * 2 else 0 }
        val pressure = report.tightWaves * 3
        val depth = report.waves.size
        val bareness = maxOf(0, 40 - givens) * 2
        val score = techniqueScore + pressure + depth + bareness
        return Rating(score, report.maxLevel, report.fusionSteps, report.waves.size, report.tightWaves, report.steps)
    }

    fun fits(difficulty: Difficulty, rating: Rating): Boolean {
        if (rating.score !in difficulty.scoreRange) return false
        // Each tier above Easy must actually need its top tier of technique.
        return when (difficulty) {
            Difficulty.EASY -> true
            Difficulty.MEDIUM -> rating.waves >= 2 || rating.maxLevel >= 2
            Difficulty.HARD -> rating.maxLevel >= 3 && rating.waves >= 3
            Difficulty.EXPERT -> rating.maxLevel >= 3 && rating.waves >= 4
            Difficulty.MASTER -> rating.maxLevel >= 4 && rating.waves >= 5
        }
    }
}
