package com.fuse9.ui.game

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fuse9.BuildConfig
import com.fuse9.game.CellStatus
import com.fuse9.game.GameMode
import com.fuse9.game.GameState
import com.fuse9.settings.Settings
import com.fuse9.ui.board.Board
import com.fuse9.ui.common.FuseIcons
import com.fuse9.ui.common.IconButtonQuiet
import com.fuse9.ui.common.Toggle
import com.fuse9.ui.common.quietClick
import com.fuse9.ui.theme.FuseText
import com.fuse9.ui.theme.LocalPalette

@Composable
fun GameScreen(vm: GameViewModel, settings: Settings, onExit: () -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val p = LocalPalette.current
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.onBackground() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { vm.onForeground() }

    Column(
        Modifier.fillMaxSize().background(p.page).statusBarsPadding().navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val game = ui.game
        Header(vm, game, settings, ui.paused, onExit)
        // The board sits low, near the thumb: spare height goes mostly above it, a little below the caption.
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val fixed = TRACKER_HEIGHT + 10.dp + CAPTION_HEIGHT
            val side = minOf(maxWidth - 20.dp, maxHeight - fixed, 560.dp).coerceAtLeast(120.dp)
            val spare = (maxHeight - fixed - side).coerceAtLeast(0.dp)
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(spare * 0.7f))
                SealTracker(game)
                Spacer(Modifier.height(10.dp))
                Box(Modifier.size(side), contentAlignment = Alignment.Center) {
                    if (game != null) {
                        Board(
                            state = game, fx = vm.fx, overlay = ui.overlay, countStyle = settings.countStyle,
                            highlightSame = settings.highlightSameDigit,
                            onTap = vm::onCellTap, onLongPress = vm::onCellLongPress,
                            modifier = Modifier.fillMaxSize(),
                        )
                        if (ui.paused) PauseVeil(vm::resume)
                    } else if (ui.loading) {
                        Preparing()
                    }
                }
                Caption(ui.caption)
            }
        }
        if (game != null) Column(Modifier.widthIn(max = 560.dp)) {
            ToolRow(game.inputMode, ui.hintLevel, vm::onUndo, vm::onErase, vm::onNotes, vm::onSeal, vm::onHint)
            Spacer(Modifier.height(2.dp))
            DigitPad(game.inputMode, game.remaining(), ui.highlightDigit, vm::onDigit)
        }
        Spacer(Modifier.height(10.dp))
    }
    if (BuildConfig.DEBUG_TOOLS && ui.debug.open && ui.game != null) DebugPanel(vm, ui)
}

@Composable
private fun Header(vm: GameViewModel, game: GameState?, settings: Settings, paused: Boolean, onExit: () -> Unit) {
    val p = LocalPalette.current
    val clock by vm.clock.collectAsStateWithLifecycle()
    Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButtonQuiet(FuseIcons.Back, "Back to menu", onClick = onExit)
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            if (game != null) {
                Text(game.puzzle.difficulty.label, style = FuseText.Numeric, color = p.ink)
                val mode = when {
                    game.tutorial -> "First board"
                    game.mode == GameMode.CLASSIC -> null
                    else -> game.mode.label
                }
                if (mode != null) Text(mode.uppercase(), style = FuseText.Label, color = p.inkSoft)
            }
        }
        if (game != null) {
            StrikeMarks(game.mistakes, game.mode.mistakeLimit)
            Spacer(Modifier.width(14.dp))
            Row(
                Modifier.quietClick(label = if (paused) "Resume" else "Pause") { if (paused) vm.resume() else vm.pause() }.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (settings.showTimer) {
                    Text(formatTime(clock), style = FuseText.Numeric, color = p.ink)
                    Spacer(Modifier.width(6.dp))
                }
                Icon(if (paused) FuseIcons.Play else FuseIcons.Pause, null, tint = p.inkSoft, modifier = Modifier.size(18.dp))
            }
        }
        if (BuildConfig.DEBUG_TOOLS) IconButtonQuiet(FuseIcons.Bug, "Debug", tint = p.inkFaint, size = 40.dp) { vm.toggleDebug { it.copy(open = !it.open) } }
    }
}

/**
 * Nine slots, one per digit. A slot fills when the seal hiding that digit is found — the
 * at-a-glance answer to "which digits are already sealed?", which is half of FUSE9's logic.
 */
@Composable
private fun SealTracker(game: GameState?) {
    val p = LocalPalette.current
    val found = IntArray(10)
    if (game != null) for (c in game.puzzle.seals) {
        val st = game.cells[c].status
        if (st == CellStatus.SEALED) found[game.truth.solution[c]] = 1
        if (st == CellStatus.TRIPPED) found[game.truth.solution[c]] = 2
    }
    val count = found.count { it > 0 }
    Row(
        Modifier.height(TRACKER_HEIGHT).semantics { contentDescription = "$count of 9 seals found" },
        horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        for (d in 1..9) {
            Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                val state = found[d]
                Canvas(Modifier.size(20.dp)) {
                    val r = size.minDimension / 2 - 1.dp.toPx()
                    when (state) {
                        0 -> drawCircle(p.lineThin, r, style = Stroke(1.dp.toPx()))
                        1 -> drawCircle(p.seal, r, style = Stroke(1.4.dp.toPx()))
                        else -> {
                            drawArc(p.danger, -60f + 10f, 160f, false, style = Stroke(1.4.dp.toPx()), topLeft = Offset(center.x - r, center.y - r), size = androidx.compose.ui.geometry.Size(r * 2, r * 2))
                            drawArc(p.danger, 120f + 10f, 160f, false, style = Stroke(1.4.dp.toPx()), topLeft = Offset(center.x - r, center.y - r), size = androidx.compose.ui.geometry.Size(r * 2, r * 2))
                        }
                    }
                }
                Text(
                    "$d", style = FuseText.Small.copy(fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp)),
                    color = when (state) { 0 -> p.inkFaint.copy(alpha = 0.6f); 1 -> p.ink; else -> p.danger },
                )
            }
        }
    }
}

@Composable
private fun Caption(text: String?) {
    val p = LocalPalette.current
    Box(Modifier.fillMaxWidth().height(CAPTION_HEIGHT).padding(horizontal = 20.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
        AnimatedContent(text, transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) }, label = "caption") { t ->
            if (t != null) Text(
                t, style = FuseText.Body.copy(fontSize = 14.sp, lineHeight = 19.sp), color = p.inkSoft, textAlign = TextAlign.Center, maxLines = 3,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

@Composable
private fun PauseVeil(onResume: () -> Unit) {
    val p = LocalPalette.current
    Box(Modifier.fillMaxSize().quietClick(label = "Resume", onClick = onResume), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Paused", style = FuseText.Heading, color = p.ink)
            Spacer(Modifier.height(6.dp))
            Text("Tap to continue", style = FuseText.Small, color = p.inkSoft)
        }
    }
}

@Composable
fun Preparing() {
    val p = LocalPalette.current
    Text("Preparing a board…", style = FuseText.Small, color = p.inkSoft)
}

@Composable
private fun DebugPanel(vm: GameViewModel, ui: GameUi) {
    val p = LocalPalette.current
    val g = ui.game ?: return
    val pz = g.puzzle
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier.fillMaxWidth().height(340.dp).background(p.board, RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .padding(16.dp).verticalScroll(rememberScrollState()),
        ) {
            Text("Debug", style = FuseText.Heading, color = p.ink)
            Toggle("Show solution", ui.debug.solution) { v -> vm.toggleDebug { it.copy(solution = v) } }
            Toggle("Show seal locations", ui.debug.seals) { v -> vm.toggleDebug { it.copy(seals = v) } }
            Toggle("Show candidate map", ui.debug.candidates) { v -> vm.toggleDebug { it.copy(candidates = v) } }
            val lines = listOf(
                "id" to pz.id,
                "seed" to pz.seed.toString(),
                "generator" to "v${pz.generatorVersion}",
                "difficulty" to "${pz.difficulty.label}  score ${pz.score}  max level ${pz.maxLevel}",
                "givens" to pz.givens.size.toString(),
                "solver steps" to pz.solverSteps.toString(),
                "generation" to "${pz.generationMillis} ms",
                "seals" to pz.seals.joinToString(" ") { com.fuse9.puzzle.Grid.cellName(it) },
                "techniques" to pz.usage.entries.sortedBy { it.key.weight }.joinToString("\n") { "${it.key.name.lowercase()} ×${it.value}" },
            )
            for ((k, v) in lines) {
                Row(Modifier.padding(vertical = 3.dp)) {
                    Text(k, style = FuseText.Small, color = p.inkSoft, modifier = Modifier.width(96.dp))
                    Text(v, style = FuseText.Small, color = p.ink)
                }
            }
            Text(
                "Close", style = FuseText.Item, color = p.accent,
                modifier = Modifier.padding(top = 8.dp).quietClick { vm.toggleDebug { it.copy(open = false) } }.alpha(0.9f),
            )
        }
    }
}

private val TRACKER_HEIGHT = 26.dp
private val CAPTION_HEIGHT = 66.dp

fun formatTime(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
