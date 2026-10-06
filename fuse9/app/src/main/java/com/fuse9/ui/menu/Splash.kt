package com.fuse9.ui.menu

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.fuse9.ui.common.quietClick
import com.fuse9.ui.theme.FuseText
import com.fuse9.ui.theme.LocalPalette
import com.fuse9.ui.i18n.LocalStrings

/** ~850 ms: a square draws itself, nine cells settle in, the centre locks. Tap skips. */
@Composable
fun Splash(onDone: () -> Unit) {
    val p = LocalPalette.current
    val t = remember { Animatable(0f) }
    val done by rememberUpdatedState(onDone)
    LaunchedEffect(Unit) {
        t.animateTo(1f, tween(850, easing = LinearEasing))
        done()
    }
    Box(Modifier.fillMaxSize().background(p.page).quietClick(label = LocalStrings.current.skip) { done() }, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LogoMark(t.value, Modifier.size(84.dp))
            Spacer(Modifier.height(22.dp))
            Text("FUSE9", style = FuseText.Title, color = p.ink, modifier = Modifier.alpha(span(t.value, 0.62f, 0.95f)))
        }
    }
}

private fun span(t: Float, a: Float, b: Float) = ((t - a) / (b - a)).coerceIn(0f, 1f)

/** The FUSE9 mark at animation time [t] (0..1); t = 1 is the resting logo. */
@Composable
fun LogoMark(t: Float, modifier: Modifier = Modifier, weight: Float = 1f) {
    val p = LocalPalette.current
    Canvas(modifier) {
        val s = size.minDimension
        val w = 1.6.dp.toPx() * weight
        val edge = span(t, 0f, 0.35f)
        // Outline drawn as one continuous stroke around the square.
        val perimeter = s * 4 * edge
        val pts = listOf(Offset(0f, 0f), Offset(s, 0f), Offset(s, s), Offset(0f, s), Offset(0f, 0f))
        var left = perimeter
        for (i in 0 until 4) {
            if (left <= 0f) break
            val f = (left / s).coerceAtMost(1f)
            val a = pts[i]; val b = pts[i + 1]
            drawLine(p.ink, a, Offset(a.x + (b.x - a.x) * f, a.y + (b.y - a.y) * f), w)
            left -= s
        }
        val cell = s / 3f
        for (i in 0 until 9) {
            val local = span(t, 0.3f + i * 0.03f, 0.48f + i * 0.03f)
            if (local <= 0f) continue
            val r = i / 3
            val c = i % 3
            val center = Offset(c * cell + cell / 2, r * cell + cell / 2)
            if (i == 4) continue
            drawCircle(p.inkSoft.copy(alpha = local), cell * 0.07f * (0.6f + 0.4f * local) * (1f + (weight - 1f) * 0.25f), center)
        }
        val lock = span(t, 0.55f, 0.8f)
        if (lock > 0f) {
            val center = Offset(s / 2, s / 2)
            val ring = cell * 0.3f * (1.25f - 0.25f * lock)
            drawCircle(p.accent.copy(alpha = lock), ring, center, style = androidx.compose.ui.graphics.drawscope.Stroke(w))
            drawCircle(p.accent.copy(alpha = lock), cell * 0.1f, center)
            for (k in 0 until 4) {
                val a = Math.toRadians(k * 90.0)
                val dx = kotlin.math.cos(a).toFloat(); val dy = kotlin.math.sin(a).toFloat()
                val r0 = ring + w * 1.6f
                drawLine(p.accent.copy(alpha = lock), center + Offset(dx * r0, dy * r0), center + Offset(dx * (r0 + cell * 0.08f), dy * (r0 + cell * 0.08f)), w)
            }
        }
    }
}
