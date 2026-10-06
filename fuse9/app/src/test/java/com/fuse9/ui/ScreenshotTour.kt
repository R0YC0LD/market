package com.fuse9.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.fuse9.FuseApp
import com.fuse9.game.Action
import com.fuse9.game.CellStatus
import com.fuse9.game.GameMode
import com.fuse9.game.GameState
import com.fuse9.game.MoveEngine
import com.fuse9.puzzle.Difficulty
import com.fuse9.puzzle.PuzzleGenerator
import com.fuse9.settings.ThemeChoice
import com.fuse9.ui.game.GameScreen
import com.fuse9.ui.game.GameViewModel
import com.fuse9.ui.game.ResultScreen
import com.fuse9.ui.menu.MenuInfo
import com.fuse9.ui.menu.MenuScreen
import com.fuse9.ui.menu.RulesScreen
import com.fuse9.ui.menu.SettingsScreen
import com.fuse9.ui.menu.Splash
import com.fuse9.ui.theme.FuseTheme
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders key screens to PNG for visual review (no device needed).
 * Run: ./gradlew :app:testDebugUnitTest --tests '*ScreenshotTour*' -Dfuse9.screens=/abs/out/dir
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-xhdpi", application = FuseApp::class)
class ScreenshotTour {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val out = System.getProperty("fuse9.screens").orEmpty()
    private val app get() = ApplicationProvider.getApplicationContext<FuseApp>().container

    @Before fun only() = Assume.assumeTrue(out.isNotBlank())

    private fun shot(name: String) {
        if (compose.mainClock.autoAdvance) compose.waitForIdle()
        // Software-draw the window: avoids PixelCopy, which Robolectric can't always service.
        val view = compose.activity.window.decorView
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bmp))
        File(out).mkdirs()
        File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun midGame(): GameState {
        val engine = MoveEngine()
        var s = GameState.new(PuzzleGenerator.generate(4242, Difficulty.HARD), GameMode.CLASSIC)
        val t = s.truth
        val hidden = (0 until 81).filter { s.cells[it].status == CellStatus.HIDDEN }
        // A plausible mid-solve: some digits, two defused seals, one trip, one wrong digit, notes.
        for (c in hidden.filter { !t.isSeal[it] }.take(22)) s = engine.reduce(engine.reduce(s, Action.Select(c)).state, Action.Digit(t.solution[c])).state
        for (c in t.seals.take(2)) s = engine.reduce(s, Action.SealAt(c)).state
        s = engine.reduce(engine.reduce(s, Action.Select(t.seals[3])).state, Action.Digit(1)).state
        val open = (0 until 81).filter { s.cells[it].status == CellStatus.HIDDEN && !t.isSeal[it] }
        s = engine.reduce(engine.reduce(s, Action.Select(open[0])).state, Action.Digit((t.solution[open[0]] % 9) + 1)).state
        s = engine.reduce(s, Action.ToggleNotes).state
        for (c in open.drop(1).take(3)) {
            s = engine.reduce(s, Action.Select(c)).state
            for (d in listOf(t.solution[c], (t.solution[c] + 3) % 9 + 1)) s = engine.reduce(s, Action.Digit(d)).state
        }
        s = engine.reduce(s, Action.Seal).state // suspect mark on the last
        s = engine.reduce(s, Action.ToggleNotes).state
        val given = (0 until 81).first { s.cells[it].status == CellStatus.GIVEN && t.clues[it] > 0 }
        return engine.reduce(s, Action.Select(given)).state.copy(elapsedMillis = 257_000)
    }

    @Test fun splash() {
        compose.mainClock.autoAdvance = false
        compose.setContent { FuseTheme(app.settings.settings.value) { Splash {} } }
        compose.mainClock.advanceTimeBy(560)
        shot("01_splash")
    }

    @Test fun menu() {
        compose.setContent {
            FuseTheme(app.settings.settings.value) {
                MenuScreen(MenuInfo(midGame(), false, "Tuesday · Medium", 4), {}, {}, {}, {}, {}, {}, {})
            }
        }
        shot("02_menu")
    }

    private fun game(name: String, dark: Boolean, hint: Int = 0) {
        if (dark) app.settings.update { it.copy(theme = ThemeChoice.DARK) }
        val vm = GameViewModel(app)
        val state = midGame()
        app.saves.saveBlocking(state)
        vm.start(com.fuse9.ui.game.StartRequest.Continue)
        compose.setContent { FuseTheme(app.settings.settings.value) { GameScreen(vm, app.settings.settings.value) {} } }
        compose.waitUntil(5_000) { vm.ui.value.game != null }
        // The hint target pulses forever; drive the clock by hand so the test can settle.
        compose.mainClock.autoAdvance = false
        repeat(hint) { vm.onHint() }
        compose.mainClock.advanceTimeBy(400)
        shot(name)
        if (dark) app.settings.update { it.copy(theme = ThemeChoice.SYSTEM) }
    }

    @Test fun gameLight() = game("03_game_light", dark = false)
    @Test fun gameDark() = game("04_game_dark", dark = true)
    @Test fun gameHint() = game("05_game_hint", dark = false, hint = 2)

    @Test fun result() {
        val engine = MoveEngine()
        var s = GameState.new(PuzzleGenerator.generate(99, Difficulty.MEDIUM), GameMode.CLASSIC)
        for (c in 0 until 81) {
            if (s.cells[c].status.isResolved) continue
            s = if (s.truth.isSeal[c]) engine.reduce(s, Action.SealAt(c)).state
            else engine.reduce(engine.reduce(s, Action.Select(c)).state, Action.Digit(s.truth.solution[c])).state
        }
        val done = s.copy(elapsedMillis = 412_000, mistakes = 1)
        compose.setContent { FuseTheme(app.settings.settings.value) { ResultScreen(done, 0, {}, {}) } }
        compose.mainClock.advanceTimeBy(3000)
        shot("06_result")
    }

    @Test fun rules() {
        compose.setContent { FuseTheme(app.settings.settings.value) { RulesScreen {} } }
        shot("07_rules")
    }

    @Test fun settings() {
        compose.setContent { FuseTheme(app.settings.settings.value) { SettingsScreen(app.settings.settings.value, {}, {}) } }
        shot("08_settings")
    }

    @Test fun tutorialBoard() {
        val vm = GameViewModel(app)
        vm.start(com.fuse9.ui.game.StartRequest.Tutorial)
        compose.setContent { FuseTheme(app.settings.settings.value) { GameScreen(vm, app.settings.settings.value) {} } }
        compose.waitUntil(5_000) { vm.ui.value.game != null }
        shot("09_tutorial_start")
        val g = vm.ui.value.game!!
        vm.onCellTap(g.puzzle.givens.first())
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(300)
        shot("10_tutorial_step2")
    }

    /** Frames from the middle of the defuse, trip and win animations. */
    @Test fun motionFrames() {
        val vm = GameViewModel(app)
        app.saves.saveBlocking(midGame())
        vm.start(com.fuse9.ui.game.StartRequest.Continue)
        compose.setContent { FuseTheme(app.settings.settings.value) { GameScreen(vm, app.settings.settings.value) {} } }
        compose.waitUntil(5_000) { vm.ui.value.game != null }
        compose.mainClock.autoAdvance = false
        val g = vm.ui.value.game!!
        val seal = g.puzzle.seals.first { !g.cells[it].status.isResolved }
        vm.onCellLongPress(seal)
        compose.mainClock.advanceTimeBy(160)
        shot("11_defuse_160ms")
        val other = g.puzzle.seals.last { !g.cells[it].status.isResolved && it != seal }
        vm.onCellTap(other); vm.onDigit(1)
        compose.mainClock.advanceTimeBy(200)
        shot("12_trip_200ms")
    }
}
