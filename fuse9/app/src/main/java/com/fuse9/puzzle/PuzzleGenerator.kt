package com.fuse9.puzzle

/**
 * The pipeline:
 *  1. complete Sudoku  2. seal layout (row/col/box/digit transversal)
 *  3. start with every safe cell given  4. remove givens in seeded order while the
 *     human-style solver — restricted to the tier's techniques — still finishes
 *  5. grade, and discard anything off-tier, too regular, or with an unfair opening.
 * Deterministic: the same seed and difficulty always yield the same puzzle.
 */
object PuzzleGenerator {
    const val VERSION = 1
    private const val MAX_ATTEMPTS = 40

    fun generate(seed: Long, difficulty: Difficulty, rules: Rules = Rules.DEFAULT): Puzzle {
        val started = System.nanoTime()
        val rng = Rng(seed xor (difficulty.ordinal.toLong() shl 56))
        var fallback: Puzzle? = null
        repeat(MAX_ATTEMPTS) {
            val candidate = attempt(rng, seed, difficulty, rules) ?: return@repeat
            val elapsed = (System.nanoTime() - started) / 1_000_000
            if (candidate.second) return candidate.first.copy(generationMillis = elapsed)
            if (fallback == null) fallback = candidate.first.copy(generationMillis = elapsed)
        }
        // Never leave the player without a board: the closest solvable candidate is still fair.
        return fallback ?: error("could not generate a puzzle for seed $seed")
    }

    /** Returns the puzzle and whether it lands exactly on the requested tier. */
    private fun attempt(rng: Rng, seed: Long, difficulty: Difficulty, rules: Rules): Pair<Puzzle, Boolean>? {
        val solution = Sudoku.generateSolution(rng)
        val seals = Seals.generate(solution, rng) ?: return null
        val truth = Truth(solution, seals)
        val solver = FusionSolver(truth, difficulty.techniques, rules, verify = false)

        val givens = (0 until Grid.CELLS).filter { !truth.isSeal[it] }.toMutableSet()
        for (cell in rng.shuffled(Grid.CELLS)) {
            if (givens.size <= difficulty.minGivens) break
            if (cell !in givens) continue
            givens.remove(cell)
            if (!isFair(truth, givens, rules) || !solver.solve(givens).solved) givens.add(cell)
        }
        val report = FusionSolver(truth, difficulty.techniques, rules, verify = true).solve(givens)
        if (!report.solved) return null
        val rating = DifficultyAnalyzer.rate(report, givens.size)
        val integrated = sudokuNeedsSeals(truth, givens, difficulty) && report.fusionSteps >= 3
        val puzzle = Puzzle(
            seed = seed, difficulty = difficulty, generatorVersion = VERSION,
            solution = solution.toList(), seals = seals.toList(), givens = givens.sorted(),
            score = rating.score, maxLevel = rating.maxLevel, zeroCascade = rules.zeroCascade,
            solverSteps = rating.steps, usage = report.usage,
        )
        return puzzle to (integrated && DifficultyAnalyzer.fits(difficulty, rating))
    }

    /**
     * Fair opening: the cascade from the givens may not pre-open more than a third of the
     * board, and every box must have at least one given so no region starts blind.
     */
    fun isFair(truth: Truth, givens: Set<Int>, rules: Rules): Boolean {
        for (box in 18 until 27) if (Grid.UNITS[box].none { it in givens }) return false
        val k = FusionSolver.initialKnowledge(truth, givens, rules)
        val cleared = (0 until Grid.CELLS).count { k.clue[it] >= 0 && !k.resolved[it] }
        return cleared <= 27
    }

    /**
     * Integration check: with the givens alone and pure Sudoku logic the grid must NOT be
     * solvable. If it were, the seals would be decoration.
     */
    fun sudokuNeedsSeals(truth: Truth, givens: Set<Int>, difficulty: Difficulty): Boolean {
        val k = Knowledge()
        for (g in givens) k.cand[g] = Bits.bit(truth.solution[g])
        val sudokuOnly = difficulty.techniques.filter { it.kind == TechniqueKind.SUDOKU && it != Technique.NAKED_SINGLE }
        while (true) {
            Techniques.eliminate(k)
            var progressed = false
            for (t in sudokuOnly) {
                val ds = Techniques.find(t, k)
                for (d in ds) for (e in d.effects) progressed = FusionSolver.applyEffect(k, e) || progressed
                if (progressed) break
            }
            if (!progressed) break
        }
        return (0 until Grid.CELLS).any { !k.digitKnown(it) }
    }
}
