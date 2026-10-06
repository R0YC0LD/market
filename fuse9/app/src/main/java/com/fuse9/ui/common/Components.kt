package com.fuse9.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fuse9.ui.theme.FuseText
import com.fuse9.ui.theme.LocalPalette
import com.fuse9.ui.i18n.LocalStrings

/** Click without Material ripples: FUSE9 answers taps with its own motion and sound. */
@Composable
fun Modifier.quietClick(enabled: Boolean = true, label: String? = null, onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    return this
        .then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier)
        .clickable(interactionSource = source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
}

@Composable
fun IconButtonQuiet(icon: ImageVector, label: String, modifier: Modifier = Modifier, tint: Color = LocalPalette.current.ink, size: Dp = 44.dp, onClick: () -> Unit) {
    Box(modifier.size(size).quietClick(label = label, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
    }
}

/** A menu line: typographic, no card, no chrome. */
@Composable
fun MenuItem(title: String, subtitle: String? = null, enabled: Boolean = true, emphasis: Boolean = false, onClick: () -> Unit) {
    val p = LocalPalette.current
    Column(
        Modifier
            .fillMaxWidth()
            .quietClick(enabled = enabled, label = title, onClick = onClick)
            .padding(vertical = 13.dp)
            .alpha(if (enabled) 1f else 0.4f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = FuseText.Item, color = if (emphasis) p.accent else p.ink)
        if (subtitle != null) {
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = FuseText.Small, color = p.inkSoft)
        }
    }
}

@Composable
fun Hairline(modifier: Modifier = Modifier, width: Dp = 28.dp) {
    val p = LocalPalette.current
    Canvas(modifier.width(width).height(1.dp)) { drawRect(p.lineThick.copy(alpha = 0.35f)) }
}

@Composable
fun ScreenHeader(title: String, onBack: () -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButtonQuiet(FuseIcons.Back, LocalStrings.current.back, onClick = onBack)
        Spacer(Modifier.width(4.dp))
        Text(title, style = FuseText.Heading, color = p.ink)
    }
}

@Composable
fun Toggle(label: String, value: Boolean, note: String? = null, onChange: (Boolean) -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().quietClick(label = label) { onChange(!value) }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = FuseText.Body, color = p.ink)
            if (note != null) Text(note, style = FuseText.Small, color = p.inkSoft)
        }
        Switch(value)
    }
}

/** A small custom switch: a track and a seal-like knob. */
@Composable
fun Switch(on: Boolean) {
    val p = LocalPalette.current
    Canvas(Modifier.size(width = 40.dp, height = 22.dp)) {
        val r = size.height / 2
        drawRoundRect(
            color = if (on) p.accent else p.tile,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
        )
        val cx = if (on) size.width - r else r
        drawCircle(p.board, radius = r - 3.dp.toPx(), center = androidx.compose.ui.geometry.Offset(cx, r))
    }
}

@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEachIndexed { i, o ->
            val active = i == selected
            Box(
                Modifier
                    .quietClick(label = o) { onSelect(i) }
                    .then(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(o, style = FuseText.Small, color = if (active) p.ink else p.inkSoft)
                    Spacer(Modifier.height(3.dp))
                    Canvas(Modifier.width(18.dp).height(2.dp)) { if (active) drawRect(p.accent) }
                }
            }
        }
    }
}

/** Thin track, small seal-like thumb. Drag or tap anywhere along it. */
@Composable
fun FuseSlider(value: Float, label: String, onChange: (Float) -> Unit) {
    val p = LocalPalette.current
    val change = androidx.compose.runtime.rememberUpdatedState(onChange)
    val percentText = LocalStrings.current.percent((value * 100).toInt())
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(36.dp)
            .semantics {
                contentDescription = label
                stateDescription = percentText
                setProgress { v -> change.value(v.coerceIn(0f, 1f)); true }
            }
            .pointerInput(Unit) {
                fun at(x: Float) = (x / size.width).coerceIn(0f, 1f)
                detectTapGestures { change.value(at(it.x)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { c, _ -> change.value((c.position.x / size.width).coerceIn(0f, 1f)) }
            },
    ) {
        val y = size.height / 2
        val r = 7.dp.toPx()
        val x = r + (size.width - 2 * r) * value
        drawLine(p.lineThin, androidx.compose.ui.geometry.Offset(r, y), androidx.compose.ui.geometry.Offset(size.width - r, y), 2.dp.toPx(), androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(p.accent, androidx.compose.ui.geometry.Offset(r, y), androidx.compose.ui.geometry.Offset(x, y), 2.dp.toPx(), androidx.compose.ui.graphics.StrokeCap.Round)
        drawCircle(p.page, r, androidx.compose.ui.geometry.Offset(x, y))
        drawCircle(p.accent, r, androidx.compose.ui.geometry.Offset(x, y), style = androidx.compose.ui.graphics.drawscope.Stroke(1.6.dp.toPx()))
        drawCircle(p.accent, r * 0.35f, androidx.compose.ui.geometry.Offset(x, y))
    }
}
