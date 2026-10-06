package com.fuse9.puzzle

enum class TechniqueKind { SUDOKU, SEAL, FUSION }

/**
 * Human-style deductions, ordered roughly by how hard they are to spot.
 * `level` gates which difficulties may require a technique; `weight` feeds the score.
 * FUSION techniques are the ones that need both digit logic and seal logic at once.
 */
enum class Technique(val level: Int, val weight: Int, val kind: TechniqueKind) {
    NAKED_SINGLE(1, 1, TechniqueKind.SUDOKU),
    HIDDEN_SINGLE(1, 2, TechniqueKind.SUDOKU),
    UNIT_SEALED(1, 1, TechniqueKind.SEAL),
    UNIT_LAST_CELL(1, 2, TechniqueKind.SEAL),
    COUNT_SATISFIED(1, 1, TechniqueKind.SEAL),
    COUNT_FULL(1, 2, TechniqueKind.SEAL),
    SEALED_DIGIT_SAFE(1, 2, TechniqueKind.FUSION),
    SEAL_DIGITS_DISTINCT(1, 2, TechniqueKind.FUSION),
    SEAL_DIGIT_HOME(2, 4, TechniqueKind.FUSION),
    SEAL_DIGIT_POINTING(3, 6, TechniqueKind.FUSION),
    LOCKED_CANDIDATES(3, 5, TechniqueKind.SUDOKU),
    REGION_SUBSET(3, 6, TechniqueKind.SEAL),
    NAKED_SUBSET(4, 9, TechniqueKind.SUDOKU),
    HIDDEN_PAIR(4, 10, TechniqueKind.SUDOKU),
    REGION_OVERLAP(4, 10, TechniqueKind.SEAL);

    companion object {
        fun upTo(level: Int): Set<Technique> = entries.filter { it.level <= level }.toSet()
    }
}

enum class EffectType { SET_DIGIT, ELIMINATE, SAFE, SEAL }

/** One atomic change to the solver's knowledge. For ELIMINATE, [value] is a digit mask. */
data class Effect(val type: EffectType, val cell: Int, val value: Int = 0)

/**
 * A single line of reasoning: what was concluded, by which technique, and which cells the
 * reasoning looked at. [unit] and [source] let the hint system point the player at the
 * right region or clue.
 */
data class Deduction(
    val technique: Technique,
    val effects: List<Effect>,
    val focus: List<Int>,
    val unit: Int = -1,
    val digit: Int = 0,
    val source: Int = -1,
)
