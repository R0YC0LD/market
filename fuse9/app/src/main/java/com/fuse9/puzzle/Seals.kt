package com.fuse9.puzzle

/**
 * Seals are FUSE9's mines. A valid seal layout over a solution has exactly nine seals:
 * one per row, one per column, one per box, and every seal hides a different digit.
 * The last rule is what welds the two logics together: once the 4-seal is found,
 * every other cell holding a 4 is safe.
 */
object Seals {
    const val COUNT = 9

    fun generate(solution: IntArray, rng: Rng, attempts: Int = 64): IntArray? {
        repeat(attempts) {
            val cols = IntArray(9) { -1 }
            if (place(solution, 0, cols, 0, 0, 0, rng)) {
                val layout = IntArray(9) { r -> Grid.cell(r, cols[r]) }
                if (!isDegenerate(layout)) return layout
            }
        }
        return null
    }

    private fun place(
        solution: IntArray, row: Int, cols: IntArray,
        usedCols: Int, usedBoxes: Int, usedDigits: Int, rng: Rng,
    ): Boolean {
        if (row == 9) return true
        for (c in rng.shuffled(9)) {
            val cell = Grid.cell(row, c)
            val box = Grid.box(cell)
            val digitBit = Bits.bit(solution[cell])
            if (usedCols and (1 shl c) != 0) continue
            if (usedBoxes and (1 shl box) != 0) continue
            if (usedDigits and digitBit != 0) continue
            cols[row] = c
            if (place(solution, row + 1, cols, usedCols or (1 shl c), usedBoxes or (1 shl box), usedDigits or digitBit, rng)) return true
        }
        cols[row] = -1
        return false
    }

    fun isValid(solution: IntArray, seals: IntArray): Boolean {
        if (seals.size != COUNT || seals.toSet().size != COUNT) return false
        var rows = 0; var cols = 0; var boxes = 0; var digits = 0
        for (s in seals) {
            if (s !in 0 until Grid.CELLS) return false
            rows = rows or (1 shl Grid.row(s))
            cols = cols or (1 shl Grid.col(s))
            boxes = boxes or (1 shl Grid.box(s))
            digits = digits or Bits.bit(solution[s])
        }
        return rows == 0x1FF && cols == 0x1FF && boxes == 0x1FF && digits == Grid.ALL_DIGITS
    }

    /**
     * Rejects layouts that read as a pattern rather than a puzzle: seals marching along a
     * diagonal or a cyclic shift, or too few seals touching each other (dead open board)
     * or too many (one dense clump).
     */
    fun isDegenerate(seals: IntArray): Boolean {
        val shifts = seals.map { Math.floorMod(Grid.col(it) - Grid.row(it), 9) }.toSet().size
        val antiShifts = seals.map { Math.floorMod(Grid.col(it) + Grid.row(it), 9) }.toSet().size
        if (shifts <= 3 || antiShifts <= 3) return true
        val sealSet = seals.toSet()
        val touching = seals.count { s -> Grid.NEIGHBORS[s].any { it in sealSet } }
        return touching > 6
    }

    fun mask(seals: IntArray): BooleanArray = BooleanArray(Grid.CELLS).also { m -> seals.forEach { m[it] = true } }

    /** Number of seals among the 8 neighbours of each cell. */
    fun clues(isSeal: BooleanArray): IntArray = IntArray(Grid.CELLS) { c -> Grid.NEIGHBORS[c].count { isSeal[it] } }
}
