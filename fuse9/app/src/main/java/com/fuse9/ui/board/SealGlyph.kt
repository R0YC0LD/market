package com.fuse9.ui.board

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

/**
 * The seal mark, drawn procedurally so every state can be animated: a ring with four lock
 * teeth around a solid core. Locked = teeth on the axes + core. Defused = teeth turned 45°,
 * core gone. Broken = ring split open.
 */
internal fun DrawScope.drawSeal(
    center: Offset,
    radius: Float,
    color: Color,
    strokeWidth: Float,
    teethAngle: Float = 0f,
    teethLength: Float = radius * 0.28f,
    coreRadius: Float = 0f,
    coreOffsetY: Float = 0f,
    gap: Float = 0f,
    alpha: Float = 1f,
) {
    if (alpha <= 0f) return
    val c = color.copy(alpha = color.alpha * alpha.coerceIn(0f, 1f))
    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    val topLeft = Offset(center.x - radius, center.y - radius)
    val size = Size(radius * 2, radius * 2)
    if (gap <= 0f) {
        drawCircle(c, radius, center, style = stroke)
    } else {
        val g = 46f * gap.coerceIn(0f, 1f)
        drawArc(c, -60f + g / 2, 180f - g, false, topLeft, size, style = stroke)
        drawArc(c, 120f + g / 2, 180f - g, false, topLeft, size, style = stroke)
    }
    if (teethLength > 0f) {
        for (k in 0 until 4) {
            val a = Math.toRadians((teethAngle + k * 90f).toDouble())
            val dx = cos(a).toFloat()
            val dy = sin(a).toFloat()
            val r0 = radius + strokeWidth * 1.6f
            drawLine(c, Offset(center.x + dx * r0, center.y + dy * r0), Offset(center.x + dx * (r0 + teethLength), center.y + dy * (r0 + teethLength)), strokeWidth, StrokeCap.Round)
        }
    }
    if (coreRadius > 0f) drawCircle(c, coreRadius, Offset(center.x, center.y + coreOffsetY))
}
