package com.fuse9.game

import com.fuse9.puzzle.Bits
import com.fuse9.puzzle.Deduction
import com.fuse9.puzzle.EffectType
import com.fuse9.puzzle.FusionSolver
import com.fuse9.puzzle.Grid
import com.fuse9.puzzle.Knowledge
import com.fuse9.puzzle.Technique
import com.fuse9.puzzle.Techniques

enum class HintAction { PLACE, SEAL }

/**
 * A hint is taught in three steps: where to look ([region]), why ([reasons], with [focus]
 * cells lit), and finally the move itself.
 */
data class Hint(
    val cell: Int,
    val action: HintAction,
    val digit: Int,
    val region: List<Int>,
    val look: Look,
    val reasons: List<Reason>,
    val focus: List<Int>,
    val technique: Technique?,
)

/**
 * Finds the next provable move from what is visible on the board — never from the answer —
 * and explains it in the player's terms.
 */
object HintEngine {
    private val ORDER = Technique.entries.filter { it != Technique.NAKED_SINGLE }.sortedBy { it.weight }

    fun visibleKnowledge(state: GameState): Knowledge {
        val k = Knowledge()
        val truth = state.truth
        for (c in 0 until Grid.CELLS) {
            val cell = state.cells[c]
            when {
                cell.status.isSeal -> {
                    k.safety[c] = Knowledge.SEAL
                    k.cand[c] = Bits.bit(truth.solution[c])
                    k.resolved[c] = true
                }
                cell.status.isResolved -> {
                    k.safety[c] = Knowledge.SAFE
                    k.cand[c] = Bits.bit(truth.solution[c])
                    k.clue[c] = truth.clues[c]
                    k.resolved[c] = true
                }
                cell.safe -> k.safety[c] = Knowledge.SAFE
            }
        }
        return k
    }

    /**
     * [prefer] = SEAL keeps reasoning until a defusal is provable (used by the tutorial to
     * teach seals); otherwise the first provable move wins.
     */
    fun find(state: GameState, prefer: HintAction? = null): Hint? {
        if (state.isOver) return null
        val k = visibleKnowledge(state)
        val digitWhy = arrayOfNulls<Deduction>(Grid.CELLS)
        val safetyWhy = arrayOfNulls<Deduction>(Grid.CELLS)
        var last: Deduction? = null
        repeat(MAX_STEPS) {
            Techniques.eliminate(k)
            val move = pickMove(k, state.selected, prefer)
            if (move >= 0) return build(state, k, move, digitWhy[move], safetyWhy[move], last)
            val d = ORDER.firstNotNullOfOrNull { t -> Techniques.find(t, k).firstOrNull { applies(k, it) } }
                ?: return if (prefer != null) find(state, null) else null
            for (e in d.effects) {
                if (!FusionSolver.applyEffect(k, e)) continue
                when (e.type) {
                    EffectType.SET_DIGIT, EffectType.ELIMINATE -> digitWhy[e.cell] = d
                    EffectType.SAFE, EffectType.SEAL -> safetyWhy[e.cell] = d
                }
            }
            last = d
        }
        return null
    }

    private fun applies(k: Knowledge, d: Deduction): Boolean {
        val probe = k.copy()
        return d.effects.any { FusionSolver.applyEffect(probe, it) }
    }

    /** Prefers the provable move nearest to where the player is already looking. */
    private fun pickMove(k: Knowledge, selected: Int, prefer: HintAction?): Int {
        var best = -1
        var bestDist = Int.MAX_VALUE
        for (c in 0 until Grid.CELLS) {
            val ok = when (prefer) {
                HintAction.SEAL -> k.canSeal(c)
                HintAction.PLACE -> k.canPlace(c)
                null -> k.canPlace(c) || k.canSeal(c)
            }
            if (!ok) continue
            val dist = if (selected < 0) c else
                maxOf(kotlin.math.abs(Grid.row(c) - Grid.row(selected)), kotlin.math.abs(Grid.col(c) - Grid.col(selected)))
            if (dist < bestDist) { best = c; bestDist = dist }
        }
        return best
    }

    private fun build(state: GameState, k: Knowledge, cell: Int, digitWhy: Deduction?, safetyWhy: Deduction?, last: Deduction?): Hint {
        val seal = k.isSeal(cell)
        val digit = state.truth.solution[cell]
        val key = if (seal) safetyWhy else (safetyWhy ?: digitWhy)
        val anchor = key ?: digitWhy ?: last
        val (region, look) = regionOf(anchor, cell)
        val reasons = if (seal) listOf(sealReason(safetyWhy)) else listOf(safetyReason(state, cell, safetyWhy), digitReason(digit, digitWhy))
        val focus = ((safetyWhy?.focus ?: emptyList()) + (digitWhy?.focus ?: emptyList()) + cell).distinct()
        return Hint(cell, if (seal) HintAction.SEAL else HintAction.PLACE, digit, region, look, reasons, focus, anchor?.technique)
    }

    private fun regionOf(d: Deduction?, cell: Int): Pair<List<Int>, Look> = when {
        d == null -> Grid.UNITS[Grid.boxUnit(cell)].toList() to Look(LookAt.AROUND_BOX, unit = Grid.boxUnit(cell))
        d.unit >= 0 -> Grid.UNITS[d.unit].toList() to Look(LookAt.UNIT, unit = d.unit)
        d.source >= 0 && d.technique.name.startsWith("COUNT") ->
            (Grid.NEIGHBORS[d.source].toList() + d.source) to Look(LookAt.DOTS, cell = d.source)
        else -> d.focus to Look(LookAt.SEALS)
    }

    private fun sealReason(d: Deduction?): Reason = when (d?.technique) {
        Technique.UNIT_LAST_CELL -> Reason(Why.UNIT_LAST_CELL, unit = d.unit)
        Technique.COUNT_FULL -> Reason(Why.COUNT_FULL, cell = d.source)
        Technique.SEAL_DIGIT_HOME -> Reason(Why.SEAL_HOME, digit = d.digit)
        Technique.REGION_SUBSET, Technique.REGION_OVERLAP -> Reason(Why.OVERLAP_SEAL)
        else -> Reason(Why.MUST_BE_SEAL)
    }

    private fun safetyReason(state: GameState, cell: Int, d: Deduction?): Reason = when (d?.technique) {
        null -> Reason(if (state.cells[cell].safe) Why.PROVEN_SAFE else Why.SAFE)
        Technique.UNIT_SEALED -> Reason(Why.UNIT_SEALED, unit = d.unit)
        Technique.COUNT_SATISFIED -> Reason(Why.COUNT_SATISFIED, cell = d.source)
        Technique.SEALED_DIGIT_SAFE -> Reason(Why.SEALED_DIGIT_SAFE)
        Technique.SEAL_DIGIT_POINTING -> Reason(Why.SEAL_POINTING, unit = d.unit, digit = d.digit)
        Technique.REGION_SUBSET, Technique.REGION_OVERLAP -> Reason(Why.OVERLAP_SAFE)
        else -> Reason(Why.SAFE)
    }

    private fun digitReason(digit: Int, d: Deduction?): Reason = when (d?.technique) {
        Technique.HIDDEN_SINGLE -> Reason(Why.HIDDEN_SINGLE, unit = d.unit, digit = digit)
        Technique.SEAL_DIGIT_HOME -> Reason(Why.SEAL_HOME_DIGIT, digit = digit)
        Technique.LOCKED_CANDIDATES, Technique.NAKED_SUBSET, Technique.HIDDEN_PAIR -> Reason(Why.NARROWED, unit = d.unit, digit = digit)
        else -> Reason(Why.ONLY_FIT, digit = digit)
    }

    private const val MAX_STEPS = 400
}
