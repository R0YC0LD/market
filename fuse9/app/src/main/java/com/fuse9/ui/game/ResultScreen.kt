package com.fuse9.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.fuse9.game.CellStatus
import com.fuse9.game.GameMode
import com.fuse9.game.GameState
import com.fuse9.game.Scoring
import com.fuse9.game.Verdict
import com.fuse9.puzzle.Grid
import com.fuse9.ui.common.Hairline
import com.fuse9.ui.common.MenuItem
import com.fuse9.ui.theme.FuseText
import com.fuse9.ui.theme.LocalPalette

/**
 * The board tells the story: a miniature replays the solve in the order it happened, then the
 * verdict settles underneath. No banners, no confetti.
 */
@Composable
fun ResultScreen(state: GameState, dailyStreak: Int, onNext: () -> Unit, onMenu: () -> Unit) {
    val p = LocalPalette.current
    val summary = remember(state) { Scoring.summarize(state) }
    val replay = remember { Animatable(0f) }
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(state) {
        replay.animateTo(1f, tween(1700, easing = LinearEasing))
        reveal.animateTo(1f, tween(500))
    }
    Column(
        Modifier.fillMaxSize().background(p.page).statusBarsPadding().navigationBarsPadding().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(36.dp))
        Text("FUSE9", style = FuseText.Label, color = p.inkSoft)
        Spacer(Modifier.height(22.dp))
        MiniReplay(state, replay.value)
        Spacer(Modifier.height(28.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.semantics { contentDescription = "${summary.verdict.word}. ${summary.verdict.line}" }) {
            Text(summary.verdict.word, style = FuseText.Word, color = if (summary.verdict == Verdict.UNSOLVED) p.inkSoft else p.ink, modifier = Modifier.padding(bottom = 2.dp))
            Text(summary.verdict.line, style = FuseText.Small, color = p.inkSoft)
        }
        Spacer(Modifier.height(16.dp))
        Marks(summary.marks, reveal.value)
        Spacer(Modifier.height(22.dp))
        Text(formatTime(summary.millis), style = FuseText.Heading, color = p.ink)
        Spacer(Modifier.height(6.dp))
        val parts = buildList {
            add("${state.sealsFound} of 9 seals")
            add(if (summary.mistakes == 1) "1 strike" else "${summary.mistakes} strikes")
            if (summary.hints > 0) add(if (summary.hints == 1) "1 hint" else "${summary.hints} hints")
            if (summary.ripples > 0) add("${summary.ripples} ripples")
        }
        Text(parts.joinToString("  ·  "), style = FuseText.Small, color = p.inkSoft)
        if (state.mode == GameMode.DAILY && dailyStreak > 0) {
            Spacer(Modifier.height(6.dp))
            Text("Daily streak  $dailyStreak", style = FuseText.Small, color = p.accent)
        }
        Spacer(Modifier.weight(1f))
        Hairline()
        Spacer(Modifier.height(8.dp))
        MenuItem(if (state.mode == GameMode.DAILY) "Another board" else "Next board", emphasis = true, onClick = onNext)
        MenuItem("Menu", onClick = onMenu)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MiniReplay(state: GameState, t: Float) {
    val p = LocalPalette.current
    val order = remember(state) {
        val trail = state.trail.distinct().filter { state.cells[it].status.isResolved }
        IntArray(Grid.CELLS) { -1 }.also { arr -> trail.forEachIndexed { i, c -> arr[c] = i } } to trail.size.coerceAtLeast(1)
    }
    Canvas(Modifier.size(196.dp)) {
        val cs = size.width / 9f
        val dp = density
        drawRoundRect(p.board, cornerRadius = CornerRadius(6 * dp, 6 * dp))
        for (c in 0 until Grid.CELLS) {
            val o = Offset(Grid.col(c) * cs, Grid.row(c) * cs)
            val cell = state.cells[c]
            val idx = order.first[c]
            val shown = when {
                cell.status == CellStatus.GIVEN -> 1f
                idx >= 0 -> ((t * (order.second + 3) - idx) / 3f).coerceIn(0f, 1f)
                else -> 0f
            }
            val inset = 1.2f * dp
            val fill = if (cell.status == CellStatus.GIVEN) p.tileSafe else lerp(p.tile, p.board, shown)
            drawRoundRect(fill, o + Offset(inset, inset), Size(cs - inset * 2, cs - inset * 2), CornerRadius(2 * dp, 2 * dp))
            val center = o + Offset(cs / 2, cs / 2)
            when (cell.status) {
                CellStatus.SEALED -> drawCircle(p.seal.copy(alpha = shown), cs * 0.3f, center, style = Stroke(1.2f * dp))
                CellStatus.TRIPPED -> drawCircle(p.danger.copy(alpha = shown), cs * 0.3f, center, style = Stroke(1.2f * dp))
                CellStatus.SOLVED -> drawCircle(p.accent.copy(alpha = 0.75f * shown), cs * 0.12f, center)
                CellStatus.GIVEN -> drawCircle(p.inkSoft, cs * 0.09f, center)
                CellStatus.HIDDEN -> if (state.truth.isSeal[c]) drawCircle(p.inkFaint, cs * 0.3f, center, style = Stroke(1f * dp))
            }
        }
        for (i in listOf(3, 6)) {
            drawLine(p.lineThick.copy(alpha = 0.5f), Offset(i * cs, 0f), Offset(i * cs, size.height), 1.2f * dp)
            drawLine(p.lineThick.copy(alpha = 0.5f), Offset(0f, i * cs), Offset(size.width, i * cs), 1.2f * dp)
        }
    }
}

@Composable
private fun Marks(n: Int, t: Float) {
    val p = LocalPalette.current
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.semantics { contentDescription = "$n of 5 marks" }) {
        for (i in 0 until 5) {
            val on = i < n
            val local = ((t * 5f) - i).coerceIn(0f, 1f)
            Canvas(Modifier.size(16.dp)) {
                val r = size.minDimension / 2 - 2.dp.toPx()
                drawCircle(if (on) p.success else p.lineThin, r, style = Stroke(1.4.dp.toPx()))
                if (on) drawCircle(p.success.copy(alpha = local), r * 0.42f)
            }
        }
    }
}
