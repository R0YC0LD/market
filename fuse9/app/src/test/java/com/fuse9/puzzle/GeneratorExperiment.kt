package com.fuse9.puzzle

import org.junit.Assume
import org.junit.Test

/** Tuning harness: ./gradlew :app:testDebugUnitTest --tests '*GeneratorExperiment*' -Dfuse9.experiment=true */
class GeneratorExperiment {
    @Test
    fun survey() {
        Assume.assumeTrue(System.getenv("FUSE9_EXPERIMENT") != null)
        for (cascade in listOf(false)) {
            val rules = Rules(cascade)
            println("=== cascade=$cascade")
            for (d in Difficulty.entries) {
                val stats = (1..16L).map { seed ->
                    val t0 = System.nanoTime()
                    val p = PuzzleGenerator.generate(seed * 7919, d, rules)
                    val ms = (System.nanoTime() - t0) / 1_000_000
                    val r = FusionSolver(p.truth, d.techniques, rules).solve(p.givens)
                    val k = FusionSolver.initialKnowledge(p.truth, p.givens, rules)
                    val cleared = (0 until 81).count { k.clue[it] >= 0 && !k.resolved[it] }
                    val integrated = PuzzleGenerator.sudokuNeedsSeals(p.truth, p.givens.toSet(), d)
                    "fit=${DifficultyAnalyzer.fits(d, DifficultyAnalyzer.rate(r, p.givens.size))} givens=${p.givens.size} score=${p.score} lvl=${p.maxLevel} fus=${r.fusionSteps} waves=${r.waves.size} tight=${r.tightWaves} first=${r.waves.firstOrNull()} cleared=$cleared integ=$integrated ms=$ms"
                }
                println(d)
                stats.forEach { println("  $it") }
            }
        }
    }
}
