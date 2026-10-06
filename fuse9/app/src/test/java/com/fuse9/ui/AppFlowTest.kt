package com.fuse9.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.click
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.fuse9.MainActivity
import com.fuse9.game.PuzzleSource
import com.fuse9.puzzle.Difficulty
import com.fuse9.puzzle.Grid
import com.fuse9.puzzle.PuzzleGenerator
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Drives the real app: first launch lands on the teaching board, moves work, progress is kept. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-xhdpi")
class AppFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private val board = hasContentDescription("FUSE9 board", substring = true).or(hasContentDescription("row ", substring = true))

    private fun tapCell(cell: Int, long: Boolean = false) {
        compose.onNode(board).performTouchInput {
            val pad = width * 0.012f
            val cs = (width - pad * 2) / 9f
            val at = Offset(pad + (Grid.col(cell) + 0.5f) * cs, pad + (Grid.row(cell) + 0.5f) * cs)
            if (long) longClick(at) else click(at)
        }
        compose.waitForIdle()
    }

    @Test
    fun firstLaunchTeachesOnABoardAndKeepsProgress() {
        // Splash plays (~0.85 s) and hands over to the first board on its own.
        compose.waitUntil(10_000) { compose.onAllNodes(board).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("First board".uppercase()).assertExists()

        val puzzle = PuzzleGenerator.generate(PuzzleSource.TUTORIAL_SEED, Difficulty.EASY)
        val seal = puzzle.seals.first()
        tapCell(seal, long = true)
        compose.onNodeWithContentDescription("1 of 9 seals found").assertExists()

        val safe = (0 until 81).first { it !in puzzle.givens && it !in puzzle.seals }
        tapCell(safe)
        val digit = puzzle.solution[safe]
        compose.onNodeWithContentDescription("Digit $digit").performClick()
        compose.waitForIdle()
        compose.onNode(hasContentDescription("digit $digit, ${puzzle.truth.clues[safe]} seals adjacent", substring = true)).assertExists()

        compose.onNodeWithContentDescription("Back to menu").performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasContentDescription("Continue")).fetchSemanticsNodes().isNotEmpty() }
    }
}
