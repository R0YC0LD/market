package com.fuse9.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fuse9.AppContainer
import com.fuse9.game.GameMode
import com.fuse9.game.PuzzleSource
import com.fuse9.ui.game.GameScreen
import com.fuse9.ui.game.GameViewModel
import com.fuse9.ui.game.ResultScreen
import com.fuse9.ui.game.StartRequest
import com.fuse9.ui.menu.MenuInfo
import com.fuse9.ui.menu.MenuScreen
import com.fuse9.ui.menu.ModesScreen
import com.fuse9.ui.menu.RulesScreen
import com.fuse9.ui.menu.SettingsScreen
import com.fuse9.ui.menu.Splash
import com.fuse9.ui.menu.StatsScreen
import com.fuse9.ui.theme.FuseTheme
import com.fuse9.ui.theme.LocalPalette
import java.time.LocalDate

enum class Screen { SPLASH, MENU, GAME, RESULT, MODES, STATS, SETTINGS, RULES }

@Composable
fun FuseRoot(app: AppContainer) {
    val settings by app.settings.settings.collectAsStateWithLifecycle()
    FuseTheme(settings) {
        val vm: GameViewModel = viewModel(factory = GameViewModel.Factory(app))
        var screen by rememberSaveable { mutableStateOf(Screen.SPLASH) }
        val stats by app.stats.stats.collectAsStateWithLifecycle()
        val ui by vm.ui.collectAsStateWithLifecycle()

        fun play(request: StartRequest) {
            vm.start(request)
            screen = Screen.GAME
        }

        LaunchedEffect(ui.finished) { if (ui.finished && screen == Screen.GAME) screen = Screen.RESULT }
        BackHandler(enabled = screen != Screen.MENU && screen != Screen.SPLASH) {
            if (screen == Screen.GAME) vm.pause()
            screen = Screen.MENU
        }

        Box(Modifier.fillMaxSize().background(LocalPalette.current.page)) {
            AnimatedContent(screen, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) }, label = "screen") { s ->
                when (s) {
                    Screen.SPLASH -> Splash {
                        // A new player goes straight onto a board; the board does the teaching.
                        if (!stats.tutorialDone && !app.saves.hasSave()) play(StartRequest.Tutorial) else screen = Screen.MENU
                    }
                    Screen.MENU -> {
                        val today = LocalDate.now()
                        val strings = com.fuse9.ui.i18n.LocalStrings.current
                        val saved by produceState<com.fuse9.game.GameState?>(null, screen) { value = app.saves.load() }
                        MenuScreen(
                            info = MenuInfo(
                                saved = saved,
                                dailyDone = today.toString() in stats.dailiesDone,
                                dailyLabel = "${strings.dayName(today.dayOfWeek)} · ${strings.difficulty(PuzzleSource.dailyDifficulty(today))}",
                                streak = app.stats.currentStreak(today),
                            ),
                            onContinue = { play(StartRequest.Continue) },
                            onNew = { play(StartRequest.Fresh(it, GameMode.CLASSIC)) },
                            onDaily = { play(StartRequest.Daily) },
                            onModes = { screen = Screen.MODES },
                            onStats = { screen = Screen.STATS },
                            onSettings = { screen = Screen.SETTINGS },
                            onRules = { screen = Screen.RULES },
                        )
                    }
                    Screen.GAME -> GameScreen(vm, settings) { vm.pause(); screen = Screen.MENU }
                    Screen.RESULT -> {
                        val g = ui.game
                        if (g == null) screen = Screen.MENU else ResultScreen(
                            g, app.stats.currentStreak(),
                            onNext = {
                                val mode = if (g.mode == GameMode.DAILY || g.tutorial) GameMode.CLASSIC else g.mode
                                play(StartRequest.Fresh(g.puzzle.difficulty, mode))
                            },
                            onMenu = { screen = Screen.MENU },
                        )
                    }
                    Screen.MODES -> ModesScreen(onBack = { screen = Screen.MENU }) { mode, d -> play(StartRequest.Fresh(d, mode)) }
                    Screen.STATS -> StatsScreen(stats, app.stats.currentStreak()) { screen = Screen.MENU }
                    Screen.SETTINGS -> SettingsScreen(settings, app.settings::update) { screen = Screen.MENU }
                    Screen.RULES -> RulesScreen { screen = Screen.MENU }
                }
            }
        }
    }
}
