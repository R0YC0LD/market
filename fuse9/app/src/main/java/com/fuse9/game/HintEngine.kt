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
 * A hint is taught in three steps: where to look ([region]), why ([reason], with [focus]
 * cells lit), and finally the move itself.
 */
data class Hint(
    val cell: Int,
    val action: HintAction,
    val digit: Int,
    val region: List<Int>,
    val regionLabel: String,
    val reason: String,
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
        val (region, label) = regionOf(anchor, cell)
        val reason = if (seal) {
            sealReason(safetyWhy)
        } else {
            listOf(safetyReason(state, cell, safetyWhy), digitReason(cell, digit, digitWhy)).joinToString(" ")
        }
        val focus = ((safetyWhy?.focus ?: emptyList()) + (digitWhy?.focus ?: emptyList()) + cell).distinct()
        return Hint(cell, if (seal) HintAction.SEAL else HintAction.PLACE, digit, region, label, reason, focus, anchor?.technique)
    }

    private fun regionOf(d: Deduction?, cell: Int): Pair<List<Int>, String> = when {
        d == null -> Grid.UNITS[Grid.boxUnit(cell)].toList() to "Look around ${Grid.unitName(Grid.boxUnit(cell))}."
        d.unit >= 0 -> Grid.UNITS[d.unit].toList() to "Look at ${Grid.unitName(d.unit)}."
        d.source >= 0 && d.technique.name.startsWith("COUNT") ->
            (Grid.NEIGHBORS[d.source].toList() + d.source) to "Look at the dots in ${Grid.cellName(d.source)}."
        else -> d.focus to "Look at the seals you have found."
    }

    private fun sealReason(d: Deduction?): String = when (d?.technique) {
        Technique.UNIT_LAST_CELL -> "${cap(Grid.unitName(d.unit))} needs one seal, and every other cell in it is safe."
        Technique.COUNT_FULL -> "The dots at ${Grid.cellName(d.source)} need every hidden neighbour to be a seal."
        Technique.SEAL_DIGIT_HOME -> "Some seal must hide the ${d.digit}. This is the only cell left that can."
        Technique.REGION_SUBSET, Technique.REGION_OVERLAP -> "Compare the overlapping dots: their seals can only fit here."
        else -> "This cell has to be a seal."
    }

    private fun safetyReason(state: GameState, cell: Int, d: Deduction?): String = when (d?.technique) {
        null -> if (state.cells[cell].safe) "It's proven safe." else "It's safe."
        Technique.UNIT_SEALED -> "${cap(Grid.unitName(d.unit))} already has its seal, so this is safe."
        Technique.COUNT_SATISFIED -> "The dots at ${Grid.cellName(d.source)} are satisfied, so this is safe."
        Technique.SEALED_DIGIT_SAFE -> "Its digit is already sealed, and a digit hides only once — safe."
        Technique.SEAL_DIGIT_POINTING -> "The ${d.digit}-seal lies in ${Grid.unitName(d.unit)}; this cell can't be ${d.digit}, so it's safe."
        Technique.REGION_SUBSET, Technique.REGION_OVERLAP -> "Overlapping dots leave no room for a seal here."
        else -> "It's safe."
    }

    private fun digitReason(cell: Int, digit: Int, d: Deduction?): String = when (d?.technique) {
        Technique.HIDDEN_SINGLE -> "In ${Grid.unitName(d.unit)}, $digit has nowhere else to go."
        Technique.SEAL_DIGIT_HOME -> "The $digit-seal has only this home."
        Technique.LOCKED_CANDIDATES, Technique.NAKED_SUBSET, Technique.HIDDEN_PAIR ->
            "After narrowing ${Grid.unitName(d.unit)}, only $digit fits."
        else -> "Only $digit fits: its row, column and box hold the rest."
    }

    private fun cap(s: String) = s.replaceFirstChar { it.uppercase() }

    private const val MAX_STEPS = 400
}
