package com.fuse9.game

import com.fuse9.puzzle.Grid
import com.fuse9.puzzle.Puzzle
import kotlinx.serialization.Serializable

@Serializable
enum class CellStatus {
    /** Unknown to the player. */
    HIDDEN,
    /** Open from the start. */
    GIVEN,
    /** Player placed the correct digit. */
    SOLVED,
    /** Player defused the seal; its digit is now shown. */
    SEALED,
    /** Player stepped on the seal; it is resolved but counted as a strike. */
    TRIPPED;

    val isResolved: Boolean get() = this != HIDDEN
    val isSeal: Boolean get() = this == SEALED || this == TRIPPED
    val showsCount: Boolean get() = this == GIVEN || this == SOLVED
}

@Serializable
data class Cell(
    val status: CellStatus = CellStatus.HIDDEN,
    /** Player's pencil candidates (digit bitmask). */
    val notes: Int = 0,
    /** Player's own unverified "I think this is a seal" mark. */
    val suspect: Boolean = false,
    /** Proven safe: beside an open zero, or revealed by a strike. */
    val safe: Boolean = false,
)

@Serializable
enum class GameStatus { PLAYING, WON, LOST }

@Serializable
enum class InputMode { DIGIT, NOTES }

@Serializable
data class PlayStats(
    val placements: Int = 0,
    val sealsDefused: Int = 0,
    val wrongDigits: Int = 0,
    val sealHits: Int = 0,
    val wrongSeals: Int = 0,
    val ripples: Int = 0,
    val unitsCompleted: Int = 0,
    val bestStreak: Int = 0,
    val streak: Int = 0,
)

/** Board content that undo restores. Strikes and what they revealed are never undone. */
@Serializable
data class Snapshot(val cells: List<Cell>, val selected: Int)

@Serializable
data class GameState(
    val puzzle: Puzzle,
    val mode: GameMode,
    val cells: List<Cell>,
    val selected: Int = -1,
    val inputMode: InputMode = InputMode.DIGIT,
    val mistakes: Int = 0,
    val hintsUsed: Int = 0,
    val elapsedMillis: Long = 0,
    val status: GameStatus = GameStatus.PLAYING,
    val stats: PlayStats = PlayStats(),
    val history: List<Snapshot> = emptyList(),
    /** Cells whose truth a strike exposed; re-applied after undo. */
    val exposed: List<Int> = emptyList(),
    val tutorial: Boolean = false,
    /** Cells in the order they were resolved — drives the result-screen replay. */
    val trail: List<Int> = emptyList(),
) {
    val truth get() = puzzle.truth

    fun digitAt(cell: Int): Int = if (cells[cell].status.isResolved) puzzle.truth.solution[cell] else 0
    fun countAt(cell: Int): Int = if (cells[cell].status.showsCount) puzzle.truth.clues[cell] else -1

    val sealsFound: Int get() = cells.count { it.status.isSeal }
    val isOver: Boolean get() = status != GameStatus.PLAYING

    /** How many of each digit are still unplaced, index 1..9. */
    fun remaining(): IntArray {
        val left = IntArray(10) { 9 }
        left[0] = 0
        for (c in 0 until Grid.CELLS) if (cells[c].status.isResolved) left[puzzle.truth.solution[c]]--
        return left
    }

    companion object {
        fun new(puzzle: Puzzle, mode: GameMode, tutorial: Boolean = false): GameState {
            val givens = puzzle.givens.toSet()
            val cells = List(Grid.CELLS) { if (it in givens) Cell(status = CellStatus.GIVEN) else Cell() }
            val base = GameState(puzzle = puzzle, mode = mode, cells = cells, tutorial = tutorial)
            // Zeros among the givens already prove their neighbours safe.
            return MoveEngine.markZeroNeighbours(base, givens.filter { puzzle.truth.clues[it] == 0 }).first
        }
    }
}
