package com.fuse9.ui.board

import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.withFrameNanos
import com.fuse9.game.GameEvent
import com.fuse9.game.GameState
import com.fuse9.puzzle.Grid
import kotlinx.coroutines.channels.Channel

enum class FxKind {
    PLACE, PULSE, BOX_TRACE, WRONG, WRONG_SEAL, DEFUSE, TRIP, RIPPLE, SWEEP, UNDO, NOTE, WIN_WAVE, WIN_SEAL, BREATHE, LOSS_REVEAL,
}

/** One running animation. [cell] is -1 for board-wide effects; [data] is kind-specific. */
class Fx(val kind: FxKind, val cell: Int, val start: Long, val delayMs: Int, val durMs: Int, val data: Int = 0) {
    fun progress(now: Long): Float = ((now - start) / 1_000_000f - delayMs) / durMs
    fun done(now: Long) = progress(now) >= 1f
}

/**
 * Turns game events into short, local animations. Runs a frame loop only while something is
 * moving, and redraws only the board canvas — never recomposes the screen.
 */
class BoardFx {
    val effects = ArrayList<Fx>()
    val frame = mutableLongStateOf(0L)
    var selectedAt: Long = 0L
        private set
    /** Keeps frames flowing for slow ambient motion (hint pulse). */
    @Volatile var ambient: Boolean = false
        set(value) { field = value; if (value) wake.trySend(Unit) }

    private val wake = Channel<Unit>(Channel.CONFLATED)

    private fun now() = System.nanoTime()

    private fun add(kind: FxKind, cell: Int, durMs: Int, delayMs: Int = 0, data: Int = 0, at: Long = now()) {
        effects += Fx(kind, cell, at, delayMs, durMs, data)
        wake.trySend(Unit)
    }

    fun select() {
        selectedAt = now()
        wake.trySend(Unit)
    }

    fun clear() = effects.clear()

    fun onEvent(e: GameEvent, state: GameState) {
        val t = now()
        when (e) {
            is GameEvent.Selected -> select()
            is GameEvent.Placed -> {
                add(FxKind.PLACE, e.cell, 170, at = t)
                // Resolved cells sharing a unit answer with a faint pulse, nearest first.
                for (u in Grid.CELL_UNITS[e.cell]) for (c in Grid.UNITS[u]) {
                    if (c == e.cell || !state.cells[c].status.isResolved) continue
                    val d = maxOf(kotlin.math.abs(Grid.row(c) - Grid.row(e.cell)), kotlin.math.abs(Grid.col(c) - Grid.col(e.cell)))
                    add(FxKind.PULSE, c, 220, delayMs = 40 + d * 24, at = t)
                }
            }
            is GameEvent.WrongDigit -> add(FxKind.WRONG, e.cell, 280, data = e.digit, at = t)
            is GameEvent.WrongSeal -> add(FxKind.WRONG_SEAL, e.cell, 300, at = t)
            is GameEvent.Defused -> add(FxKind.DEFUSE, e.cell, 520, at = t)
            is GameEvent.Tripped -> add(FxKind.TRIP, e.cell, 560, at = t)
            is GameEvent.Ripple -> e.cells.forEachIndexed { i, c -> add(FxKind.RIPPLE, c, 240, delayMs = 60 + i * 38, at = t) }
            is GameEvent.UnitCompleted -> {
                val cells = Grid.UNITS[e.unit]
                val origin = cells.indexOf(e.origin).coerceAtLeast(0)
                cells.forEachIndexed { i, c -> add(FxKind.SWEEP, c, 300, delayMs = 120 + kotlin.math.abs(i - origin) * 26, data = e.unit, at = t) }
                if (e.unit >= 18) add(FxKind.BOX_TRACE, -1, 520, delayMs = 100, data = e.unit, at = t)
            }
            is GameEvent.Undone -> e.changed.forEach { add(FxKind.UNDO, it, 170, at = t) }
            is GameEvent.NoteToggled -> add(FxKind.NOTE, e.cell, 130, data = e.digit * 10 + if (e.added) 1 else 0, at = t)
            is GameEvent.SuspectToggled -> add(FxKind.NOTE, e.cell, 130, data = if (e.on) 1 else 0, at = t)
            GameEvent.Won -> {
                for (c in 0 until Grid.CELLS) add(FxKind.WIN_WAVE, c, 320, delayMs = 160 + (Grid.row(c) + Grid.col(c)) * 26, at = t)
                state.puzzle.seals.sortedBy { Grid.row(it) }.forEachIndexed { i, c -> add(FxKind.WIN_SEAL, c, 320, delayMs = 760 + i * 70, at = t) }
                add(FxKind.BREATHE, -1, 820, delayMs = 1420, at = t)
            }
            GameEvent.Lost -> state.puzzle.seals.forEachIndexed { i, c ->
                if (!state.cells[c].status.isResolved) add(FxKind.LOSS_REVEAL, c, 360, delayMs = 250 + i * 60, at = t)
            }
            else -> Unit
        }
    }

    /** Frame pump. Suspends entirely while nothing is animating. */
    suspend fun run() {
        while (true) {
            if (effects.isEmpty() && !ambient && now() - selectedAt > SELECT_NANOS) wake.receive()
            withFrameNanos { frameNanos ->
                val t = now()
                effects.removeAll { it.done(t) }
                frame.longValue = frameNanos
            }
        }
    }

    companion object {
        const val SELECT_MS = 95
        private const val SELECT_NANOS = SELECT_MS * 1_000_000L + 20_000_000L
    }
}
