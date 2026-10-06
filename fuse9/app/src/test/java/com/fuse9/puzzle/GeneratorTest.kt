package com.fuse9.puzzle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneratorTest {
    @Test
    fun deterministicForSeed() {
        val a = PuzzleGenerator.generate(1234, Difficulty.MEDIUM)
        val b = PuzzleGenerator.generate(1234, Difficulty.MEDIUM)
        assertEquals(a.solution, b.solution)
        assertEquals(a.seals, b.seals)
        assertEquals(a.givens, b.givens)
        assertEquals(PuzzleGenerator.VERSION, a.generatorVersion)
    }

    @Test
    fun everyTierIsHumanSolvableAndConsistent() {
        for (d in Difficulty.entries) for (seed in 1L..4L) {
            val p = PuzzleGenerator.generate(seed * 31, d)
            val solution = p.solution.toIntArray()
            assertTrue(Sudoku.isValidSolution(solution))
            assertTrue(Seals.isValid(solution, p.seals.toIntArray()))
            assertTrue("givens must be safe", p.givens.none { it in p.seals })
            val report = FusionSolver(p.truth, d.techniques, p.rules, verify = true).solve(p.givens)
            assertTrue("$d/$seed must solve with its own techniques", report.solved)
            assertTrue("$d/$seed exceeds tier", report.maxLevel <= d.maxLevel)
        }
    }

    @Test
    fun sealsAreNeverDecoration() {
        for (seed in 1L..6L) {
            val p = PuzzleGenerator.generate(seed, Difficulty.HARD)
            assertTrue(PuzzleGenerator.sudokuNeedsSeals(p.truth, p.givens.toSet(), Difficulty.HARD))
            val report = FusionSolver(p.truth, Difficulty.HARD.techniques, p.rules).solve(p.givens)
            assertTrue("fusion deductions are required", report.fusionSteps > 0)
        }
    }

    @Test
    fun everySolverMoveIsProvenNotGuessed() {
        // verify = true makes the solver throw on any deduction that contradicts the answer;
        // run it across many seeds so every technique is exercised.
        for (seed in 100L..130L) {
            val p = PuzzleGenerator.generate(seed, Difficulty.entries[(seed % 5).toInt()])
            assertTrue(FusionSolver(p.truth, Technique.entries.toSet(), p.rules, verify = true).solve(p.givens).solved)
        }
    }

    @Test
    fun harderTiersOpenBarer() {
        val easy = (1L..4L).map { PuzzleGenerator.generate(it, Difficulty.EASY).givens.size }.average()
        val master = (1L..4L).map { PuzzleGenerator.generate(it, Difficulty.MASTER).givens.size }.average()
        assertTrue(easy > master + 8)
    }

    @Test
    fun soundnessCheckCatchesBadDeductions() {
        val p = PuzzleGenerator.generate(77, Difficulty.EASY)
        val solver = FusionSolver(p.truth)
        val k = FusionSolver.initialKnowledge(p.truth, p.givens, p.rules)
        val safeCell = (0 until 81).first { it !in p.seals && it !in p.givens }
        val bad = Deduction(Technique.UNIT_LAST_CELL, listOf(Effect(EffectType.SEAL, safeCell)), emptyList())
        val thrown = runCatching { solver.apply(k, bad) }.exceptionOrNull()
        assertTrue(thrown is IllegalStateException)
    }
}

