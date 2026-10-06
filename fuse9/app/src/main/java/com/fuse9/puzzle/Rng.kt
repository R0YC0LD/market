package com.fuse9.puzzle

/**
 * SplitMix64. Owned implementation so a seed produces the same puzzle on every
 * device and every Kotlin/JVM version — puzzles are identified by their seed.
 */
class Rng(seed: Long) {
    private var state = seed

    fun nextLong(): Long {
        state += -0x61c8864680b583ebL
        var z = state
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
        return z xor (z ushr 31)
    }

    /** Uniform int in [0, bound). */
    fun nextInt(bound: Int): Int {
        require(bound > 0)
        return ((nextLong() ushr 33) % bound).toInt()
    }

    fun shuffle(array: IntArray) {
        for (i in array.size - 1 downTo 1) {
            val j = nextInt(i + 1)
            val t = array[i]; array[i] = array[j]; array[j] = t
        }
    }

    fun shuffled(n: Int): IntArray = IntArray(n) { it }.also { shuffle(it) }

    companion object {
        /** Stable string hash (FNV-1a 64) for date seeds etc. */
        fun seedOf(text: String): Long {
            var h = -0x340d631b7bdddcdbL
            for (ch in text) {
                h = h xor ch.code.toLong()
                h *= 0x100000001b3L
            }
            return h
        }
    }
}
