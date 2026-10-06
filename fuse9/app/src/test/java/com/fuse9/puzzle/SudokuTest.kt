package com.fuse9.puzzle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SudokuTest {
    @Test
    fun generatedSolutionsAreValid() {
        repeat(20) { seed ->
            assertTrue(Sudoku.isValidSolution(Sudoku.generateSolution(Rng(seed.toLong()))))
        }
    }

    @Test
    fun sameSeedSameGrid() {
        assertTrue(Sudoku.generateSolution(Rng(42)).contentEquals(Sudoku.generateSolution(Rng(42))))
        assertFalse(Sudoku.generateSolution(Rng(42)).contentEquals(Sudoku.generateSolution(Rng(43))))
    }

    @Test
    fun invalidBoardsAreRejected() {
        val grid = Sudoku.generateSolution(Rng(7))
        val broken = grid.copyOf().also { val t = it[0]; it[0] = it[1]; it[1] = t }
        assertFalse(Sudoku.isValidSolution(broken))
        assertFalse(Sudoku.isConsistent(broken))
        assertFalse(Sudoku.isValidSolution(grid.copyOf().also { it[40] = 0 }))
    }

    @Test
    fun solutionCounting() {
        val grid = Sudoku.generateSolution(Rng(3))
        assertEquals(1, Sudoku.countSolutions(grid))
        // Removing one cell keeps it unique; an empty grid has many.
        assertEquals(1, Sudoku.countSolutions(grid.copyOf().also { it[10] = 0 }))
        assertEquals(2, Sudoku.countSolutions(IntArray(81), limit = 2))
    }

    @Test
    fun candidatesExcludePeers() {
        val grid = Sudoku.generateSolution(Rng(11))
        val partial = grid.copyOf().also { it[0] = 0 }
        assertEquals(Bits.bit(grid[0]), Sudoku.candidates(partial, 0))
        val open = IntArray(81).also { it[1] = 5; it[9] = 3; it[20] = 7 }
        val mask = Sudoku.candidates(open, 0)
        assertFalse(Bits.has(mask, 5)); assertFalse(Bits.has(mask, 3)); assertFalse(Bits.has(mask, 7))
        assertEquals(6, Bits.count(mask))
    }
}
