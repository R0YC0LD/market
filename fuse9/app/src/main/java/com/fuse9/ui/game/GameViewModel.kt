package com.fuse9.ui.game

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fuse9.AppContainer
import com.fuse9.game.Action
import com.fuse9.game.GameEvent
import com.fuse9.game.GameMode
import com.fuse9.game.GameState
import com.fuse9.game.GameStatus
import com.fuse9.game.Hint
import com.fuse9.game.HintAction
import com.fuse9.game.HintEngine
import com.fuse9.game.MoveEngine
import com.fuse9.game.TutorialGuide
import com.fuse9.puzzle.Difficulty
import com.fuse9.puzzle.Grid
import com.fuse9.puzzle.Techniques
import com.fuse9.ui.board.BoardFx
import com.fuse9.ui.board.BoardOverlay
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface StartRequest {
    data object Continue : StartRequest
    data class Fresh(val difficulty: Difficulty, val mode: GameMode) : StartRequest
    data object Daily : StartRequest
    data object Tutorial : StartRequest
    data class Seeded(val seed: Long, val difficulty: Difficulty) : StartRequest
}

/** What the line under the board says; worded by the UI in the player's language. */
sealed interface Caption {
    data class LookHere(val look: com.fuse9.game.Look) : Caption
    data class Because(val reasons: List<com.fuse9.game.Reason>) : Caption
    data class Guide(val line: TutorialGuide.Line) : Caption
}

data class DebugUi(val open: Boolean = false, val solution: Boolean = false, val seals: Boolean = false, val candidates: Boolean = false)

data class GameUi(
    val game: GameState? = null,
    val loading: Boolean = true,
    val paused: Boolean = false,
    val hint: Hint? = null,
    val hintLevel: Int = 0,
    val caption: Caption? = null,
    val captionCells: Set<Int> = emptySet(),
    val captionTarget: Int = -1,
    val highlightDigit: Int = 0,
    val finished: Boolean = false,
    val debug: DebugUi = DebugUi(),
) {
    val overlay: BoardOverlay
        get() {
            val g = game
            val hintRegion = if (hintLevel == 1) hint?.region?.toSet() ?: emptySet() else emptySet()
            val hintFocus = if (hintLevel >= 2) hint?.focus?.toSet() ?: emptySet() else emptySet()
            val target = if (hintLevel >= 2) hint?.cell ?: -1 else captionTarget
            return BoardOverlay(
                region = hintRegion,
                focus = hintFocus + captionCells,
                target = target,
                highlightDigit = highlightDigit,
                paused = paused,
                showSolution = debug.solution,
                showSeals = debug.seals,
                candidates = if (debug.candidates && g != null) candidateMap(g) else null,
            )
        }

    private fun candidateMap(g: GameState): IntArray {
        val k = HintEngine.visibleKnowledge(g)
        Techniques.eliminate(k)
        return IntArray(Grid.CELLS) { if (k.resolved[it]) 0 else k.cand[it] }
    }
}

/**
 * Owns one board in play: routes input through the pure [MoveEngine], fans events out to
 * animation and feedback, keeps the clock, saves, and records the result. Rules, hints and
 * generation all live outside it.
 */
class GameViewModel(private val app: AppContainer) : ViewModel() {
    private val _ui = MutableStateFlow(GameUi())
    val ui: StateFlow<GameUi> = _ui.asStateFlow()
    private val _clock = MutableStateFlow(0L)
    /** Elapsed play time, separate from [ui] so the ticking clock never recomposes the board. */
    val clock: StateFlow<Long> = _clock.asStateFlow()

    val fx = BoardFx()
    private val feedback = Feedback(app.audio, app.haptics, viewModelScope)
    private var guide: TutorialGuide? = null
    private var engine = MoveEngine(app.settings.settings.value.autoCleanNotes)
    private var saveJob: Job? = null
    private var foreground = true
    private var lastTick = SystemClock.elapsedRealtime()

    init {
        viewModelScope.launch {
            app.settings.settings.collect { engine = MoveEngine(it.autoCleanNotes) }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(250)
                val now = SystemClock.elapsedRealtime()
                val dt = now - lastTick
                lastTick = now
                val s = _ui.value
                if (foreground && !s.paused && !s.loading && s.game?.status == GameStatus.PLAYING) _clock.value += dt
            }
        }
    }

    fun start(request: StartRequest) {
        _ui.value = GameUi(loading = true)
        fx.clear()
        viewModelScope.launch {
            val state: GameState? = when (request) {
                StartRequest.Continue -> app.saves.load()
                is StartRequest.Fresh -> GameState.new(app.puzzles.next(request.difficulty), request.mode)
                StartRequest.Daily -> GameState.new(app.puzzles.daily(), GameMode.DAILY)
                StartRequest.Tutorial -> GameState.new(app.puzzles.tutorial(), GameMode.ZEN, tutorial = true)
                is StartRequest.Seeded -> GameState.new(app.puzzles.generate(request.seed, request.difficulty), GameMode.CLASSIC)
            }
            if (state == null) {
                _ui.value = GameUi(loading = false)
                return@launch
            }
            guide = if (state.tutorial && !app.stats.stats.value.tutorialDone) TutorialGuide() else null
            _clock.value = state.elapsedMillis
            lastTick = SystemClock.elapsedRealtime()
            _ui.value = GameUi(game = state, loading = false, finished = state.isOver)
            refreshGuide()
            app.puzzles.prefetch(state.puzzle.difficulty)
        }
    }

    // ---- Input ------------------------------------------------------------------------------

    fun onCellTap(cell: Int) {
        val s = _ui.value
        if (s.paused) { resume(); return }
        val g = s.game ?: return
        if (g.isOver) return
        _ui.update { it.copy(highlightDigit = 0) }
        dispatch(Action.Select(cell))
    }

    fun onCellLongPress(cell: Int) {
        if (_ui.value.paused) return
        dispatch(Action.SealAt(cell))
    }

    fun onDigit(digit: Int) {
        val s = _ui.value
        val g = s.game ?: return
        if (s.paused || g.isOver) return
        val sel = g.selected
        if (sel < 0 || g.cells[sel].status.isResolved) {
            // No open target: the pad becomes a lens on that digit across the board.
            _ui.update { it.copy(highlightDigit = if (it.highlightDigit == digit) 0 else digit) }
            feedback.tap()
            return
        }
        dispatch(Action.Digit(digit))
    }

    fun onSeal() = dispatch(Action.Seal)
    fun onNotes() = dispatch(Action.ToggleNotes)
    fun onUndo() = dispatch(Action.Undo)
    fun onErase() = dispatch(Action.Erase)

    /** Look → why → do. Each press goes one level deeper; a hint is counted once. */
    fun onHint() {
        val s = _ui.value
        val g = s.game ?: return
        if (s.paused || g.isOver) return
        val current = s.hint
        if (current == null) {
            val hint = HintEngine.find(g) ?: return
            feedback.hint()
            _ui.update { it.copy(hint = hint, hintLevel = 1, caption = Caption.LookHere(hint.look), captionCells = emptySet(), captionTarget = -1, game = g.copy(hintsUsed = g.hintsUsed + 1)) }
            fx.ambient = true
            return
        }
        when (s.hintLevel) {
            1 -> {
                feedback.hint()
                _ui.update { it.copy(hintLevel = 2, caption = Caption.Because(current.reasons)) }
            }
            else -> {
                fx.ambient = false
                _ui.update { it.copy(hint = null, hintLevel = 0, caption = null) }
                if (current.action == HintAction.SEAL) dispatch(Action.SealAt(current.cell))
                else { dispatch(Action.Select(current.cell)); dispatch(Action.Digit(current.digit)) }
            }
        }
    }

    fun pause() {
        val g = _ui.value.game ?: return
        if (g.isOver) return
        _ui.update { it.copy(paused = true) }
        saveNow()
    }

    fun resume() {
        lastTick = SystemClock.elapsedRealtime()
        _ui.update { it.copy(paused = false) }
    }

    fun onBackground() {
        foreground = false
        pause()
    }

    fun onForeground() {
        foreground = true
        lastTick = SystemClock.elapsedRealtime()
    }

    fun toggleDebug(transform: (DebugUi) -> DebugUi) = _ui.update { it.copy(debug = transform(it.debug)) }

    // ---- Core loop --------------------------------------------------------------------------

    private fun dispatch(action: Action) {
        val before = _ui.value.game ?: return
        val outcome = engine.reduce(before, action)
        val next = outcome.state.copy(elapsedMillis = _clock.value)
        val events = outcome.events
        val moved = events.any { it !is GameEvent.Selected && it !is GameEvent.Rejected && it !is GameEvent.ModeChanged }
        _ui.update {
            it.copy(
                game = next,
                hint = if (moved) null else it.hint,
                hintLevel = if (moved) 0 else it.hintLevel,
                caption = if (moved) null else it.caption,
            )
        }
        if (moved) fx.ambient = false
        for (e in events) fx.onEvent(e, next)
        feedback.on(events)
        guide?.let { g -> events.forEach(g::onEvent) }
        if (moved || guide != null) refreshGuide()
        if (events.any { it is GameEvent.Won || it is GameEvent.Lost }) finish(next) else if (moved) scheduleSave()
    }

    private fun refreshGuide() {
        val g = guide ?: return
        val state = _ui.value.game ?: return
        if (_ui.value.hint != null) return
        val line = g.line(state)
        _ui.update { it.copy(caption = line?.let { l -> Caption.Guide(l) }, captionCells = line?.cells ?: emptySet(), captionTarget = line?.target ?: -1) }
        if (g.finished) {
            app.stats.markTutorialDone()
            if (g.step == TutorialGuide.Step.DONE) viewModelScope.launch {
                delay(4500)
                g.dismissDone()
                _ui.update { if (line != null && it.caption == Caption.Guide(line)) it.copy(caption = null, captionCells = emptySet()) else it }
            }
        }
    }

    private fun finish(state: GameState) {
        saveJob?.cancel()
        viewModelScope.launch {
            app.saves.clear()
            if (!state.tutorial || state.status == GameStatus.WON) app.stats.record(state)
            delay(if (state.status == GameStatus.WON) 2300 else 1500)
            _ui.update { it.copy(finished = true) }
        }
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(350)
            _ui.value.game?.let { if (!it.isOver) app.saves.save(it.copy(elapsedMillis = _clock.value)) }
        }
    }

    private fun saveNow() {
        saveJob?.cancel()
        _ui.value.game?.let { if (!it.isOver) app.saves.saveBlocking(it.copy(elapsedMillis = _clock.value)) }
    }

    override fun onCleared() {
        saveNow()
        super.onCleared()
    }

    class Factory(private val app: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = GameViewModel(app) as T
    }
}
