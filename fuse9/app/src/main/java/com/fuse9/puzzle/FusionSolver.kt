package com.fuse9.puzzle

/** Statistics from a full human-style solve; drives difficulty grading. */
data class SolveReport(
    val solved: Boolean,
    val usage: Map<Technique, Int>,
    val waves: List<Int>,
    val steps: Int,
    val cascadeCells: Int,
) {
    val maxLevel: Int get() = usage.keys.maxOfOrNull { it.level } ?: 0
    val fusionSteps: Int get() = usage.filterKeys { it.kind == TechniqueKind.FUSION }.values.sum()
    val tightWaves: Int get() = waves.count { it <= 2 }
    fun uses(t: Technique) = usage[t] ?: 0
}

/**
 * Solves FUSE9 the way a careful player would: only with the information visible on the
 * board, using the cheapest technique that makes progress, and making every move it can
 * prove (placing a digit reveals that cell's seal count, defusing a seal reveals its digit).
 *
 * When it finishes, the puzzle has exactly one answer reachable without guessing.
 */
class FusionSolver(
    private val truth: Truth,
    private val allowed: Set<Technique> = Technique.entries.toSet(),
    private val rules: Rules = Rules.DEFAULT,
    /** Throws if a deduction contradicts the answer. Cheap; keep on outside hot loops. */
    private val verify: Boolean = true,
) {
    private val ordered = allowed.filter { it != Technique.NAKED_SINGLE }.sortedBy { it.weight }
    private val easy = ordered.filter { it.level <= 1 }
    private val hard = ordered.filter { it.level > 1 }

    fun solve(givens: Collection<Int>): SolveReport = solve(initialKnowledge(truth, givens, rules))

    fun solve(k: Knowledge): SolveReport {
        val usage = HashMap<Technique, Int>()
        val waves = ArrayList<Int>()
        var steps = 0
        var cascade = 0
        var guard = 0
        while (guard++ < 10_000) {
            val singles = Techniques.eliminate(k).count { !k.resolved[it] }
            if (singles > 0) usage.merge(Technique.NAKED_SINGLE, singles, Int::plus)
            if (k.allResolved()) return SolveReport(true, usage, waves, steps, cascade)
            if (applyFirst(k, easy, usage)) { steps++; continue }
            val (moves, opened) = makeMoves(k)
            if (moves > 0) { waves += moves; cascade += opened; continue }
            if (applyFirst(k, hard, usage)) { steps++; continue }
            return SolveReport(false, usage, waves, steps, cascade)
        }
        error("solver did not converge")
    }

    private fun applyFirst(k: Knowledge, techniques: List<Technique>, usage: MutableMap<Technique, Int>): Boolean {
        for (t in techniques) {
            var applied = 0
            for (d in Techniques.find(t, k)) if (apply(k, d)) applied++
            if (applied > 0) {
                usage.merge(t, applied, Int::plus)
                return true
            }
        }
        return false
    }

    fun apply(k: Knowledge, d: Deduction): Boolean {
        var changed = false
        for (e in d.effects) {
            if (verify) check(consistent(e)) { "unsound ${d.technique} at ${Grid.cellName(e.cell)}: $e" }
            changed = applyEffect(k, e) || changed
        }
        return changed
    }

    private fun consistent(e: Effect): Boolean = when (e.type) {
        EffectType.SET_DIGIT -> truth.solution[e.cell] == e.value
        EffectType.ELIMINATE -> !Bits.has(e.value, truth.solution[e.cell])
        EffectType.SAFE -> !truth.isSeal[e.cell]
        EffectType.SEAL -> truth.isSeal[e.cell]
    }

    /** Plays every provable move. Returns (moves made, cells opened by zero-count cascades). */
    private fun makeMoves(k: Knowledge): Pair<Int, Int> {
        var moves = 0
        var opened = 0
        for (c in 0 until Grid.CELLS) {
            if (k.canPlace(c)) {
                if (verify) check(Bits.digitOf(k.cand[c]) == truth.solution[c] && !truth.isSeal[c])
                k.resolved[c] = true
                opened += reveal(k, truth, c, rules)
                moves++
            } else if (k.canSeal(c)) {
                k.resolved[c] = true
                k.cand[c] = Bits.bit(truth.solution[c])
                moves++
            }
        }
        return moves to opened
    }

    companion object {
        fun applyEffect(k: Knowledge, e: Effect): Boolean {
            val c = e.cell
            return when (e.type) {
                EffectType.SET_DIGIT -> {
                    val b = Bits.bit(e.value)
                    if (k.cand[c] == b) false else { k.cand[c] = b; true }
                }
                EffectType.ELIMINATE -> {
                    val after = k.cand[c] and e.value.inv()
                    if (after == k.cand[c]) false else { k.cand[c] = after; true }
                }
                EffectType.SAFE -> if (k.isSafe(c)) false else { k.safety[c] = Knowledge.SAFE; true }
                EffectType.SEAL -> if (k.isSeal(c)) false else { k.safety[c] = Knowledge.SEAL; true }
            }
        }

        /**
         * Makes a safe cell's seal count visible and, if it is zero, clears its neighbours too
         * (their counts appear, digits stay hidden). Returns how many extra cells were cleared.
         */
        fun reveal(k: Knowledge, truth: Truth, cell: Int, rules: Rules): Int {
            k.safety[cell] = Knowledge.SAFE
            if (k.clue[cell] >= 0) return 0
            k.clue[cell] = truth.clues[cell]
            if (!rules.zeroCascade) return 0
            val opened = cascadeOrder(cell, truth) { k.clue[it] >= 0 }
            for (c in opened) {
                k.safety[c] = Knowledge.SAFE
                k.clue[c] = truth.clues[c]
            }
            return opened.size
        }

        /** Shared flood-fill used by the solver and the game engine. Returns cells newly opened, in order. */
        fun cascadeOrder(start: Int, truth: Truth, isOpen: (Int) -> Boolean): List<Int> {
            val out = ArrayList<Int>()
            if (truth.clues[start] != 0) return out
            val seen = BooleanArray(Grid.CELLS)
            val queue = ArrayDeque<Int>()
            queue.add(start); seen[start] = true
            while (queue.isNotEmpty()) {
                val c = queue.removeFirst()
                if (truth.clues[c] != 0) continue
                for (n in Grid.NEIGHBORS[c]) {
                    if (seen[n]) continue
                    seen[n] = true
                    if (isOpen(n)) continue
                    out += n
                    queue.add(n)
                }
            }
            return out
        }

        /** Knowledge at the start of a puzzle: only the given cells are open. */
        fun initialKnowledge(truth: Truth, givens: Collection<Int>, rules: Rules): Knowledge {
            val k = Knowledge()
            for (g in givens) {
                k.cand[g] = Bits.bit(truth.solution[g])
                k.safety[g] = Knowledge.SAFE
                k.resolved[g] = true
            }
            for (g in givens) reveal(k, truth, g, rules)
            return k
        }
    }
}

/**
 * Rule switches; kept explicit so saved puzzles record what they were built with.
 *
 * zeroCascade (off by default): opening a zero count would reveal the neighbours' counts.
 * Generator surveys showed it hands out so much seal information that Hard+ tiers lose
 * their place-learn-deduce rhythm, so FUSE9 only marks those neighbours safe instead.
 */
data class Rules(val zeroCascade: Boolean = false) {
    companion object {
        val DEFAULT = Rules()
    }
}
