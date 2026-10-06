package com.fuse9.game

import com.fuse9.puzzle.Grid

/**
 * The first board teaches by playing. No text walls: one line at a time, each pointing at a
 * real cell on the real board, each advancing when the player does the thing — not when they
 * tap "Next".
 */
class TutorialGuide {
    enum class Step { LOOK, FIRST_PLACE, MORE, DIGIT_LINK, DONE, OFF }

    data class Line(val text: String, val cells: Set<Int> = emptySet(), val target: Int = -1)

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
                Line("Open cells show a digit. The dots say how many seals touch them.", setOfNotNull(example))
            }
            Step.FIRST_PLACE, Step.MORE -> {
                val sealHint = if (step == Step.MORE) HintEngine.find(state, prefer = HintAction.SEAL)?.takeIf { it.action == HintAction.SEAL } else null
                when {
                    sealHint != null -> Line("${sealHint.reason} Hold it — or press the seal key — to defuse.", sealHint.focus.toSet(), sealHint.cell)
                    step == Step.MORE && placements >= 2 -> Line("Nine seals: one in every row, column and box. Watch the dots.")
                    hint != null && hint.action == HintAction.PLACE ->
                        Line(if (step == Step.FIRST_PLACE) "${hint.reason} Select it and place ${hint.digit}." else "Placing a digit opens the cell and shows its dots.", hint.focus.toSet(), if (step == Step.FIRST_PLACE) hint.cell else -1)
                    else -> null
                }
            }
            Step.DIGIT_LINK -> Line("That seal hid a $sealedDigit. Each seal hides a different digit — so every other $sealedDigit is safe.",
                (0 until Grid.CELLS).filter { state.cells[it].status.isSeal }.toSet())
            Step.DONE -> Line("That's the whole idea. The rest is yours.")
            Step.OFF -> null
        }
    }
}
