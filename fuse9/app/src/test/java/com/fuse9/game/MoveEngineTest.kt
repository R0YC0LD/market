package com.fuse9.game

import com.fuse9.puzzle.Bits
import com.fuse9.puzzle.Grid
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MoveEngineTest {
    private val engine = MoveEngine()

    @Test
    fun correctDigitSolvesCell() {
        val s0 = Fixtures.game()
        val cell = Fixtures.firstHidden(s0, seal = false)
        val digit = s0.truth.solution[cell]
        val o = engine.play(s0, Action.Select(cell), Action.Digit(digit))
        assertEquals(CellStatus.SOLVED, o.state.cells[cell].status)
        assertEquals(0, o.state.mistakes)
        assertTrue(o.events.any { it is GameEvent.Placed && it.cell == cell })
        assertEquals(s0.truth.clues[cell], o.state.countAt(cell))
    }

    @Test
    fun wrongDigitIsAStrikeAndProvesSafety() {
        val s0 = Fixtures.game()
        val cell = Fixtures.firstHidden(s0, seal = false)
        val wrong = (1..9).first { it != s0.truth.solution[cell] }
        val o = engine.play(s0, Action.Select(cell), Action.Digit(wrong))
        assertEquals(CellStatus.HIDDEN, o.state.cells[cell].status)
        assertTrue(o.state.cells[cell].safe)
        assertEquals(1, o.state.mistakes)
        assertTrue(o.events.any { it is GameEvent.WrongDigit })
    }

    @Test
    fun digitOnSealTripsIt() {
        val s0 = Fixtures.game()
        val cell = Fixtures.firstHidden(s0, seal = true)
        val o = engine.play(s0, Action.Select(cell), Action.Digit(1))
        assertEquals(CellStatus.TRIPPED, o.state.cells[cell].status)
        assertEquals(1, o.state.mistakes)
        assertEquals(s0.truth.solution[cell], o.state.digitAt(cell))
    }

    @Test
    fun sealDefusesAndRevealsDigit() {
        val s0 = Fixtures.game()
        val cell = Fixtures.firstHidden(s0, seal = true)
        val o = engine.play(s0, Action.SealAt(cell))
        assertEquals(CellStatus.SEALED, o.state.cells[cell].status)
        assertEquals(s0.truth.solution[cell], o.state.digitAt(cell))
        assertEquals(1, o.state.sealsFound)
        assertTrue(o.events.any { it is GameEvent.Defused })
    }

    @Test
    fun wrongSealIsAStrike() {
        val s0 = Fixtures.game()
        val cell = Fixtures.firstHidden(s0, seal = false)
        val o = engine.play(s0, Action.SealAt(cell))
        assertEquals(1, o.state.mistakes)
        assertTrue(o.state.cells[cell].safe)
        assertEquals(CellStatus.HIDDEN, o.state.cells[cell].status)
    }

    @Test
    fun hardcoreEndsOnFirstStrike() {
        val s0 = Fixtures.game(GameMode.HARDCORE)
        val cell = Fixtures.firstHidden(s0, seal = true)
        val o = engine.play(s0, Action.Select(cell), Action.Digit(1))
        assertEquals(GameStatus.LOST, o.state.status)
        assertTrue(o.events.contains(GameEvent.Lost))
        // Nothing moves after the end.
        assertEquals(o.state, engine.reduce(o.state, Action.Digit(2)).state)
    }

    @Test
    fun classicAllowsThreeStrikes() {
        var s = Fixtures.game()
        val safes = (0 until 81).filter { s.cells[it].status == CellStatus.HIDDEN && !s.truth.isSeal[it] }.take(3)
        safes.forEachIndexed { i, c ->
            val wrong = (1..9).first { it != s.truth.solution[c] }
            s = engine.play(s, Action.Select(c), Action.Digit(wrong)).state
            assertEquals(if (i < 2) GameStatus.PLAYING else GameStatus.LOST, s.status)
        }
    }

    @Test
    fun notesToggleAndAutoClean() {
        val s0 = Fixtures.game()
        val cell = Fixtures.firstHidden(s0, seal = false)
        val digit = s0.truth.solution[cell]
        val peer = Grid.PEERS[cell].first { s0.cells[it].status == CellStatus.HIDDEN }
        var s = engine.play(s0, Action.ToggleNotes, Action.Select(peer), Action.Digit(digit), Action.Digit(9)).state
        assertTrue(Bits.has(s.cells[peer].notes, digit))
        s = engine.play(s, Action.ToggleNotes, Action.Select(cell), Action.Digit(digit)).state
        assertFalse("placing $digit clears it from peers", Bits.has(s.cells[peer].notes, digit))
    }

    @Test
    fun undoRestoresButKeepsStrikesAndExposure() {
        val s0 = Fixtures.game()
        val good = Fixtures.firstHidden(s0, seal = false)
        val seal = Fixtures.firstHidden(s0, seal = true)
        var s = engine.play(s0, Action.Select(good), Action.Digit(s0.truth.solution[good])).state
        s = engine.play(s, Action.Select(seal), Action.Digit(1)).state // strike, not undoable
        s = engine.play(s, Action.Undo).state
        assertEquals(CellStatus.HIDDEN, s.cells[good].status)
        assertEquals(CellStatus.TRIPPED, s.cells[seal].status)
        assertEquals(1, s.mistakes)
        assertTrue(engine.reduce(s, Action.Undo).events.isNotEmpty())
    }

    @Test
    fun undoOnEmptyHistoryIsRejected() {
        val o = engine.reduce(Fixtures.game(), Action.Undo)
        assertEquals(listOf(GameEvent.Rejected), o.events)
    }

    @Test
    fun solvingEverythingWins() {
        var s = Fixtures.game(GameMode.ZEN)
        val events = ArrayList<GameEvent>()
        for (c in 0 until 81) {
            if (s.cells[c].status.isResolved) continue
            val o = if (s.truth.isSeal[c]) engine.play(s, Action.SealAt(c))
            else engine.play(s, Action.Select(c), Action.Digit(s.truth.solution[c]))
            s = o.state; events += o.events
        }
        assertEquals(GameStatus.WON, s.status)
        assertTrue(events.last() == GameEvent.Won)
        assertEquals(27, events.count { it is GameEvent.UnitCompleted })
        assertEquals(9, s.sealsFound)
    }

    @Test
    fun zeroRippleMarksNeighboursSafe() {
        val s0 = Fixtures.game()
        val zero = (0 until 81).firstOrNull { s0.cells[it].status == CellStatus.HIDDEN && !s0.truth.isSeal[it] && s0.truth.clues[it] == 0 && Grid.NEIGHBORS[it].any { n -> s0.cells[n].status == CellStatus.HIDDEN && !s0.cells[n].safe } }
            ?: return
        val o = engine.play(s0, Action.Select(zero), Action.Digit(s0.truth.solution[zero]))
        val ripple = o.events.filterIsInstance<GameEvent.Ripple>().single()
        assertTrue(ripple.cells.all { o.state.cells[it].safe && !s0.truth.isSeal[it] })
    }

    @Test
    fun timerOnlyRunsWhilePlaying() {
        val s = engine.reduce(Fixtures.game(), Action.Tick(1500)).state
        assertEquals(1500, s.elapsedMillis)
        val over = s.copy(status = GameStatus.WON)
        assertEquals(over, engine.reduce(over, Action.Tick(1000)).state)
    }

    @Test
    fun suspectMarksAreNotes() {
        val s0 = Fixtures.game()
        val cell = Fixtures.firstHidden(s0, seal = false)
        val s = engine.play(s0, Action.ToggleNotes, Action.Select(cell), Action.Seal).state
        assertTrue(s.cells[cell].suspect)
        assertEquals(0, s.mistakes)
    }
}
