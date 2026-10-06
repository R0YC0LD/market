package com.fuse9.puzzle

/** What a logical player knows about the board, independent of the hidden truth. */
class Knowledge(
    /** Digit candidates per cell (bitmask). A single bit means the digit is known. */
    val cand: IntArray = IntArray(Grid.CELLS) { Grid.ALL_DIGITS },
    /** 0 = unknown, 1 = safe, 2 = seal. */
    val safety: ByteArray = ByteArray(Grid.CELLS),
    /** Visible seal count, or -1 when the count is still hidden. */
    val clue: IntArray = IntArray(Grid.CELLS) { -1 },
    /** Cell is finished on the board (digit placed / seal defused). */
    val resolved: BooleanArray = BooleanArray(Grid.CELLS),
) {
    fun copy() = Knowledge(cand.copyOf(), safety.copyOf(), clue.copyOf(), resolved.copyOf())

    fun isSafe(c: Int) = safety[c] == SAFE
    fun isSeal(c: Int) = safety[c] == SEAL
    fun isUnknown(c: Int) = safety[c] == UNKNOWN
    fun digitKnown(c: Int) = Bits.single(cand[c])
    fun canPlace(c: Int) = !resolved[c] && isSafe(c) && digitKnown(c)
    fun canSeal(c: Int) = !resolved[c] && isSeal(c)
    fun allResolved() = resolved.all { it }

    /** Mask of digits already known to be hidden under a known seal. */
    fun sealedDigits(): Int {
        var mask = 0
        for (c in 0 until Grid.CELLS) if (isSeal(c) && digitKnown(c)) mask = mask or cand[c]
        return mask
    }

    companion object {
        const val UNKNOWN: Byte = 0
        const val SAFE: Byte = 1
        const val SEAL: Byte = 2
    }
}

/** The hidden answer to a puzzle. */
class Truth(val solution: IntArray, val seals: IntArray) {
    val isSeal: BooleanArray = Seals.mask(seals)
    val clues: IntArray = Seals.clues(isSeal)
}
