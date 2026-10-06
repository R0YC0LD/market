package com.fuse9.puzzle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The two-way coupling between digits and seals, tested in isolation. */
class FusionTechniqueTest {
    @Test
    fun knownSealDigitMakesOtherCellsWithThatDigitSafe() {
        val k = Knowledge()
        k.safety[0] = Knowledge.SEAL
        k.cand[0] = Bits.bit(4)
        k.cand[40] = Bits.bit(4) // Sudoku says r5c5 is a 4
        val found = Techniques.find(Technique.SEALED_DIGIT_SAFE, k)
        assertTrue(found.any { d -> d.effects.any { it.cell == 40 && it.type == EffectType.SAFE } })
    }

    @Test
    fun onlyPossibleHomeOfASealDigitIsASeal() {
        val k = Knowledge()
        // Every cell is known safe except r1c1, and only r1c1 can still hold a 7.
        for (c in 0 until 81) { k.safety[c] = Knowledge.SAFE; k.cand[c] = Grid.ALL_DIGITS and Bits.bit(7).inv() }
        k.safety[0] = Knowledge.UNKNOWN
        k.cand[0] = Bits.bit(7) or Bits.bit(2)
        val found = Techniques.find(Technique.SEAL_DIGIT_HOME, k)
        val effects = found.flatMap { it.effects }
        assertTrue(effects.contains(Effect(EffectType.SEAL, 0)))
        assertTrue(effects.contains(Effect(EffectType.SET_DIGIT, 0, 7)))
    }

    @Test
    fun sealsCannotShareDigits() {
        val k = Knowledge()
        k.safety[0] = Knowledge.SEAL; k.cand[0] = Bits.bit(3)
        k.safety[40] = Knowledge.SEAL; k.cand[40] = Bits.bit(3) or Bits.bit(8)
        val effects = Techniques.find(Technique.SEAL_DIGITS_DISTINCT, k).flatMap { it.effects }
        assertEquals(listOf(Effect(EffectType.ELIMINATE, 40, Bits.bit(3))), effects)
    }

    @Test
    fun unitWithKnownSealClearsTheRest() {
        val k = Knowledge()
        k.safety[Grid.cell(2, 4)] = Knowledge.SEAL
        val cleared = Techniques.find(Technique.UNIT_SEALED, k).flatMap { it.effects }.map { it.cell }.toSet()
        assertTrue(Grid.UNITS[2].filter { it != Grid.cell(2, 4) }.all { it in cleared })
    }

    @Test
    fun satisfiedCountClearsNeighbours() {
        val k = Knowledge()
        val c = Grid.cell(4, 4)
        k.safety[c] = Knowledge.SAFE; k.clue[c] = 1
        k.safety[Grid.cell(3, 3)] = Knowledge.SEAL
        val cleared = Techniques.find(Technique.COUNT_SATISFIED, k).flatMap { it.effects }.map { it.cell }.toSet()
        assertEquals(Grid.NEIGHBORS[c].filter { it != Grid.cell(3, 3) }.toSet(), cleared)
    }
}
