package com.fuse9.puzzle

/** Full-grid generation and brute-force solution counting. */
object Sudoku {

    /** Builds a uniformly-shuffled complete, valid Sudoku solution. Values 1..9. */
    fun generateSolution(rng: Rng): IntArray {
        val grid = IntArray(Grid.CELLS)
        val order = IntArray(9) { it + 1 }
        check(fill(grid, 0, rng, order)) { "solution fill failed" }
        return grid
    }

    private fun fill(grid: IntArray, pos: Int, rng: Rng, scratch: IntArray): Boolean {
        if (pos == Grid.CELLS) return true
        val digits = IntArray(9) { it + 1 }.also { rng.shuffle(it) }
        for (d in digits) {
            if (canPlace(grid, pos, d)) {
                grid[pos] = d
                if (fill(grid, pos + 1, rng, scratch)) return true
                grid[pos] = 0
            }
        }
        return false
    }

    fun canPlace(grid: IntArray, cell: Int, digit: Int): Boolean {
        for (p in Grid.PEERS[cell]) if (grid[p] == digit) return false
        return true
    }

    fun isValidSolution(grid: IntArray): Boolean {
        if (grid.size != Grid.CELLS) return false
        for (unit in Grid.UNITS) {
            var seen = 0
            for (c in unit) {
                val d = grid[c]
                if (d !in 1..9) return false
                seen = seen or Bits.bit(d)
            }
            if (seen != Grid.ALL_DIGITS) return false
        }
        return true
    }

    /** True if no placed digit (non-zero) conflicts with a peer. */
    fun isConsistent(grid: IntArray): Boolean {
        for (c in 0 until Grid.CELLS) {
            val d = grid[c]
            if (d == 0) continue
            for (p in Grid.PEERS[c]) if (grid[p] == d) return false
        }
        return true
    }

    /** Candidate mask for an empty cell given the placed digits. */
    fun candidates(grid: IntArray, cell: Int): Int {
        if (grid[cell] != 0) return Bits.bit(grid[cell])
        var used = 0
        for (p in Grid.PEERS[cell]) if (grid[p] != 0) used = used or Bits.bit(grid[p])
        return Grid.ALL_DIGITS and used.inv()
    }

    /** Counts solutions of a partial grid (0 = empty), stopping at [limit]. */
    fun countSolutions(partial: IntArray, limit: Int = 2): Int {
        val grid = partial.copyOf()
        if (!isConsistent(grid)) return 0
        return count(grid, limit)
    }

    private fun count(grid: IntArray, limit: Int): Int {
        // Most-constrained empty cell first.
        var best = -1
        var bestMask = 0
        var bestCount = 10
        for (c in 0 until Grid.CELLS) {
            if (grid[c] != 0) continue
            val m = candidates(grid, c)
            val n = Bits.count(m)
            if (n == 0) return 0
            if (n < bestCount) {
                best = c; bestMask = m; bestCount = n
                if (n == 1) break
            }
        }
        if (best < 0) return 1
        var total = 0
        var m = bestMask
        while (m != 0) {
            val low = m and -m
            grid[best] = Integer.numberOfTrailingZeros(low) + 1
            total += count(grid, limit - total)
            if (total >= limit) break
            m = m xor low
        }
        grid[best] = 0
        return total
    }
}
