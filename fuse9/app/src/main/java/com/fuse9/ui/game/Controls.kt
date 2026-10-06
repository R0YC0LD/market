package com.fuse9.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fuse9.game.InputMode
import com.fuse9.ui.common.FuseIcons
import com.fuse9.ui.theme.FuseFonts
import com.fuse9.ui.theme.FuseText
import com.fuse9.ui.theme.LocalPalette
import com.fuse9.ui.i18n.LocalStrings

/** A press-responsive surface: sinks slightly while held. No ripples. */
@Composable
private fun Pressable(modifier: Modifier, label: String, state: String? = null, enabled: Boolean = true, onClick: () -> Unit, content: @Composable () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val click by rememberUpdatedState(onClick)
    val s by animateFloatAsState(if (pressed) 0.92f else 1f, tween(if (pressed) 60 else 140), label = "press")
    Box(
        modifier
            .semantics {
                contentDescription = label
                role = Role.Button
                if (state != null) stateDescription = state
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(onPress = {
                    pressed = true
                    val released = tryAwaitRelease()
                    pressed = false
                    if (released) click()
                })
            }
            .scale(s),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun ToolRow(
    inputMode: InputMode,
    hintLevel: Int,
    onUndo: () -> Unit,
    onErase: () -> Unit,
    onNotes: () -> Unit,
    onSeal: () -> Unit,
    onHint: () -> Unit,
) {
    val t = LocalStrings.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        Tool(FuseIcons.Undo, t.undo, onClick = onUndo)
        Tool(FuseIcons.Erase, t.erase, onClick = onErase)
        Tool(FuseIcons.Notes, t.notes, active = inputMode == InputMode.NOTES, onClick = onNotes)
        Tool(FuseIcons.Seal, if (inputMode == InputMode.NOTES) t.suspect else t.seal, primary = true, onClick = onSeal)
        Tool(FuseIcons.Hint, t.hint, dots = hintLevel, onClick = onHint)
    }
}

@Composable
private fun Tool(icon: ImageVector, label: String, active: Boolean = false, primary: Boolean = false, dots: Int = 0, onClick: () -> Unit) {
    val p = LocalPalette.current
    val t = LocalStrings.current
    val tint = if (active || primary) p.accent else p.ink
    Pressable(Modifier.size(width = 68.dp, height = 58.dp), label, state = if (active) t.on else null, onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                if (primary) Canvas(Modifier.size(34.dp)) { drawCircle(p.accentSoft) }
                Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(1.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t.upper(label), maxLines = 1, style = FuseText.Label.copy(fontSize = 9.sp, letterSpacing = 0.9.sp), color = if (active) p.accent else p.inkSoft)
            }
            Canvas(Modifier.size(width = 22.dp, height = 3.dp)) {
                if (active) drawRect(p.accent)
                for (i in 0 until dots.coerceAtMost(2)) drawCircle(p.accent, 1.5.dp.toPx(), Offset(size.width / 2 + (i * 2 - (dots.coerceAtMost(2) - 1)) * 3.dp.toPx(), size.height / 2))
            }
        }
    }
}

@Composable
fun DigitPad(inputMode: InputMode, remaining: IntArray, highlight: Int, onDigit: (Int) -> Unit) {
    val p = LocalPalette.current
    val t = LocalStrings.current
    val notes = inputMode == InputMode.NOTES
    Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (d in 1..9) {
            val left = remaining[d]
            val done = left == 0
            Pressable(
                Modifier.weight(1f).height(64.dp).alpha(if (done) 0.28f else 1f), t.digitKey(d),
                state = if (done) t.digitComplete else t.digitsLeft(left), onClick = { onDigit(d) },
            ) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    if (notes) {
                        // In pencil mode each key shows its digit where it would sit as a note.
                        Box(Modifier.size(30.dp)) {
                            val col = (d - 1) % 3
                            val row = (d - 1) / 3
                            Text(
                                "$d", style = TextStyle(fontFamily = FuseFonts.Sans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                                color = p.accent, modifier = Modifier.align(Alignment.TopStart).padding(start = (col * 10).dp, top = (row * 9).dp),
                            )
                        }
                    } else {
                        Text("$d", style = TextStyle(fontFamily = FuseFonts.Sans, fontWeight = FontWeight.Medium, fontSize = 27.sp), color = if (highlight == d) p.accent else p.ink)
                    }
                    Spacer(Modifier.height(2.dp))
                    Canvas(Modifier.size(width = 24.dp, height = 4.dp)) {
                        // Remaining placements as a short dotted gauge, max nine.
                        val n = left.coerceIn(0, 9)
                        val step = size.width / 9f
                        for (i in 0 until n) drawCircle(p.inkFaint, 1.1.dp.toPx(), Offset(step * (i + 0.5f), size.height / 2))
                    }
                }
            }
        }
    }
}

@Composable
fun StrikeMarks(used: Int, limit: Int) {
    val p = LocalPalette.current
    val label = LocalStrings.current.strikes(used, limit)
    if (limit == 0) {
        if (used > 0) Text("×$used", style = FuseText.Numeric, color = p.danger)
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.semantics { contentDescription = label }) {
        for (i in 0 until limit) {
            Canvas(Modifier.size(9.dp)) {
                if (i < used) drawCircle(p.danger) else drawCircle(p.inkFaint, style = Stroke(1.2.dp.toPx()))
            }
        }
    }
}
