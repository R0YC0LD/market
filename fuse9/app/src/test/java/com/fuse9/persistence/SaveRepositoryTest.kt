package com.fuse9.persistence

import com.fuse9.game.Action
import com.fuse9.game.Fixtures
import com.fuse9.game.GameState
import com.fuse9.game.MoveEngine
import com.fuse9.game.play
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SaveRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun midGame(): GameState {
        val s0 = Fixtures.game()
        val t = s0.truth
        val safe = (0 until 81).first { !s0.cells[it].status.isResolved && !t.isSeal[it] }
        val seal = t.seals.first()
        return MoveEngine().play(
            s0, Action.Select(safe), Action.Digit(t.solution[safe]), Action.SealAt(seal),
            Action.ToggleNotes, Action.Select(safe + 1), Action.Digit(4),
        ).state.copy(elapsedMillis = 93_000, hintsUsed = 2)
    }

    @Test
    fun roundTripKeepsEverything() = runTest {
        val repo = SaveRepository(tmp.root)
        val state = midGame()
        repo.save(state)
        val loaded = repo.load()!!
        assertEquals(state.cells, loaded.cells)
        assertEquals(state.puzzle.solution, loaded.puzzle.solution)
        assertEquals(state.puzzle.seals, loaded.puzzle.seals)
        assertEquals(state.puzzle.usage, loaded.puzzle.usage)
        assertEquals(state.history, loaded.history)
        assertEquals(state.selected, loaded.selected)
        assertEquals(state.inputMode, loaded.inputMode)
        assertEquals(state.elapsedMillis, loaded.elapsedMillis)
        assertEquals(state.mistakes, loaded.mistakes)
        assertEquals(state.hintsUsed, loaded.hintsUsed)
        assertEquals(state.puzzle.difficulty, loaded.puzzle.difficulty)
        // Derived answer data is rebuilt, not stored.
        assertTrue(loaded.truth.clues.contentEquals(state.truth.clues))
        // Undo still works after a restart.
        val undone = MoveEngine().reduce(loaded, Action.Undo).state
        assertFalse(undone == loaded)
    }

    @Test
    fun corruptSaveIsIgnored() = runTest {
        File(tmp.root, "game.json").writeText("{ not json")
        assertNull(SaveRepository(tmp.root).load())
    }

    @Test
    fun clearRemovesSave() = runTest {
        val repo = SaveRepository(tmp.root)
        repo.save(midGame())
        assertTrue(repo.hasSave())
        repo.clear()
        assertFalse(repo.hasSave())
    }

    @Test
    fun noTempFileLeftBehind() {
        SaveRepository(tmp.root).saveBlocking(midGame())
        assertEquals(listOf("game.json"), tmp.root.list()!!.toList())
    }
}
