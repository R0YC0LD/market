package com.fuse9.puzzle

/**
 * Static geometry of the 9x9 board. Cells are indexed 0..80 row-major.
 * Units are indexed 0..26: rows 0..8, columns 9..17, boxes 18..26.
 */
object Grid {
    const val SIZE = 9
    const val CELLS = 81
    const val UNIT_COUNT = 27
    const val ALL_DIGITS = 0x1FF

    fun row(cell: Int) = cell / SIZE
    fun col(cell: Int) = cell % SIZE
    fun box(cell: Int) = (row(cell) / 3) * 3 + col(cell) / 3
    fun cell(row: Int, col: Int) = row * SIZE + col

    fun rowUnit(cell: Int) = row(cell)
    fun colUnit(cell: Int) = SIZE + col(cell)
    fun boxUnit(cell: Int) = 2 * SIZE + box(cell)

    val UNITS: Array<IntArray> = Array(UNIT_COUNT) { u ->
        when {
            u < 9 -> IntArray(9) { cell(u, it) }
            u < 18 -> IntArray(9) { cell(it, u - 9) }
            else -> {
                val b = u - 18
                val r0 = (b / 3) * 3
                val c0 = (b % 3) * 3
                IntArray(9) { cell(r0 + it / 3, c0 + it % 3) }
            }
        }
    }

    /** The three units (row, column, box) each cell belongs to. */
    val CELL_UNITS: Array<IntArray> = Array(CELLS) { intArrayOf(rowUnit(it), colUnit(it), boxUnit(it)) }

    /** Sudoku peers: the 20 other cells sharing a row, column or box. */
    val PEERS: Array<IntArray> = Array(CELLS) { c ->
        (0 until CELLS).filter { it != c && (row(it) == row(c) || col(it) == col(c) || box(it) == box(c)) }
            .toIntArray()
    }

    /** Minesweeper neighbourhood: the up-to-8 surrounding cells. */
    val NEIGHBORS: Array<IntArray> = Array(CELLS) { c ->
        val r = row(c)
        val k = col(c)
        buildList {
            for (dr in -1..1) for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val rr = r + dr
                val cc = k + dc
                if (rr in 0 until SIZE && cc in 0 until SIZE) add(cell(rr, cc))
            }
        }.toIntArray()
    }

    fun unitName(unit: Int): String = when {
        unit < 9 -> "row ${unit + 1}"
        unit < 18 -> "column ${unit - 8}"
        else -> "box ${unit - 17}"
    }

    fun cellName(cell: Int): String = "r${row(cell) + 1}c${col(cell) + 1}"
}

/** Digit bitmask helpers. Digit d (1..9) is bit (d-1). */
object Bits {
    fun bit(digit: Int) = 1 shl (digit - 1)
    fun count(mask: Int) = Integer.bitCount(mask)
    fun single(mask: Int) = mask != 0 && (mask and (mask - 1)) == 0
    fun digitOf(mask: Int) = Integer.numberOfTrailingZeros(mask) + 1
    fun has(mask: Int, digit: Int) = (mask and bit(digit)) != 0
    inline fun forEach(mask: Int, action: (Int) -> Unit) {
        var m = mask
        while (m != 0) {
            val low = m and -m
            action(Integer.numberOfTrailingZeros(low) + 1)
            m = m xor low
        }
    }
    fun digits(mask: Int): List<Int> = buildList { Bits.forEach(mask) { add(it) } }
}

/** 81-bit cell set packed into two longs. Used by the mine constraint reasoning. */
data class CellSet(val lo: Long, val hi: Long) {
    val size: Int get() = java.lang.Long.bitCount(lo) + java.lang.Long.bitCount(hi)
    val isEmpty: Boolean get() = lo == 0L && hi == 0L
    infix fun and(o: CellSet) = CellSet(lo and o.lo, hi and o.hi)
    infix fun minus(o: CellSet) = CellSet(lo and o.lo.inv(), hi and o.hi.inv())
    fun isSubsetOf(o: CellSet) = (lo and o.lo.inv()) == 0L && (hi and o.hi.inv()) == 0L
    inline fun forEach(action: (Int) -> Unit) {
        var m = lo
        while (m != 0L) {
            val low = m and -m
            action(java.lang.Long.numberOfTrailingZeros(low))
            m = m xor low
        }
        m = hi
        while (m != 0L) {
            val low = m and -m
            action(64 + java.lang.Long.numberOfTrailingZeros(low))
            m = m xor low
        }
    }
    fun toList(): List<Int> = buildList { this@CellSet.forEach { add(it) } }

    companion object {
        val EMPTY = CellSet(0L, 0L)
        fun of(cells: Iterable<Int>): CellSet {
            var lo = 0L
            var hi = 0L
            for (c in cells) if (c < 64) lo = lo or (1L shl c) else hi = hi or (1L shl (c - 64))
            return CellSet(lo, hi)
        }
    }
}
