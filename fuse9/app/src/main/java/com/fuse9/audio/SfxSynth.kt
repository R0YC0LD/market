package com.fuse9.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

/**
 * Every sound in FUSE9 is synthesised here from oscillators and filtered noise — no samples,
 * nothing borrowed. Pure Kotlin so it can be unit-tested and rendered to WAV on the JVM.
 */
class SfxSynth(private val sampleRate: Int = SAMPLE_RATE, seed: Int = 0x5EA1) {
    private var noiseState = seed

    private fun noise(): Float {
        // xorshift32 — deterministic, so every render of a sound is identical.
        var x = noiseState
        x = x xor (x shl 13); x = x xor (x ushr 17); x = x xor (x shl 5)
        noiseState = x
        return (x and 0xFFFF) / 32768f - 1f
    }

    private fun buffer(ms: Int) = FloatArray(sampleRate * ms / 1000)
    private fun at(ms: Float) = (ms * sampleRate / 1000f).toInt()

    /**
     * Adds a decaying partial. [tauMs] is the exponential decay constant; [glideTo] bends the
     * pitch linearly over the note's length.
     */
    private fun tone(
        buf: FloatArray, startMs: Float, lengthMs: Float, freq: Float, amp: Float,
        tauMs: Float, attackMs: Float = 1.5f, glideTo: Float = freq, partials: FloatArray = floatArrayOf(1f), partialAmps: FloatArray = floatArrayOf(1f),
    ) {
        val start = at(startMs)
        val n = at(lengthMs)
        val attack = at(attackMs).coerceAtLeast(1)
        val phases = DoubleArray(partials.size)
        for (i in 0 until n) {
            val idx = start + i
            if (idx >= buf.size) break
            val t = i.toFloat() / n
            val f = freq + (glideTo - freq) * t
            val env = (if (i < attack) i.toFloat() / attack else 1f) * exp(-(i * 1000f / sampleRate) / tauMs)
            var s = 0.0
            for (p in partials.indices) {
                phases[p] += 2.0 * PI * f * partials[p] / sampleRate
                s += sin(phases[p]) * partialAmps[p]
            }
            buf[idx] += (s * amp * env).toFloat()
        }
    }

    /** Band-limited noise burst: one-pole low-pass at [lowHz] minus one-pole low-pass at [highHz]. */
    private fun hiss(buf: FloatArray, startMs: Float, lengthMs: Float, amp: Float, tauMs: Float, lowHz: Float, highHz: Float = 0f, attackMs: Float = 0.5f) {
        val start = at(startMs)
        val n = at(lengthMs)
        val attack = at(attackMs).coerceAtLeast(1)
        val aLow = onePole(lowHz)
        val aHigh = if (highHz > 0) onePole(highHz) else 0f
        var lp = 0f
        var hp = 0f
        for (i in 0 until n) {
            val idx = start + i
            if (idx >= buf.size) break
            val x = noise()
            lp += aLow * (x - lp)
            hp += aHigh * (lp - hp)
            val band = if (highHz > 0) lp - hp else lp
            val env = (if (i < attack) i.toFloat() / attack else 1f) * exp(-(i * 1000f / sampleRate) / tauMs)
            buf[idx] += band * amp * env
        }
    }

    private fun onePole(hz: Float): Float = (1f - exp(-2f * PI.toFloat() * hz / sampleRate)).coerceIn(0f, 1f)

    /** Normalises to [peak], adds 2 ms edge fades so nothing clicks. */
    private fun finish(buf: FloatArray, peak: Float): FloatArray {
        val max = buf.maxOf { abs(it) }.coerceAtLeast(1e-6f)
        val g = peak / max
        val fade = at(2f)
        for (i in buf.indices) {
            var v = buf[i] * g
            if (i < fade) v *= i.toFloat() / fade
            val tail = buf.size - 1 - i
            if (tail < fade * 4) v *= tail.toFloat() / (fade * 4)
            buf[i] = v
        }
        return buf
    }

    fun render(sfx: Sfx): FloatArray = when (sfx) {
        Sfx.SELECT -> buffer(40).also {
            hiss(it, 0f, 30f, 0.9f, 3.5f, 5200f, 1800f)
            tone(it, 0f, 30f, 1880f, 0.25f, 5f)
        }.let { finish(it, 0.22f) }

        Sfx.NOTE -> buffer(35).also {
            tone(it, 0f, 30f, 2640f, 0.5f, 4.5f)
            hiss(it, 0f, 10f, 0.25f, 2f, 7000f, 3000f)
        }.let { finish(it, 0.16f) }

        Sfx.PLACE -> buffer(190).also {
            hiss(it, 0f, 8f, 0.6f, 1.6f, 6000f, 1500f)
            tone(it, 0f, 70f, 880f, 0.55f, 18f, partials = floatArrayOf(1f, 2f), partialAmps = floatArrayOf(1f, 0.12f))
            tone(it, 46f, 140f, 1318.5f, 0.5f, 34f, partials = floatArrayOf(1f, 2f), partialAmps = floatArrayOf(1f, 0.1f))
            hiss(it, 46f, 6f, 0.3f, 1.2f, 6000f, 2000f)
        }.let { finish(it, 0.42f) }

        Sfx.WRONG -> buffer(140).also {
            tone(it, 0f, 130f, 176f, 0.9f, 30f, attackMs = 1f, glideTo = 118f, partials = floatArrayOf(1f, 2.02f), partialAmps = floatArrayOf(1f, 0.25f))
            hiss(it, 0f, 40f, 0.8f, 8f, 700f)
        }.let { finish(it, 0.46f) }

        Sfx.SUSPECT -> buffer(110).also {
            hiss(it, 0f, 6f, 0.5f, 1.2f, 8000f, 2500f)
            tone(it, 1f, 100f, 2350f, 0.4f, 16f, partials = floatArrayOf(1f, 1.583f, 2.183f), partialAmps = floatArrayOf(1f, 0.6f, 0.35f))
        }.let { finish(it, 0.24f) }

        // 0 ms click · 70 ms lock tick · 140 ms tonal rise · 280 ms settle — matches the defuse animation.
        Sfx.DISARM -> buffer(560).also {
            hiss(it, 0f, 10f, 0.7f, 2f, 7000f, 2000f)
            tone(it, 0f, 20f, 2400f, 0.35f, 4f)
            tone(it, 70f, 40f, 3100f, 0.3f, 7f, partials = floatArrayOf(1f, 1.5f), partialAmps = floatArrayOf(1f, 0.4f))
            hiss(it, 70f, 6f, 0.35f, 1.4f, 7000f, 2500f)
            tone(it, 140f, 160f, 392f, 0.42f, 90f, attackMs = 28f, glideTo = 587.3f, partials = floatArrayOf(1f, 2f), partialAmps = floatArrayOf(1f, 0.18f))
            tone(it, 280f, 120f, 98f, 0.65f, 35f, attackMs = 2f)
            tone(it, 280f, 280f, 784f, 0.22f, 95f, attackMs = 6f)
        }.let { finish(it, 0.5f) }

        Sfx.TRIP -> buffer(460).also {
            tone(it, 0f, 300f, 112f, 1f, 70f, attackMs = 2f, glideTo = 66f)
            hiss(it, 0f, 320f, 0.7f, 120f, 900f, 120f, attackMs = 35f)
            tone(it, 30f, 400f, 233f, 0.22f, 150f, attackMs = 20f)
            tone(it, 30f, 400f, 247f, 0.22f, 150f, attackMs = 20f)
        }.let { finish(it, 0.55f) }

        Sfx.RIPPLE -> buffer(26).also {
            tone(it, 0f, 22f, 1560f, 0.6f, 3f)
            hiss(it, 0f, 8f, 0.35f, 1.5f, 6000f, 2500f)
        }.let { finish(it, 0.14f) }

        Sfx.UNIT -> buffer(420).also {
            tone(it, 0f, 410f, 1046.5f, 0.5f, 120f, attackMs = 3f)
            tone(it, 18f, 390f, 1568f, 0.32f, 110f, attackMs = 3f)
            tone(it, 0f, 200f, 2093f, 0.08f, 40f)
        }.let { finish(it, 0.24f) }

        Sfx.COMPLETE -> buffer(1400).also {
            val mallet = floatArrayOf(1f, 3.0f, 4.1f)
            val amps = floatArrayOf(1f, 0.08f, 0.03f)
            tone(it, 0f, 500f, 587.33f, 0.45f, 160f, attackMs = 3f, partials = mallet, partialAmps = amps)
            tone(it, 115f, 500f, 880f, 0.42f, 160f, attackMs = 3f, partials = mallet, partialAmps = amps)
            tone(it, 230f, 500f, 739.99f, 0.4f, 170f, attackMs = 3f, partials = mallet, partialAmps = amps)
            tone(it, 380f, 1000f, 1174.66f, 0.42f, 320f, attackMs = 4f, partials = mallet, partialAmps = amps)
            tone(it, 380f, 1000f, 293.66f, 0.28f, 360f, attackMs = 60f)
            tone(it, 380f, 1000f, 440f, 0.12f, 300f, attackMs = 80f)
        }.let { finish(it, 0.5f) }

        Sfx.LOSE -> buffer(760).also {
            tone(it, 0f, 400f, 440f, 0.4f, 140f, attackMs = 6f, partials = floatArrayOf(1f, 2f), partialAmps = floatArrayOf(1f, 0.1f))
            tone(it, 190f, 560f, 329.63f, 0.42f, 210f, attackMs = 8f, partials = floatArrayOf(1f, 2f), partialAmps = floatArrayOf(1f, 0.1f))
            hiss(it, 190f, 300f, 0.15f, 120f, 600f)
        }.let { finish(it, 0.34f) }

        Sfx.UNDO -> buffer(100).also {
            hiss(it, 0f, 90f, 0.6f, 60f, 2500f, 400f, attackMs = 55f)
            tone(it, 40f, 55f, 720f, 0.2f, 16f, glideTo = 520f)
        }.let { finish(it, 0.17f) }

        Sfx.HINT -> buffer(260).also {
            tone(it, 0f, 160f, 1318.5f, 0.4f, 60f, attackMs = 4f)
            tone(it, 70f, 190f, 1568f, 0.4f, 75f, attackMs = 4f)
        }.let { finish(it, 0.2f) }

        Sfx.TAP -> buffer(30).also {
            hiss(it, 0f, 22f, 0.8f, 2.5f, 4200f, 1200f)
            tone(it, 0f, 20f, 1200f, 0.2f, 4f)
        }.let { finish(it, 0.15f) }
    }

    /** 16-bit mono PCM WAV. */
    fun wav(samples: FloatArray): ByteArray {
        val dataLen = samples.size * 2
        val out = java.io.ByteArrayOutputStream(44 + dataLen)
        fun int(v: Int) { out.write(v and 0xFF); out.write((v shr 8) and 0xFF); out.write((v shr 16) and 0xFF); out.write((v shr 24) and 0xFF) }
        fun short(v: Int) { out.write(v and 0xFF); out.write((v shr 8) and 0xFF) }
        out.write("RIFF".toByteArray()); int(36 + dataLen); out.write("WAVE".toByteArray())
        out.write("fmt ".toByteArray()); int(16); short(1); short(1); int(sampleRate); int(sampleRate * 2); short(2); short(16)
        out.write("data".toByteArray()); int(dataLen)
        for (s in samples) short((s.coerceIn(-1f, 1f) * 32767).toInt())
        return out.toByteArray()
    }

    companion object {
        const val SAMPLE_RATE = 44100
        /** Bump when any sound changes so cached WAVs are re-rendered. */
        const val VERSION = 1
    }
}

enum class Sfx(val priority: Int) {
    TAP(0), SELECT(0), NOTE(0), PLACE(1), WRONG(2), SUSPECT(1), DISARM(3), TRIP(3),
    RIPPLE(0), UNIT(2), COMPLETE(4), LOSE(4), UNDO(1), HINT(1),
}
