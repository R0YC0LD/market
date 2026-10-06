package com.fuse9.puzzle

import com.fuse9.puzzle.EffectType.ELIMINATE
import com.fuse9.puzzle.EffectType.SAFE
import com.fuse9.puzzle.EffectType.SEAL
import com.fuse9.puzzle.EffectType.SET_DIGIT

/**
 * Finders for every [Technique]. Each finder only reads [Knowledge] and returns the
 * deductions it can make right now; nothing here looks at the hidden answer.
 */
internal object Techniques {

    fun find(t: Technique, k: Knowledge): List<Deduction> = when (t) {
        Technique.NAKED_SINGLE -> emptyList() // produced by candidate elimination
        Technique.HIDDEN_SINGLE -> hiddenSingles(k)
        Technique.UNIT_SEALED -> unitSealed(k)
        Technique.UNIT_LAST_CELL -> unitLastCell(k)
        Technique.COUNT_SATISFIED -> countSatisfied(k)
        Technique.COUNT_FULL -> countFull(k)
        Technique.SEALED_DIGIT_SAFE -> sealedDigitSafe(k)
        Technique.SEAL_DIGITS_DISTINCT -> sealDigitsDistinct(k)
        Technique.SEAL_DIGIT_HOME -> sealDigitHome(k)
        Technique.SEAL_DIGIT_POINTING -> sealDigitPointing(k)
        Technique.LOCKED_CANDIDATES -> lockedCandidates(k)
        Technique.REGION_SUBSET -> regionSubset(k)
        Technique.NAKED_SUBSET -> nakedSubsets(k)
        Technique.HIDDEN_PAIR -> hiddenPairs(k)
        Technique.REGION_OVERLAP -> regionOverlap(k)
    }

    /** Removes known digits from peers until stable. Returns cells that became known. */
    fun eliminate(k: Knowledge): List<Int> {
        val newlyKnown = ArrayList<Int>()
        var changed = true
        while (changed) {
            changed = false
            for (c in 0 until Grid.CELLS) {
                val m = k.cand[c]
                if (!Bits.single(m)) continue
                for (p in Grid.PEERS[c]) {
                    val before = k.cand[p]
                    if (before and m != 0) {
                        val after = before and m.inv()
                        k.cand[p] = after
                        if (Bits.single(after)) newlyKnown.add(p)
                        changed = true
                    }
                }
            }
        }
        return newlyKnown
    }

    // ---- Sudoku -------------------------------------------------------------------------

    private fun hiddenSingles(k: Knowledge): List<Deduction> {
        val out = ArrayList<Deduction>()
        val claimed = HashSet<Int>()
        for (u in 0 until Grid.UNIT_COUNT) {
            val unit = Grid.UNITS[u]
            for (d in 1..9) {
                val b = Bits.bit(d)
                var where = -1
                var n = 0
                for (c in unit) if (k.cand[c] and b != 0) { n++; where = c }
                if (n == 1 && !Bits.single(k.cand[where]) && claimed.add(where)) {
                    out += Deduction(Technique.HIDDEN_SINGLE, listOf(Effect(SET_DIGIT, where, d)), unit.toList(), unit = u, digit = d)
                }
            }
        }
        return out
    }

    private fun lockedCandidates(k: Knowledge): List<Deduction> {
        val out = ArrayList<Deduction>()
        for (u in 0 until Grid.UNIT_COUNT) {
            val unit = Grid.UNITS[u]
            for (d in 1..9) {
                val b = Bits.bit(d)
                val cells = unit.filter { k.cand[it] and b != 0 }
                if (cells.size < 2 || cells.any { Bits.single(k.cand[it]) }) continue
                // Every candidate position shares another unit -> eliminate there.
                val shared = Grid.CELL_UNITS[cells[0]].filter { other -> other != u && cells.all { other in Grid.CELL_UNITS[it] } }
                for (other in shared) {
                    val effects = Grid.UNITS[other].filter { it !in cells && k.cand[it] and b != 0 }
                        .map { Effect(ELIMINATE, it, b) }
                    if (effects.isNotEmpty()) {
                        out += Deduction(Technique.LOCKED_CANDIDATES, effects, cells, unit = u, digit = d, source = other)
                    }
                }
            }
        }
        return out
    }

    private fun nakedSubsets(k: Knowledge): List<Deduction> {
        val out = ArrayList<Deduction>()
        for (u in 0 until Grid.UNIT_COUNT) {
            val open = Grid.UNITS[u].filter { Bits.count(k.cand[it]) in 2..3 }
            for (i in open.indices) for (j in i + 1 until open.size) {
                val pairMask = k.cand[open[i]] or k.cand[open[j]]
                if (Bits.count(pairMask) == 2) {
                    addSubsetElim(k, u, listOf(open[i], open[j]), pairMask, out)
                }
                for (l in j + 1 until open.size) {
                    val tri = pairMask or k.cand[open[l]]
                    if (Bits.count(tri) == 3) addSubsetElim(k, u, listOf(open[i], open[j], open[l]), tri, out)
                }
            }
        }
        return out
    }

    private fun addSubsetElim(k: Knowledge, u: Int, cells: List<Int>, mask: Int, out: MutableList<Deduction>) {
        val effects = Grid.UNITS[u].filter { it !in cells && k.cand[it] and mask != 0 }
            .map { Effect(ELIMINATE, it, k.cand[it] and mask) }
        if (effects.isNotEmpty()) out += Deduction(Technique.NAKED_SUBSET, effects, cells, unit = u)
    }

    private fun hiddenPairs(k: Knowledge): List<Deduction> {
        val out = ArrayList<Deduction>()
        for (u in 0 until Grid.UNIT_COUNT) {
            val unit = Grid.UNITS[u]
            val where = IntArray(10) { -1 }
            val pos = Array(10) { IntArray(0) }
            for (d in 1..9) {
                val cells = unit.filter { k.cand[it] and Bits.bit(d) != 0 }
                if (cells.size == 2 && cells.none { Bits.single(k.cand[it]) }) {
                    pos[d] = cells.toIntArray(); where[d] = 1
                }
            }
            for (a in 1..9) for (b in a + 1..9) {
                if (where[a] != 1 || where[b] != 1 || !pos[a].contentEquals(pos[b])) continue
                val keep = Bits.bit(a) or Bits.bit(b)
                val effects = pos[a].filter { k.cand[it] and keep.inv() != 0 }
                    .map { Effect(ELIMINATE, it, k.cand[it] and keep.inv()) }
                if (effects.isNotEmpty()) out += Deduction(Technique.HIDDEN_PAIR, effects, pos[a].toList(), unit = u)
            }
        }
        return out
    }

    // ---- Seals ----------------------------------------------------------------------------

    private fun unitSealed(k: Knowledge): List<Deduction> {
        val out = ArrayList<Deduction>()
        for (u in 0 until Grid.UNIT_COUNT) {
            val unit = Grid.UNITS[u]
            val seal = unit.firstOrNull { k.isSeal(it) } ?: continue
            val effects = unit.filter { k.isUnknown(it) }.map { Effect(SAFE, it) }
            if (effects.isNotEmpty()) out += Deduction(Technique.UNIT_SEALED, effects, unit.toList(), unit = u, source = seal)
        }
        return out
    }

    private fun unitLastCell(k: Knowledge): List<Deduction> {
        val out = ArrayList<Deduction>()
        for (u in 0 until Grid.UNIT_COUNT) {
            val unit = Grid.UNITS[u]
            if (unit.any { k.isSeal(it) }) continue
            val open = unit.filter { k.isUnknown(it) }
            if (open.size == 1) out += Deduction(Technique.UNIT_LAST_CELL, listOf(Effect(SEAL, open[0])), unit.toList(), unit = u)
        }
        return out
    }

    private fun countSatisfied(k: Knowledge): List<Deduction> {
        val out = ArrayList<Deduction>()
        for (c in 0 until Grid.CELLS) {
            val clue = k.clue[c]
            if (clue < 0) continue
            val nb = Grid.NEIGHBORS[c]
            val unknown = nb.filter { k.isUnknown(it) }
            if (unknown.isEmpty()) continue
            if (nb.count { k.isSeal(it) } == clue) {
                out += Deduction(Technique.COUNT_SATISFIED, unknown.map { Effect(SAFE, it) }, nb.toList(), source = c)
            }
        }
        return out
    }

    private fun countFull(k: Knowledge): List<Deduction> {
        val out = ArrayList<Deduction>()
        for (c in 0 until Grid.CELLS) {
            val clue = k.clue[c]
            if (clue <= 0) continue
            val nb = Grid.NEIGHBORS[c]
            val unknown = nb.filter { k.isUnknown(it) }
            if (unknown.isEmpty()) continue
            if (nb.count { k.isSeal(it) } + unknown.size == clue) {
                out += Deduction(Technique.COUNT_FULL, unknown.map { Effect(SEAL, it) }, nb.toList(), source = c)
            }
        }
        return out
    }

    private data class Region(val cells: CellSet, val seals: Int, val source: Int, val unit: Int)

    /** Every "exactly n seals among these unknown cells" constraint currently on the board. */
    private fun regions(k: Knowledge): List<Region> {
        val out = ArrayList<Region>()
        for (u in 0 until Grid.UNIT_COUNT) {
            val unit = Grid.UNITS[u]
            if (unit.any { k.isSeal(it) }) continue
            val unknown = unit.filter { k.isUnknown(it) }
            if (unknown.size >= 2) out += Region(CellSet.of(unknown), 1, -1, u)
        }
        for (c in 0 until Grid.CELLS) {
            if (k.clue[c] < 0) continue
            val nb = Grid.NEIGHBORS[c]
            val unknown = nb.filter { k.isUnknown(it) }
            if (unknown.size < 2) continue
            val need = k.clue[c] - nb.count { k.isSeal(it) }
            if (need in 1 until unknown.size) out += Region(CellSet.of(unknown), need, c, -1)
        }
        return out
    }

    private fun focusOf(a: Region, b: Region): List<Int> = buildList {
        if (a.source >= 0) add(a.source)
        if (b.source >= 0) add(b.source)
        if (a.unit >= 0) addAll(Grid.UNITS[a.unit].toList())
        if (b.unit >= 0) addAll(Grid.UNITS[b.unit].toList())
    }.distinct()

    private fun regionSubset(k: Knowledge): List<Deduction> {
        val out = ArrayList<Deduction>()
        val rs = regions(k)
        for (a in rs) for (b in rs) {
            if (a === b || !a.cells.isSubsetOf(b.cells) || a.cells == b.cells) continue
            val diff = b.cells minus a.cells
            val rest = b.seals - a.seals
            val type = when (rest) {
                0 -> SAFE
                diff.size -> SEAL
                else -> null
            } ?: continue
            out += Deduction(Technique.REGION_SUBSET, diff.toList().map { Effect(type, it) }, focusOf(a, b),
                unit = a.unit.takeIf { it >= 0 } ?: b.unit, source = a.source.takeIf { it >= 0 } ?: b.source)
        }
        return out
    }

    private fun regionOverlap(k: Knowledge): List<Deduction> {
        val out = ArrayList<Deduction>()
        val rs = regions(k)
        for (a in rs) for (b in rs) {
            if (a === b) continue
            val inter = a.cells and b.cells
            if (inter.isEmpty || a.cells.isSubsetOf(b.cells) || b.cells.isSubsetOf(a.cells)) continue
            val aOnly = a.cells minus inter
            val bOnly = b.cells minus inter
            val least = a.seals - aOnly.size // seals forced into the overlap by A
            if (least <= 0 || least != b.seals) continue
            val effects = bOnly.toList().map { Effect(SAFE, it) } + aOnly.toList().map { Effect(SEAL, it) }
            if (effects.isNotEmpty()) {
                out += Deduction(Technique.REGION_OVERLAP, effects, focusOf(a, b),
                    unit = a.unit.takeIf { it >= 0 } ?: b.unit, source = a.source.takeIf { it >= 0 } ?: b.source)
            }
        }
        return out
    }

    // ---- Fusion: digits x seals ---------------------------------------------------------------

    private fun sealedDigitSafe(k: Knowledge): List<Deduction> {
        val sealed = k.sealedDigits()
        if (sealed == 0) return emptyList()
        val out = ArrayList<Deduction>()
        for (c in 0 until Grid.CELLS) {
            if (!k.isUnknown(c) || k.cand[c] and sealed.inv() != 0) continue
            val sources = (0 until Grid.CELLS).filter { k.isSeal(it) && k.digitKnown(it) && k.cand[it] and k.cand[c] != 0 }
            out += Deduction(Technique.SEALED_DIGIT_SAFE, listOf(Effect(SAFE, c)), sources + c,
                digit = if (Bits.single(k.cand[c])) Bits.digitOf(k.cand[c]) else 0, source = sources.firstOrNull() ?: -1)
        }
        return out
    }

    private fun sealDigitsDistinct(k: Knowledge): List<Deduction> {
        val sealed = k.sealedDigits()
        if (sealed == 0) return emptyList()
        val out = ArrayList<Deduction>()
        for (c in 0 until Grid.CELLS) {
            if (!k.isSeal(c) || k.digitKnown(c)) continue
            val drop = k.cand[c] and sealed
            if (drop != 0) {
                val sources = (0 until Grid.CELLS).filter { k.isSeal(it) && k.digitKnown(it) && k.cand[it] and drop != 0 }
                out += Deduction(Technique.SEAL_DIGITS_DISTINCT, listOf(Effect(ELIMINATE, c, drop)), sources + c, source = c)
            }
        }
        return out
    }

    /** Cells that could still be the seal hiding digit [d]. */
    private fun sealHomes(k: Knowledge, d: Int): List<Int> {
        val b = Bits.bit(d)
        return (0 until Grid.CELLS).filter { !k.isSafe(it) && k.cand[it] and b != 0 }
    }

    private fun sealDigitHome(k: Knowledge): List<Deduction> {
        val sealed = k.sealedDigits()
        val out = ArrayList<Deduction>()
        for (d in 1..9) {
            if (Bits.has(sealed, d)) continue
            val homes = sealHomes(k, d)
            if (homes.size != 1) continue
            val c = homes[0]
            val effects = buildList {
                if (!k.isSeal(c)) add(Effect(SEAL, c))
                if (!k.digitKnown(c)) add(Effect(SET_DIGIT, c, d))
            }
            if (effects.isNotEmpty()) out += Deduction(Technique.SEAL_DIGIT_HOME, effects, listOf(c), digit = d, source = c)
        }
        return out
    }

    private fun sealDigitPointing(k: Knowledge): List<Deduction> {
        val sealed = k.sealedDigits()
        val out = ArrayList<Deduction>()
        for (d in 1..9) {
            if (Bits.has(sealed, d)) continue
            val homes = sealHomes(k, d)
            if (homes.size < 2) continue
            val shared = Grid.CELL_UNITS[homes[0]].filter { u -> homes.all { u in Grid.CELL_UNITS[it] } }
            for (u in shared) {
                // The unit's only seal must be the d-seal, so cells here that cannot hold d are safe.
                val effects = Grid.UNITS[u].filter { k.isUnknown(it) && !Bits.has(k.cand[it], d) }.map { Effect(SAFE, it) }
                if (effects.isNotEmpty()) out += Deduction(Technique.SEAL_DIGIT_POINTING, effects, homes, unit = u, digit = d)
            }
        }
        return out
    }
}
