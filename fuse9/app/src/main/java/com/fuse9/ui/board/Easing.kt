package com.fuse9.ui.board

import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

internal object Ease {
    fun clamp(t: Float) = t.coerceIn(0f, 1f)
    fun outCubic(t: Float) = 1f - (1f - clamp(t)).pow(3)
    fun inOutSine(t: Float) = (1f - kotlin.math.cos(PI.toFloat() * clamp(t))) / 2f
    /** Overshoots ~6% then settles: the "snap" of a digit landing. */
    fun outBack(t: Float, s: Float = 1.9f): Float {
        val x = clamp(t) - 1f
        return 1f + (s + 1f) * x * x * x + s * x * x
    }
    fun bump(t: Float) = if (t <= 0f || t >= 1f) 0f else sin(PI.toFloat() * t)
    /** Maps t from [a,b] into [0,1]. */
    fun span(t: Float, a: Float, b: Float) = clamp((t - a) / (b - a))
}
