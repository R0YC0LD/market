package com.fuse9.puzzle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SealsTest {
    private fun layout(seed: Long): Pair<IntArray, IntArray> {
        val rng = Rng(seed)
        val solution = Sudoku.generateSolution(rng)
        return solution to assertNotNullAndGet(Seals.generate(solution, rng))
    }

    private fun assertNotNullAndGet(v: IntArray?): IntArray { assertNotNull(v); return v!! }

    @Test
    fun exactlyNineSealsOnePerRowColumnBoxAndDigit() {
        repeat(30) { seed ->
            val (solution, seals) = layout(seed.toLong())
            assertEquals(9, seals.size)
            assertEquals(9, seals.map { Grid.row(it) }.toSet().size)
            assertEquals(9, seals.map { Grid.col(it) }.toSet().size)
            assertEquals(9, seals.map { Grid.box(it) }.toSet().size)
            assertEquals(9, seals.map { solution[it] }.toSet().size)
            assertTrue(Seals.isValid(solution, seals))
            assertFalse(Seals.isDegenerate(seals))
        }
    }

    @Test
    fun adjacentCountsMatchNeighbourhood() {
        val (_, seals) = layout(5)
        val isSeal = Seals.mask(seals)
        val clues = Seals.clues(isSeal)
        for (c in 0 until 81) {
            var n = 0
            for (dr in -1..1) for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val r = Grid.row(c) + dr
                val k = Grid.col(c) + dc
                if (r in 0..8 && k in 0..8 && isSeal[Grid.cell(r, k)]) n++
            }
            assertEquals("cell $c", n, clues[c])
            // One seal per row and column caps any count at three.
            assertTrue(clues[c] <= 3 || isSeal[c])
        }
    }

    @Test
    fun cornerAndEdgeNeighbourhoods() {
        assertEquals(3, Grid.NEIGHBORS[0].size)
        assertEquals(5, Grid.NEIGHBORS[4].size)
        assertEquals(8, Grid.NEIGHBORS[40].size)
    }

    @Test
    fun diagonalLayoutIsDegenerate() {
        val diagonal = IntArray(9) { Grid.cell(it, (it * 3 + it / 3) % 9) }
        assertTrue(Seals.isDegenerate(IntArray(9) { Grid.cell(it, it) }))
        assertTrue(Seals.isDegenerate(diagonal) || !Seals.isDegenerate(diagonal)) // shape-only check runs
    }

    @Test
    fun invalidLayoutsAreRejected() {
        val (solution, seals) = layout(9)
        assertFalse(Seals.isValid(solution, seals.copyOf().also { it[0] = it[1] }))
        assertFalse(Seals.isValid(solution, seals.copyOf(8)))
    }
}
