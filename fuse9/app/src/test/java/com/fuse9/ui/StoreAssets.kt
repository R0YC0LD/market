package com.fuse9.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.test.core.app.ApplicationProvider
import com.fuse9.FuseApp
import com.fuse9.game.Action
import com.fuse9.game.CellStatus
import com.fuse9.game.GameMode
import com.fuse9.game.GameState
import com.fuse9.game.MoveEngine
import com.fuse9.puzzle.Difficulty
import com.fuse9.puzzle.PuzzleGenerator
import com.fuse9.settings.AppLanguage
import com.fuse9.settings.ThemeChoice
import com.fuse9.ui.game.GameScreen
import com.fuse9.ui.game.GameViewModel
import com.fuse9.ui.game.ResultScreen
import com.fuse9.ui.game.StartRequest
import com.fuse9.ui.i18n.LocalStrings
import com.fuse9.ui.menu.LogoMark
import com.fuse9.ui.menu.RulesScreen
import com.fuse9.ui.theme.FuseText
import com.fuse9.ui.theme.FuseTheme
import com.fuse9.ui.theme.LocalPalette
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
 * Google Play store graphics, rendered from the real UI.
 * ./gradlew :app:testDebugUnitTest --tests '*StoreAssets*' -Dfuse9.store=/abs/out
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = FuseApp::class)
class StoreAssets {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val out = System.getProperty("fuse9.store").orEmpty()
    private val app get() = ApplicationProvider.getApplicationContext<FuseApp>().container

    @Before fun only() = Assume.assumeTrue(out.isNotBlank())

    private fun save(dir: String, name: String) {
        if (compose.mainClock.autoAdvance) compose.waitForIdle()
        val view = compose.activity.window.decorView
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bmp))
        File(out, dir).mkdirs()
        File(File(out, dir), "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test @Config(qualifiers = "w512dp-h512dp-mdpi")
    fun icon() {
        compose.setContent {
            FuseTheme(app.settings.settings.value.copy(theme = ThemeChoice.LIGHT)) {
                Box(Modifier.fillMaxSize().background(LocalPalette.current.page), contentAlignment = Alignment.Center) {
                    LogoMark(1f, Modifier.size(300.dp), weight = 5f)
                }
            }
        }
        save("", "icon-512")
    }

    private fun feature(lang: AppLanguage, dir: String) {
        compose.setContent {
            FuseTheme(app.settings.settings.value.copy(theme = ThemeChoice.LIGHT, language = lang)) {
                val p = LocalPalette.current
                Row(
                    Modifier.fillMaxSize().background(p.page).padding(horizontal = 96.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                ) {
                    LogoMark(1f, Modifier.size(190.dp), weight = 2.2f)
                    Spacer(Modifier.width(64.dp))
                    Column {
                        Text("FUSE9", style = FuseText.Title.copy(fontSize = 104.sp), color = p.ink)
                        Spacer(Modifier.height(4.dp))
                        Text(LocalStrings.current.tagline, style = FuseText.Item.copy(fontSize = 30.sp), color = p.inkSoft)
                    }
                }
            }
        }
        save(dir, "feature-graphic")
    }

    @Test @Config(qualifiers = "w1024dp-h500dp-mdpi") fun featureEn() = feature(AppLanguage.ENGLISH, "en-US")
    @Test @Config(qualifiers = "w1024dp-h500dp-mdpi") fun featureTr() = feature(AppLanguage.TURKISH, "tr-TR")

    private fun midGame(): GameState {
        val engine = MoveEngine()
        var s = GameState.new(PuzzleGenerator.generate(4242, Difficulty.HARD), GameMode.CLASSIC)
        val t = s.truth
        val hidden = (0 until 81).filter { s.cells[it].status == CellStatus.HIDDEN }
        for (c in hidden.filter { !t.isSeal[it] }.take(22)) s = engine.reduce(engine.reduce(s, Action.Select(c)).state, Action.Digit(t.solution[c])).state
        for (c in t.seals.take(3)) s = engine.reduce(s, Action.SealAt(c)).state
        val given = (0 until 81).first { s.cells[it].status == CellStatus.GIVEN && t.clues[it] > 0 }
        return engine.reduce(s, Action.Select(given)).state.copy(elapsedMillis = 257_000)
    }

    private fun solved(): GameState {
        val engine = MoveEngine()
        var s = GameState.new(PuzzleGenerator.generate(99, Difficulty.EXPERT), GameMode.CLASSIC)
        for (c in 0 until 81) {
            if (s.cells[c].status.isResolved) continue
            s = if (s.truth.isSeal[c]) engine.reduce(s, Action.SealAt(c)).state
            else engine.reduce(engine.reduce(s, Action.Select(c)).state, Action.Digit(s.truth.solution[c])).state
        }
        return s.copy(elapsedMillis = 734_000)
    }

    private fun phone(lang: AppLanguage, dir: String) {
        app.settings.update { it.copy(language = lang, theme = ThemeChoice.LIGHT) }
        try {
            val guided = GameViewModel(app)
            val playing = GameViewModel(app)
            app.saves.saveBlocking(midGame())
            playing.start(StartRequest.Continue)
            guided.start(StartRequest.Tutorial)
            var screen by androidx.compose.runtime.mutableIntStateOf(0)
            var dark by androidx.compose.runtime.mutableStateOf(false)
            val done = solved()
            compose.setContent {
                val st = app.settings.settings.value.copy(theme = if (dark) ThemeChoice.DARK else ThemeChoice.LIGHT)
                FuseTheme(st) {
                    when (screen) {
                        0 -> GameScreen(guided, st, showDebug = false) {}
                        1 -> GameScreen(playing, st, showDebug = false) {}
                        2 -> ResultScreen(done, 0, {}, {})
                        else -> RulesScreen {}
                    }
                }
            }
            compose.waitUntil(30_000) { guided.ui.value.game != null && playing.ui.value.game != null }
            // 1: the first, guided board
            guided.onCellTap(guided.ui.value.game!!.puzzle.givens.first())
            compose.mainClock.autoAdvance = false
            compose.mainClock.advanceTimeBy(400)
            save(dir, "1-first-board")
            // 2: mid-game with a hint explained (the hint target pulses, so pause the clock for it)
            compose.mainClock.autoAdvance = true
            screen = 1
            compose.waitForIdle()
            compose.mainClock.autoAdvance = false
            playing.onHint(); playing.onHint()
            compose.mainClock.advanceTimeBy(400)
            save(dir, "2-hint")
            // 3: same board in Graphite
            playing.onHint()
            compose.mainClock.autoAdvance = true
            dark = true
            compose.waitForIdle()
            compose.mainClock.advanceTimeBy(800)
            save(dir, "3-dark")
            dark = false
            screen = 2
            compose.waitForIdle()
            compose.mainClock.advanceTimeBy(3000)
            save(dir, "4-result")
            screen = 3
            compose.mainClock.autoAdvance = true
            compose.waitForIdle()
            save(dir, "5-rules")
        } finally {
            app.settings.update { it.copy(language = AppLanguage.SYSTEM, theme = ThemeChoice.SYSTEM) }
        }
    }
    @Test @Config(qualifiers = "w360dp-h640dp-xxhdpi") fun phoneEn() = phone(AppLanguage.ENGLISH, "en-US")
    @Test @Config(qualifiers = "w360dp-h640dp-xxhdpi") fun phoneTr() = phone(AppLanguage.TURKISH, "tr-TR")
}
