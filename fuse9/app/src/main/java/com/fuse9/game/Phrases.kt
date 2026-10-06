package com.fuse9.game

/**
 * Language-neutral explanations. The rules layer says *what* the reasoning was; the UI decides
 * how to word it in the player's language.
 */
enum class Why {
    // Why a cell is safe
    PROVEN_SAFE, SAFE, UNIT_SEALED, COUNT_SATISFIED, SEALED_DIGIT_SAFE, SEAL_POINTING, OVERLAP_SAFE,
    // Why a cell is a seal
    UNIT_LAST_CELL, COUNT_FULL, SEAL_HOME, OVERLAP_SEAL, MUST_BE_SEAL,
    // Why a cell holds its digit
    HIDDEN_SINGLE, SEAL_HOME_DIGIT, NARROWED, ONLY_FIT,
}

/** [unit] and [cell] are -1 when not relevant; [digit] is 0 when not relevant. */
data class Reason(val why: Why, val unit: Int = -1, val cell: Int = -1, val digit: Int = 0)

enum class LookAt { AROUND_BOX, UNIT, DOTS, SEALS }

data class Look(val at: LookAt, val unit: Int = -1, val cell: Int = -1)
