package com.fuse9.game

import com.fuse9.puzzle.Bits
import com.fuse9.puzzle.Grid

sealed interface Action {
    data class Select(val cell: Int) : Action
    /** Digit pad press on the selected cell: places, or toggles a note in NOTES mode. */
    data class Digit(val digit: Int) : Action
    /** Seal button on the selected cell: defuses, or toggles a suspect mark in NOTES mode. */
    data object Seal : Action
    /** Long-press shortcut: select and seal in one gesture. */
    data class SealAt(val cell: Int) : Action
    data object ToggleNotes : Action
    data object Erase : Action
    data object Undo : Action
    data class Tick(val millis: Long) : Action
}

data class Outcome(val state: GameState, val events: List<GameEvent>)

/**
 * The rules of FUSE9 as a pure function: (state, action) -> (state, events).
 * Every placement and every seal is checked against the answer immediately; a wrong one
 * is a strike and reveals the truth about that cell, so the player is never stuck guessing
 * the same cell twice.
 */
class MoveEngine(private val autoCleanNotes: Boolean = true) {

    fun reduce(state: GameState, action: Action): Outcome {
        if (action is Action.Tick) {
            return if (state.status == GameStatus.PLAYING) Outcome(state.copy(elapsedMillis = state.elapsedMillis + action.millis), emptyList())
            else Outcome(state, emptyList())
        }
        if (state.isOver) return Outcome(state, listOf(GameEvent.Rejected))
        return when (action) {
            is Action.Select -> select(state, action.cell)
            is Action.Digit -> digit(state, action.digit)
            Action.Seal -> seal(state, state.selected)
            is Action.SealAt -> {
                val selected = select(state, action.cell)
                val sealed = seal(selected.state, action.cell)
                Outcome(sealed.state, selected.events + sealed.events)
            }
            Action.ToggleNotes -> {
                val mode = if (state.inputMode == InputMode.DIGIT) InputMode.NOTES else InputMode.DIGIT
                Outcome(state.copy(inputMode = mode), listOf(GameEvent.ModeChanged(mode)))
            }
            Action.Erase -> erase(state)
            Action.Undo -> undo(state)
            is Action.Tick -> error("handled above")
        }
    }

    private fun select(state: GameState, cell: Int): Outcome {
        if (cell !in 0 until Grid.CELLS) return Outcome(state, listOf(GameEvent.Rejected))
        if (cell == state.selected) return Outcome(state, emptyList())
        return Outcome(state.copy(selected = cell), listOf(GameEvent.Selected(cell)))
    }

    private fun digit(state: GameState, digit: Int): Outcome {
        val cell = state.selected
        if (digit !in 1..9 || cell < 0) return Outcome(state, listOf(GameEvent.Rejected))
        val current = state.cells[cell]
        if (current.status.isResolved) return Outcome(state, listOf(GameEvent.Rejected))

        if (state.inputMode == InputMode.NOTES) {
            val added = !Bits.has(current.notes, digit)
            val notes = current.notes xor Bits.bit(digit)
            val next = remember(state).withCell(cell, current.copy(notes = notes))
            return Outcome(next, listOf(GameEvent.NoteToggled(cell, digit, added)))
        }

        val truth = state.truth
        val answer = truth.solution[cell]
        return when {
            truth.isSeal[cell] -> strike(state, cell, CellStatus.TRIPPED, GameEvent.Tripped(cell, answer)) { it.copy(sealHits = it.sealHits + 1) }
            digit == answer -> resolve(remember(state), cell, CellStatus.SOLVED, GameEvent.Placed(cell, digit))
            else -> {
                val withoutNote = state.withCell(cell, current.copy(notes = current.notes and Bits.bit(digit).inv()))
                strike(withoutNote, cell, null, GameEvent.WrongDigit(cell, digit)) { it.copy(wrongDigits = it.wrongDigits + 1) }
            }
        }
    }

    private fun seal(state: GameState, cell: Int): Outcome {
        if (cell < 0) return Outcome(state, listOf(GameEvent.Rejected))
        val current = state.cells[cell]
        if (current.status.isResolved) return Outcome(state, listOf(GameEvent.Rejected))
        if (state.inputMode == InputMode.NOTES) {
            val on = !current.suspect
            return Outcome(remember(state).withCell(cell, current.copy(suspect = on)), listOf(GameEvent.SuspectToggled(cell, on)))
        }
        return if (state.truth.isSeal[cell]) {
            resolve(remember(state), cell, CellStatus.SEALED, GameEvent.Defused(cell, state.truth.solution[cell]))
        } else {
            strike(state, cell, null, GameEvent.WrongSeal(cell)) { it.copy(wrongSeals = it.wrongSeals + 1) }
        }
    }

    private fun erase(state: GameState): Outcome {
        val cell = state.selected
        if (cell < 0) return Outcome(state, listOf(GameEvent.Rejected))
        val current = state.cells[cell]
        if (current.status.isResolved || (current.notes == 0 && !current.suspect)) return Outcome(state, listOf(GameEvent.Rejected))
        return Outcome(remember(state).withCell(cell, current.copy(notes = 0, suspect = false)), listOf(GameEvent.Undone(listOf(cell))))
    }

    private fun undo(state: GameState): Outcome {
        val last = state.history.lastOrNull() ?: return Outcome(state, listOf(GameEvent.Rejected))
        var restored = state.copy(cells = last.cells, selected = last.selected, history = state.history.dropLast(1))
        restored = reapplyExposed(restored)
        val changed = (0 until Grid.CELLS).filter { restored.cells[it] != state.cells[it] }
        return Outcome(restored, listOf(GameEvent.Undone(changed)))
    }

    /** Correct placement or defusal: open the cell, then ripple, unit and win checks. */
    private fun resolve(state: GameState, cell: Int, status: CellStatus, event: GameEvent): Outcome {
        val digit = state.truth.solution[cell]
        var next = state.withCell(cell, Cell(status = status)).copy(trail = state.trail + cell)
        if (autoCleanNotes) next = clearNoteFromPeers(next, cell, digit)
        val streak = next.stats.streak + 1
        next = next.copy(stats = next.stats.let {
            it.copy(
                placements = it.placements + if (status == CellStatus.SOLVED) 1 else 0,
                sealsDefused = it.sealsDefused + if (status == CellStatus.SEALED) 1 else 0,
                streak = streak, bestStreak = maxOf(it.bestStreak, streak),
            )
        })
        val events = mutableListOf(event)
        if (status == CellStatus.SOLVED && state.truth.clues[cell] == 0) {
            val (rippled, cells) = markZeroNeighbours(next, listOf(cell))
            next = rippled
            if (cells.isNotEmpty()) {
                next = next.copy(stats = next.stats.copy(ripples = next.stats.ripples + 1))
                events += GameEvent.Ripple(cell, cells)
            }
        }
        return finish(next, cell, events)
    }

    /**
     * A wrong move. Seal hits resolve the cell as TRIPPED; wrong digits or wrong seals on a safe
     * cell mark it safe. Either way the truth about that cell is now known and stays known.
     */
    private inline fun strike(
        state: GameState, cell: Int, status: CellStatus?, event: GameEvent,
        stat: (PlayStats) -> PlayStats,
    ): Outcome {
        val current = state.cells[cell]
        val updated = if (status != null) Cell(status = status) else current.copy(safe = true, suspect = false)
        var next = state.withCell(cell, updated).copy(
            mistakes = state.mistakes + 1,
            exposed = (state.exposed + cell).distinct(),
            trail = if (status != null) state.trail + cell else state.trail,
            stats = stat(state.stats).copy(streak = 0),
        )
        if (status != null && autoCleanNotes) next = clearNoteFromPeers(next, cell, state.truth.solution[cell])
        val events = mutableListOf(event)
        val limit = next.mode.mistakeLimit
        if (limit > 0 && next.mistakes >= limit) {
            return Outcome(next.copy(status = GameStatus.LOST), events + GameEvent.Lost)
        }
        return if (status != null) finish(next, cell, events) else Outcome(next, events)
    }

    private fun finish(state: GameState, cell: Int, events: MutableList<GameEvent>): Outcome {
        var next = state
        for (unit in Grid.CELL_UNITS[cell]) {
            if (Grid.UNITS[unit].all { next.cells[it].status.isResolved }) {
                events += GameEvent.UnitCompleted(unit, cell)
                next = next.copy(stats = next.stats.copy(unitsCompleted = next.stats.unitsCompleted + 1))
            }
        }
        if (next.cells.all { it.status.isResolved }) {
            next = next.copy(status = GameStatus.WON, selected = -1)
            events += GameEvent.Won
        }
        return Outcome(next, events)
    }

    private fun remember(state: GameState): GameState {
        val history = (state.history + Snapshot(state.cells, state.selected)).takeLast(HISTORY_LIMIT)
        return state.copy(history = history)
    }

    private fun clearNoteFromPeers(state: GameState, cell: Int, digit: Int): GameState {
        val bit = Bits.bit(digit)
        val cells = state.cells.toMutableList()
        var changed = false
        for (p in Grid.PEERS[cell]) {
            val c = cells[p]
            if (c.notes and bit != 0) { cells[p] = c.copy(notes = c.notes and bit.inv()); changed = true }
        }
        return if (changed) state.copy(cells = cells) else state
    }

    private fun reapplyExposed(state: GameState): GameState {
        val cells = state.cells.toMutableList()
        for (c in state.exposed) {
            cells[c] = if (state.truth.isSeal[c]) {
                if (cells[c].status.isSeal) cells[c] else Cell(status = CellStatus.TRIPPED)
            } else if (cells[c].status.isResolved) cells[c] else cells[c].copy(safe = true, suspect = false)
        }
        return state.copy(cells = cells)
    }

    companion object {
        const val HISTORY_LIMIT = 150

        /**
         * Zero ripple: every hidden neighbour of an open zero is proven safe. Returns the new state
         * and the newly marked cells, ordered clockwise from the top-left so they fall like dominoes.
         */
        fun markZeroNeighbours(state: GameState, origins: List<Int>): Pair<GameState, List<Int>> {
            val cells = state.cells.toMutableList()
            val marked = ArrayList<Int>()
            for (origin in origins) {
                for (n in clockwise(origin)) {
                    val c = cells[n]
                    if (c.status == CellStatus.HIDDEN && !c.safe) {
                        cells[n] = c.copy(safe = true, suspect = false)
                        marked += n
                    }
                }
            }
            return state.copy(cells = cells) to marked
        }

        private val CLOCKWISE = listOf(-1 to -1, -1 to 0, -1 to 1, 0 to 1, 1 to 1, 1 to 0, 1 to -1, 0 to -1)

        private fun clockwise(cell: Int): List<Int> {
            val r = Grid.row(cell)
            val c = Grid.col(cell)
            return CLOCKWISE.mapNotNull { (dr, dc) ->
                val rr = r + dr
                val cc = c + dc
                if (rr in 0..8 && cc in 0..8) Grid.cell(rr, cc) else null
            }
        }
    }
}

internal fun GameState.withCell(cell: Int, value: Cell): GameState =
    copy(cells = cells.toMutableList().also { it[cell] = value })
