package com.fuse9.game

import com.fuse9.puzzle.Grid

/**
 * The first board teaches by playing. No text walls: one line at a time, each pointing at a
 * real cell on the real board, each advancing when the player does the thing — not when they
 * tap "Next".
 */
class TutorialGuide {
    enum class Step { LOOK, FIRST_PLACE, MORE, DIGIT_LINK, DONE, OFF }

    enum class Kind { LOOK, PLACE_PROVEN, PLACE_BECAUSE, DEFUSE, NINE_SEALS, OPENS, DIGIT_LINK, DONE }

    /** One guide line: what to say ([kind], [reasons], [digit]) and which cells to point at. */
    data class Line(
        val kind: Kind,
        val cells: Set<Int> = emptySet(),
        val target: Int = -1,
        val digit: Int = 0,
        val reasons: List<Reason> = emptyList(),
    )

    var step: Step = Step.LOOK
        private set
    private var placements = 0
    private var sealedDigit = 0

    fun onEvent(e: GameEvent) {
        when (e) {
            is GameEvent.Selected -> if (step == Step.LOOK) step = Step.FIRST_PLACE
            is GameEvent.Placed -> {
                placements++
                when (step) {
                    Step.FIRST_PLACE, Step.LOOK -> step = Step.MORE
                    Step.DIGIT_LINK -> step = Step.DONE
                    else -> Unit
                }
            }
            is GameEvent.Defused -> if (step != Step.DONE && step != Step.OFF) { sealedDigit = e.digit; step = Step.DIGIT_LINK }
            GameEvent.Won, GameEvent.Lost -> step = Step.OFF
            else -> Unit
        }
    }

    fun dismissDone() { if (step == Step.DONE) step = Step.OFF }

    val finished: Boolean get() = step == Step.DONE || step == Step.OFF

    fun line(state: GameState): Line? {
        val hint = if (step == Step.FIRST_PLACE || step == Step.MORE) HintEngine.find(state) else null
        return when (step) {
            Step.LOOK -> {
                val example = (0 until Grid.CELLS).firstOrNull { state.cells[it].status == CellStatus.GIVEN && state.truth.clues[it] > 0 }
                Line(Kind.LOOK, setOfNotNull(example))
            }
            Step.FIRST_PLACE, Step.MORE -> {
                val sealHint = if (step == Step.MORE) HintEngine.find(state, prefer = HintAction.SEAL)?.takeIf { it.action == HintAction.SEAL } else null
                when {
                    sealHint != null -> Line(Kind.DEFUSE, sealHint.focus.toSet(), sealHint.cell, reasons = sealHint.reasons)
                    step == Step.MORE && placements >= 2 -> Line(Kind.NINE_SEALS)
                    hint != null && hint.action == HintAction.PLACE && step == Step.FIRST_PLACE ->
                        if (state.cells[hint.cell].safe) Line(Kind.PLACE_PROVEN, hint.focus.toSet(), hint.cell, hint.digit)
                        else Line(Kind.PLACE_BECAUSE, hint.focus.toSet(), hint.cell, hint.digit, hint.reasons.take(1))
                    hint != null && hint.action == HintAction.PLACE -> Line(Kind.OPENS)
                    else -> null
                }
            }
            Step.DIGIT_LINK -> Line(Kind.DIGIT_LINK, (0 until Grid.CELLS).filter { state.cells[it].status.isSeal }.toSet(), digit = sealedDigit)
            Step.DONE -> Line(Kind.DONE)
            Step.OFF -> null
        }
    }
}
