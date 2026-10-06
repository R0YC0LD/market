package com.fuse9.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs

class SfxSynthTest {
    private val synth = SfxSynth()

    @Test
    fun everySoundIsShortCleanAndUnclipped() {
        for (sfx in Sfx.entries) {
            val pcm = synth.render(sfx)
            val ms = pcm.size * 1000 / SfxSynth.SAMPLE_RATE
            assertTrue("$sfx too long: $ms ms", ms <= 1500)
            assertTrue("$sfx empty", pcm.any { abs(it) > 0.01f })
            assertTrue("$sfx clips", pcm.all { !it.isNaN() && abs(it) <= 0.6f })
            assertTrue("$sfx must start silent", abs(pcm.first()) < 0.01f)
            assertTrue("$sfx must end silent", abs(pcm.last()) < 0.01f)
        }
    }

    @Test
    fun interactionSoundsStayQuieterThanEvents() {
        fun peak(s: Sfx) = synth.render(s).maxOf { abs(it) }
        assertTrue(peak(Sfx.SELECT) < peak(Sfx.PLACE))
        assertTrue(peak(Sfx.NOTE) < peak(Sfx.DISARM))
        assertTrue(peak(Sfx.RIPPLE) < peak(Sfx.UNIT))
    }

    @Test
    fun deterministic() {
        assertTrue(SfxSynth().render(Sfx.TRIP).contentEquals(SfxSynth().render(Sfx.TRIP)))
    }

    @Test
    fun wavHeaderIsValid() {
        val wav = synth.wav(synth.render(Sfx.PLACE))
        assertEquals("RIFF", String(wav, 0, 4))
        assertEquals("WAVE", String(wav, 8, 4))
        assertEquals(wav.size - 8, (wav[4].toInt() and 0xFF) or ((wav[5].toInt() and 0xFF) shl 8) or ((wav[6].toInt() and 0xFF) shl 16))
    }

    /** Writes every sound to WAV for listening: -Dfuse9.sfx=/abs/dir */
    @Test
    fun export() {
        val dir = System.getProperty("fuse9.sfx")?.takeIf { it.isNotBlank() } ?: return
        File(dir).mkdirs()
        for (sfx in Sfx.entries) File(dir, "${sfx.name.lowercase()}.wav").writeBytes(synth.wav(synth.render(sfx)))
    }
}
