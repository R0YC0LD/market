package com.fuse9.ui.board

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.fuse9.game.CellStatus
import com.fuse9.game.GameState
import com.fuse9.game.GameStatus
import com.fuse9.puzzle.Bits
import com.fuse9.puzzle.Grid
import com.fuse9.settings.CountStyle
import com.fuse9.ui.theme.FuseFonts
import com.fuse9.ui.theme.FusePalette
import com.fuse9.ui.theme.LocalPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Things drawn over the board that are not game state: hints, guide, debug views. */
data class BoardOverlay(
    val region: Set<Int> = emptySet(),
    val focus: Set<Int> = emptySet(),
    val target: Int = -1,
    val highlightDigit: Int = 0,
    val paused: Boolean = false,
    val showSolution: Boolean = false,
    val showSeals: Boolean = false,
    val candidates: IntArray? = null,
)

/** Pre-measured digits so a frame never measures text. */
private class Glyphs(val big: Array<TextLayoutResult>, val player: Array<TextLayoutResult>, val seal: Array<TextLayoutResult>, val note: Array<TextLayoutResult>, val count: Array<TextLayoutResult>, val bigPx: Float, val sealPx: Float, val notePx: Float, val countPx: Float)

private fun measure(tm: TextMeasurer, density: Density, px: Float, weight: Int): Pair<Array<TextLayoutResult>, Float> {
    val sp = with(density) { (px / fontScale).toSp() }
    val style = TextStyle(fontFamily = FuseFonts.Sans, fontWeight = FontWeight(weight), fontSize = sp, lineHeight = sp * 1.1f)
    return Array(10) { tm.measure(it.toString(), style) } to px
}

private class GlyphCache {
    private var glyphs: Glyphs? = null
    private var cell = 0f

    fun get(tm: TextMeasurer, density: Density, cs: Float): Glyphs {
        glyphs?.let { if (cell == cs) return it }
        val (big, bigPx) = measure(tm, density, cs * 0.56f, 600)
        val (player, _) = measure(tm, density, cs * 0.56f, 500)
        val (seal, sealPx) = measure(tm, density, cs * 0.36f, 600)
        val (note, notePx) = measure(tm, density, cs * 0.21f, 600)
        val (count, countPx) = measure(tm, density, cs * 0.21f, 700)
        cell = cs
        return Glyphs(big, player, seal, note, count, bigPx, sealPx, notePx, countPx).also { glyphs = it }
    }
}

/** Per-frame animation channels, reused between frames to avoid allocation. */
private class Channels {
    val digitScale = FloatArray(81)
    val digitAlpha = FloatArray(81)
    val tint = FloatArray(81)
    val win = FloatArray(81)
    val ripple = FloatArray(81)
    val undo = FloatArray(81)
    val note = FloatArray(81)
    val defuse = FloatArray(81)
    val trip = FloatArray(81)
    val wrong = FloatArray(81)
    val wrongDigit = IntArray(81)
    val wrongSeal = FloatArray(81)
    val winSeal = FloatArray(81)
    val loss = FloatArray(81)
    var breathe = 0f
    val boxTrace = FloatArray(9) { -1f }

    fun reset() {
        digitScale.fill(1f); digitAlpha.fill(1f); tint.fill(0f); win.fill(0f); ripple.fill(-1f); undo.fill(-1f)
        note.fill(-1f); defuse.fill(-1f); trip.fill(-1f); wrong.fill(-1f); wrongSeal.fill(-1f); winSeal.fill(-1f); loss.fill(-1f)
        breathe = 0f; boxTrace.fill(-1f)
    }
}

@Composable
fun Board(
    state: GameState,
    fx: BoardFx,
    overlay: BoardOverlay,
    countStyle: CountStyle,
    highlightSame: Boolean,
    onTap: (Int) -> Unit,
    onLongPress: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val tm = rememberTextMeasurer(cacheSize = 64)
    val density = LocalDensity.current
    val tap by rememberUpdatedState(onTap)
    val press by rememberUpdatedState(onLongPress)
    val channels = remember { Channels() }
    val glyphCache = remember(density) { GlyphCache() }
    LaunchedEffect(fx) { fx.run() }

    // A defusal is final, so a resting thumb shouldn't trigger one: hold a little longer than usual.
    val base = LocalViewConfiguration.current
    val deliberate = remember(base) { object : ViewConfiguration by base { override val longPressTimeoutMillis: Long get() = LONG_PRESS_MS } }
    CompositionLocalProvider(LocalViewConfiguration provides deliberate) { BoardSurface(state, fx, overlay, countStyle, highlightSame, tap, press, channels, glyphCache, tm, density, palette, modifier) }
}

@Composable
private fun BoardSurface(
    state: GameState, fx: BoardFx, overlay: BoardOverlay, countStyle: CountStyle, highlightSame: Boolean,
    tap: (Int) -> Unit, press: (Int) -> Unit, channels: Channels, glyphCache: GlyphCache, tm: TextMeasurer,
    density: Density, palette: FusePalette, modifier: Modifier,
) {
    Spacer(
        modifier
            .aspectRatio(1f)
            .semantics { contentDescription = describe(state) }
            .pointerInput(Unit) {
                fun cellAt(o: Offset): Int? {
                    val pad = size.width * PAD
                    val cs = (size.width - pad * 2) / 9f
                    val c = ((o.x - pad) / cs).toInt()
                    val r = ((o.y - pad) / cs).toInt()
                    return if (o.x >= pad && o.y >= pad && r in 0..8 && c in 0..8) Grid.cell(r, c) else null
                }
                detectTapGestures(
                    onTap = { o -> cellAt(o)?.let { tap(it) } },
                    onLongPress = { o -> cellAt(o)?.let { press(it) } },
                )
            }
            .drawBehind {
                fx.frame.longValue // subscribe: redraw on every animation frame
                val pad = size.width * PAD
                val cs = (size.width - pad * 2) / 9f
                val glyphs = glyphCache.get(tm, density, cs)
                collect(fx, channels, System.nanoTime())
                val breathe = 1f + 0.012f * Ease.bump(channels.breathe)
                scale(breathe) {
                    drawBoard(state, overlay, palette, glyphs, channels, fx, pad, cs, countStyle, highlightSame)
                }
            },
    )
}

private const val PAD = 0.012f
private const val LONG_PRESS_MS = 520L

private fun describe(state: GameState): String {
    val c = state.selected
    if (c < 0) return "FUSE9 board, ${state.sealsFound} of 9 seals found"
    val cell = state.cells[c]
    val where = "row ${Grid.row(c) + 1}, column ${Grid.col(c) + 1}"
    return when (cell.status) {
        CellStatus.HIDDEN -> "$where, hidden" + (if (cell.safe) ", proven safe" else "") + (if (cell.suspect) ", marked suspect" else "")
        CellStatus.SEALED -> "$where, defused seal, digit ${state.digitAt(c)}"
        CellStatus.TRIPPED -> "$where, tripped seal, digit ${state.digitAt(c)}"
        else -> "$where, digit ${state.digitAt(c)}, ${state.countAt(c)} seals adjacent"
    }
}

private fun collect(fx: BoardFx, ch: Channels, now: Long) {
    ch.reset()
    for (f in fx.effects) {
        val p = f.progress(now)
        if (p < 0f) {
            // Hide a reveal until its moment arrives.
            if (f.kind == FxKind.PLACE) ch.digitAlpha[f.cell] = 0f
            if (f.kind == FxKind.RIPPLE) ch.ripple[f.cell] = 0f
            continue
        }
        if (p >= 1f) continue
        val c = f.cell
        when (f.kind) {
            FxKind.PLACE -> {
                ch.digitScale[c] = 0.8f + 0.2f * Ease.outBack(p)
                ch.digitAlpha[c] = Ease.span(p, 0f, 0.4f)
            }
            FxKind.PULSE -> ch.tint[c] = maxOf(ch.tint[c], 0.55f * Ease.bump(p))
            FxKind.SWEEP -> ch.tint[c] = maxOf(ch.tint[c], Ease.bump(p))
            FxKind.BOX_TRACE -> ch.boxTrace[f.data - 18] = p
            FxKind.WRONG -> { ch.wrong[c] = p; ch.wrongDigit[c] = f.data }
            FxKind.WRONG_SEAL -> ch.wrongSeal[c] = p
            FxKind.DEFUSE -> ch.defuse[c] = p
            FxKind.TRIP -> ch.trip[c] = p
            FxKind.RIPPLE -> ch.ripple[c] = p
            FxKind.UNDO -> ch.undo[c] = p
            FxKind.NOTE -> ch.note[c] = p
            FxKind.WIN_WAVE -> ch.win[c] = Ease.bump(p)
            FxKind.WIN_SEAL -> ch.winSeal[c] = p
            FxKind.BREATHE -> ch.breathe = p
            FxKind.LOSS_REVEAL -> ch.loss[c] = p
        }
    }
}

private fun DrawScope.drawBoard(
    state: GameState, overlay: BoardOverlay, p: FusePalette, g: Glyphs, ch: Channels, fx: BoardFx,
    pad: Float, cs: Float, countStyle: CountStyle, highlightSame: Boolean,
) {
    val dp = density
    val boardSize = cs * 9
    val corner = CornerRadius(8 * dp, 8 * dp)
    drawRoundRect(p.board, Offset(pad, pad), Size(boardSize, boardSize), corner)

    val sel = state.selected
    val selDigit = when {
        overlay.highlightDigit > 0 -> overlay.highlightDigit
        sel >= 0 && state.cells[sel].status.isResolved -> state.digitAt(sel)
        else -> 0
    }
    val now = System.nanoTime()
    val lift = if (sel >= 0) Ease.outCubic((now - fx.selectedAt) / (BoardFx.SELECT_MS * 1_000_000f)) else 0f
    val pulse = 0.5f + 0.5f * sin(now / 1_000_000_000.0 * PI * 1.6).toFloat()

    // ---- Tiles and tints ----
    for (c in 0 until Grid.CELLS) {
        val x = pad + Grid.col(c) * cs
        val y = pad + Grid.row(c) * cs
        val cell = state.cells[c]
        val inset = 1.6f * dp
        val tl = Offset(x + inset, y + inset)
        val sz = Size(cs - inset * 2, cs - inset * 2)
        val r = CornerRadius(3.5f * dp, 3.5f * dp)
        if (!overlay.paused && cell.status == CellStatus.HIDDEN) {
            val rp = ch.ripple[c]
            val safeAmount = when {
                rp >= 0f -> Ease.outCubic(rp)
                cell.safe -> 1f
                else -> 0f
            }
            val fill = lerp(p.tile, p.tileSafe, safeAmount)
            if (safeAmount < 1f) drawRoundRect(p.tileEdge, tl + Offset(0f, 1.2f * dp), sz, r)
            drawRoundRect(fill, tl, sz, r)
            if (safeAmount > 0f) {
                drawRoundRect(
                    p.inkFaint.copy(alpha = 0.55f * safeAmount), tl + Offset(1.5f * dp, 1.5f * dp), Size(sz.width - 3 * dp, sz.height - 3 * dp), r,
                    style = Stroke(1f * dp, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.5f * dp, 2.5f * dp))),
                )
            }
        }
        if (overlay.paused) continue
        if (sel >= 0 && c != sel && (Grid.row(c) == Grid.row(sel) || Grid.col(c) == Grid.col(sel) || Grid.box(c) == Grid.box(sel))) {
            drawRect(p.peerTint, Offset(x, y), Size(cs, cs))
        }
        if (highlightSame && selDigit > 0 && cell.status.isResolved && state.digitAt(c) == selDigit) {
            drawRoundRect(p.accentSoft, tl, sz, r)
        }
        if (c in overlay.region) drawRect(p.accentSoft.copy(alpha = p.accentSoft.alpha * 0.8f), Offset(x, y), Size(cs, cs))
        val t = ch.tint[c]
        if (t > 0f) drawRoundRect(p.accent.copy(alpha = 0.16f * t), tl, sz, r)
        val w = ch.win[c]
        if (w > 0f) drawRoundRect(p.success.copy(alpha = 0.22f * w), tl, sz, r)
    }

    // ---- Grid ----
    val thin = 1f * dp
    val thick = 1.8f * dp
    for (i in 1 until 9) {
        if (i % 3 == 0) continue
        val o = pad + i * cs
        drawLine(p.lineThin, Offset(o, pad), Offset(o, pad + boardSize), thin)
        drawLine(p.lineThin, Offset(pad, o), Offset(pad + boardSize, o), thin)
    }
    for (i in listOf(3, 6)) {
        val o = pad + i * cs
        drawLine(p.lineThick, Offset(o, pad), Offset(o, pad + boardSize), thick)
        drawLine(p.lineThick, Offset(pad, o), Offset(pad + boardSize, o), thick)
    }
    drawRoundRect(p.lineThick, Offset(pad, pad), Size(boardSize, boardSize), corner, style = Stroke(thick))
    if (overlay.paused) return

    // ---- Selected count neighbourhood ----
    if (sel >= 0 && state.cells[sel].status.showsCount) {
        val r0 = (Grid.row(sel) - 1).coerceAtLeast(0)
        val r1 = (Grid.row(sel) + 1).coerceAtMost(8)
        val c0 = (Grid.col(sel) - 1).coerceAtLeast(0)
        val c1 = (Grid.col(sel) + 1).coerceAtMost(8)
        drawRoundRect(
            p.accent.copy(alpha = 0.5f * lift), Offset(pad + c0 * cs + 2 * dp, pad + r0 * cs + 2 * dp),
            Size((c1 - c0 + 1) * cs - 4 * dp, (r1 - r0 + 1) * cs - 4 * dp), CornerRadius(5 * dp, 5 * dp),
            style = Stroke(1.2f * dp, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3 * dp, 3 * dp))),
        )
    }

    // ---- Contents ----
    for (c in 0 until Grid.CELLS) {
        val cx = pad + Grid.col(c) * cs + cs / 2
        val cy = pad + Grid.row(c) * cs + cs / 2
        val center = Offset(cx, cy)
        val cell = state.cells[c]
        val scaleUp = if (c == sel) 1f + 0.04f * lift else 1f
        val undoP = ch.undo[c]
        val contentAlpha = if (undoP >= 0f) 0.3f + 0.7f * Ease.outCubic(undoP) else 1f
        scale(scaleUp, center) {
            when (cell.status) {
                CellStatus.GIVEN, CellStatus.SOLVED -> {
                    val digit = state.digitAt(c)
                    val layout = if (cell.status == CellStatus.GIVEN) g.big[digit] else g.player[digit]
                    val color = if (cell.status == CellStatus.GIVEN) p.ink else p.accent
                    val a = ch.digitAlpha[c] * contentAlpha
                    scale(ch.digitScale[c], center) { drawDigit(layout, center, g.bigPx, color.copy(alpha = color.alpha * a)) }
                    drawCount(state, c, center, cs, p, g, countStyle, contentAlpha)
                }
                CellStatus.SEALED -> drawSealed(state, c, center, cs, p, g, ch, contentAlpha)
                CellStatus.TRIPPED -> drawTripped(state, c, center, cs, p, g, ch)
                CellStatus.HIDDEN -> {
                    drawNotes(cell.notes, center, cs, p, g, ch.note[c], contentAlpha)
                    if (cell.suspect) drawSuspect(center, cs, p, contentAlpha)
                    if (overlay.showSolution) drawDigit(g.note[state.truth.solution[c]], center + Offset(cs * 0.3f, cs * 0.3f), g.notePx, p.inkFaint)
                    if (overlay.showSeals && state.truth.isSeal[c]) drawCircle(p.danger, cs * 0.16f, center, style = Stroke(1.2f * dp))
                    overlay.candidates?.let { if (cell.notes == 0) drawNotes(it[c], center, cs, p.copy(inkSoft = p.accent.copy(alpha = 0.55f)), g, -1f, 1f) }
                    val lossP = ch.loss[c]
                    if (state.status == GameStatus.LOST && state.truth.isSeal[c]) {
                        val a = if (lossP >= 0f) Ease.outCubic(lossP) else if (fx.effects.any { it.kind == FxKind.LOSS_REVEAL && it.cell == c }) 0f else 1f
                        drawSeal(center, cs * 0.26f, p.seal, 1.3f * dp, coreRadius = cs * 0.07f, alpha = 0.55f * a)
                    }
                }
            }
            drawWrong(c, center, cs, p, g, ch)
        }
    }

    // ---- Completed box traces ----
    for (b in 0 until 9) {
        val t = ch.boxTrace[b]
        if (t < 0f) continue
        drawBoxTrace(b, pad, cs, p, t)
    }

    // ---- Hint focus, target and selection ----
    for (c in overlay.focus) {
        if (c == overlay.target) continue
        val x = pad + Grid.col(c) * cs
        val y = pad + Grid.row(c) * cs
        drawCornerMarks(Offset(x, y), cs, p.accent.copy(alpha = 0.7f), dp)
    }
    if (overlay.target >= 0) {
        val x = pad + Grid.col(overlay.target) * cs
        val y = pad + Grid.row(overlay.target) * cs
        drawRoundRect(
            p.accent.copy(alpha = 0.35f + 0.45f * pulse), Offset(x + 2.5f * dp, y + 2.5f * dp), Size(cs - 5 * dp, cs - 5 * dp),
            CornerRadius(4 * dp, 4 * dp), style = Stroke(2f * dp),
        )
    }
    if (sel >= 0) {
        val x = pad + Grid.col(sel) * cs
        val y = pad + Grid.row(sel) * cs
        val grow = (1f - lift) * 3 * dp
        drawRoundRect(
            p.accent.copy(alpha = lift), Offset(x + 1.5f * dp - grow, y + 1.5f * dp - grow), Size(cs - 3 * dp + grow * 2, cs - 3 * dp + grow * 2),
            CornerRadius(4.5f * dp, 4.5f * dp), style = Stroke(2f * dp),
        )
    }
}

private fun DrawScope.drawDigit(layout: TextLayoutResult, center: Offset, fontPx: Float, color: Color) {
    if (color.alpha <= 0.001f) return
    // Centre on the cap height rather than the line box, so digits sit optically centred.
    val capHalf = fontPx * 0.36f
    val topLeft = Offset(center.x - layout.size.width / 2f, center.y + capHalf - layout.firstBaseline)
    drawText(layout, color = color, topLeft = topLeft)
}

private fun DrawScope.drawCount(state: GameState, c: Int, center: Offset, cs: Float, p: FusePalette, g: Glyphs, style: CountStyle, alpha: Float) {
    val n = state.truth.clues[c]
    if (n <= 0) return
    val found = Grid.NEIGHBORS[c].count { state.cells[it].status.isSeal }
    val satisfied = found >= n
    val color = (if (satisfied) p.inkFaint else p.inkSoft).let { it.copy(alpha = it.alpha * alpha) }
    when (style) {
        CountStyle.PIPS -> {
            val r = maxOf(cs * 0.046f, 1.8f * density)
            val gap = cs * 0.13f
            val y = center.y + cs * 0.36f
            val x0 = center.x - gap * (n - 1) / 2f
            for (i in 0 until n) {
                val o = Offset(x0 + gap * i, y)
                if (satisfied) drawCircle(color, r * 0.85f, o, style = Stroke(1.1f * density)) else drawCircle(color, r, o)
            }
        }
        CountStyle.NUMERALS -> drawDigit(g.count[n], Offset(center.x - cs * 0.32f, center.y - cs * 0.3f), g.countPx, color)
    }
}

private fun DrawScope.drawNotes(mask: Int, center: Offset, cs: Float, p: FusePalette, g: Glyphs, noteP: Float, alpha: Float) {
    if (mask == 0) return
    Bits.forEach(mask) { d ->
        val col = (d - 1) % 3 - 1
        val row = (d - 1) / 3 - 1
        val o = Offset(center.x + col * cs * 0.29f, center.y + row * cs * 0.29f)
        val a = if (noteP >= 0f) Ease.outCubic(noteP) else 1f
        drawDigit(g.note[d], o, g.notePx, p.inkSoft.copy(alpha = p.inkSoft.alpha * a * alpha))
    }
}

/** The player's own "seal?" mark: a folded corner, clear of the notes grid. */
private fun DrawScope.drawSuspect(center: Offset, cs: Float, p: FusePalette, alpha: Float) {
    val inset = 1.6f * density
    val right = center.x + cs / 2 - inset
    val top = center.y - cs / 2 + inset
    val s = cs * 0.2f
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(right - s, top); lineTo(right, top); lineTo(right, top + s); close()
    }
    drawPath(path, p.accent.copy(alpha = 0.85f * alpha))
}

private fun DrawScope.drawSealed(state: GameState, c: Int, center: Offset, cs: Float, p: FusePalette, g: Glyphs, ch: Channels, alpha: Float) {
    val dp = density
    val ring = cs * 0.33f
    val d = ch.defuse[c]
    val ws = ch.winSeal[c]
    val digit = state.digitAt(c)
    if (d >= 0f) {
        // 0–120 ms ring tightens · 70–160 lock turns · 140–220 core sinks · 220–380 digit lands.
        val ms = d * 520f
        val tighten = Ease.outCubic(Ease.span(ms, 0f, 120f))
        val turn = Ease.inOutSine(Ease.span(ms, 70f, 160f))
        val sink = Ease.span(ms, 140f, 220f)
        val reveal = Ease.span(ms, 220f, 380f)
        drawSeal(
            center, ring * (1.22f - 0.22f * tighten), p.seal, (1.2f + 0.6f * (1f - tighten)) * dp,
            teethAngle = 45f * turn, teethLength = ring * 0.26f,
            coreRadius = cs * 0.11f * (1f - sink), coreOffsetY = cs * 0.06f * sink, alpha = alpha,
        )
        if (reveal > 0f) scale(0.78f + 0.22f * Ease.outBack(reveal), center) {
            drawDigit(g.seal[digit], center, g.sealPx, p.ink.copy(alpha = Ease.span(reveal, 0f, 0.5f) * alpha))
        }
        return
    }
    val calm = if (ws >= 0f) Ease.outCubic(ws) else if (state.status == GameStatus.WON) 1f else 0f
    // On a won board the seals retire: teeth fold away, the ring thins to a quiet mark.
    drawSeal(
        center, ring * (1f + 0.08f * Ease.bump(ws.coerceAtLeast(0f))), p.seal.copy(alpha = 0.85f - 0.4f * calm), 1.2f * dp,
        teethAngle = 45f, teethLength = ring * 0.26f * (1f - calm), alpha = alpha,
    )
    drawDigit(g.seal[digit], center, g.sealPx, p.ink.copy(alpha = alpha))
}

private fun DrawScope.drawTripped(state: GameState, c: Int, center: Offset, cs: Float, p: FusePalette, g: Glyphs, ch: Channels) {
    val dp = density
    val ring = cs * 0.33f
    val t = ch.trip[c]
    val digit = state.digitAt(c)
    if (t >= 0f) {
        // Freeze · halo widens · fragments drawn inward · ring cracks · digit surfaces.
        val ms = t * 560f
        val halo = Ease.span(ms, 60f, 320f)
        if (halo > 0f && halo < 1f) drawCircle(p.danger.copy(alpha = 0.5f * (1f - halo)), cs * (0.36f + 0.4f * Ease.outCubic(halo)), center, style = Stroke(1.1f * dp))
        val pull = Ease.span(ms, 120f, 360f)
        if (pull > 0f && pull < 1f) for (i in 0 until 8) {
            val a = i * PI.toFloat() / 4f + 0.3f
            val rr = cs * (0.7f - 0.42f * Ease.outCubic(pull))
            drawCircle(p.danger.copy(alpha = 0.7f * (1f - pull)), 1.4f * dp, Offset(center.x + cos(a) * rr, center.y + sin(a) * rr))
        }
        val crack = Ease.outCubic(Ease.span(ms, 260f, 420f))
        val color = lerp(p.seal, p.danger, crack)
        drawSeal(center, ring, color, 1.3f * dp, teethLength = ring * 0.26f * (1f - crack), coreRadius = cs * 0.11f * (1f - crack), gap = crack)
        val reveal = Ease.span(ms, 360f, 560f)
        drawDigit(g.seal[digit], center, g.sealPx, p.danger.copy(alpha = reveal))
        return
    }
    drawSeal(center, ring, p.danger, 1.3f * dp, teethLength = 0f, gap = 1f)
    drawDigit(g.seal[digit], center, g.sealPx, p.danger)
}

private fun DrawScope.drawWrong(c: Int, center: Offset, cs: Float, p: FusePalette, g: Glyphs, ch: Channels) {
    val w = ch.wrong[c]
    if (w >= 0f) {
        // Misregistered print: two offset ghosts of the wrong digit, a dry shake, gone.
        val shake = sin(w * PI.toFloat() * 6f) * (1f - w) * 3f * density
        val a = 0.85f * (1f - Ease.outCubic(w))
        val d = ch.wrongDigit[c]
        translate(shake) {
            drawDigit(g.player[d], center + Offset(-1.2f * density, 0f), g.bigPx, p.danger.copy(alpha = a * 0.6f))
            drawDigit(g.player[d], center + Offset(1.2f * density, 0.6f * density), g.bigPx, p.danger.copy(alpha = a * 0.45f))
        }
    }
    val ws = ch.wrongSeal[c]
    if (ws >= 0f) {
        val shake = sin(ws * PI.toFloat() * 6f) * (1f - ws) * 3f * density
        translate(shake) {
            drawSeal(center, cs * 0.3f, p.danger, 1.2f * density, coreRadius = cs * 0.08f, alpha = 0.8f * (1f - Ease.outCubic(ws)))
        }
    }
}

private fun DrawScope.drawCornerMarks(tl: Offset, cs: Float, color: Color, dp: Float) {
    val l = cs * 0.2f
    val i = 3f * dp
    val w = 1.5f * dp
    val br = Offset(tl.x + cs, tl.y + cs)
    drawLine(color, Offset(tl.x + i, tl.y + i), Offset(tl.x + i + l, tl.y + i), w, StrokeCap.Round)
    drawLine(color, Offset(tl.x + i, tl.y + i), Offset(tl.x + i, tl.y + i + l), w, StrokeCap.Round)
    drawLine(color, Offset(br.x - i, br.y - i), Offset(br.x - i - l, br.y - i), w, StrokeCap.Round)
    drawLine(color, Offset(br.x - i, br.y - i), Offset(br.x - i, br.y - i - l), w, StrokeCap.Round)
}

private fun DrawScope.drawBoxTrace(box: Int, pad: Float, cs: Float, p: FusePalette, t: Float) {
    val x0 = pad + (box % 3) * 3 * cs
    val y0 = pad + (box / 3) * 3 * cs
    val side = cs * 3
    val inset = 3f * density
    val len = (side - inset * 2) * 4
    val drawn = len * Ease.outCubic(Ease.span(t, 0f, 0.7f))
    val alpha = 1f - Ease.span(t, 0.6f, 1f)
    val color = p.accent.copy(alpha = 0.75f * alpha)
    val w = 1.6f * density
    val corners = listOf(
        Offset(x0 + inset, y0 + inset), Offset(x0 + side - inset, y0 + inset),
        Offset(x0 + side - inset, y0 + side - inset), Offset(x0 + inset, y0 + side - inset), Offset(x0 + inset, y0 + inset),
    )
    var remaining = drawn
    for (i in 0 until 4) {
        if (remaining <= 0f) break
        val a = corners[i]
        val b = corners[i + 1]
        val segLen = side - inset * 2
        val f = (remaining / segLen).coerceAtMost(1f)
        drawLine(color, a, Offset(a.x + (b.x - a.x) * f, a.y + (b.y - a.y) * f), w, StrokeCap.Round)
        remaining -= segLen
    }
}
