package com.fuse9.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import com.fuse9.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

/**
 * Low-latency SFX through SoundPool. Sounds are synthesised once into the cache directory.
 * A light mixer keeps overlapping sounds from stacking into clipping: identical sounds within
 * 30 ms are dropped, and gain falls off as more sounds pile into the same instant.
 */
class AudioManager(private val context: Context) {
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        ).build()
    private val ids = ConcurrentHashMap<Sfx, Int>()
    private val ready = ConcurrentHashMap.newKeySet<Int>()
    private val lastPlayed = ConcurrentHashMap<Sfx, Long>()
    private val recent = ArrayDeque<Long>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile var settings: Settings = Settings()

    init {
        pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) ready.add(id) }
        scope.launch { loadAll() }
    }

    private fun loadAll() {
        val dir = File(context.cacheDir, "sfx/v${SfxSynth.VERSION}").apply { mkdirs() }
        val synth = SfxSynth()
        for (sfx in Sfx.entries) {
            val file = File(dir, "${sfx.name.lowercase()}.wav")
            if (!file.exists() || file.length() < 64) {
                val tmp = File(dir, file.name + ".tmp")
                tmp.writeBytes(synth.wav(synth.render(sfx)))
                tmp.renameTo(file)
            }
            ids[sfx] = pool.load(file.absolutePath, 1)
        }
    }

    fun play(sfx: Sfx, rate: Float = 1f, gain: Float = 1f) {
        val s = settings
        if (!s.sound || s.sfxVolume <= 0f) return
        val id = ids[sfx] ?: return
        if (id !in ready) return
        val now = SystemClock.uptimeMillis()
        if (sfx != Sfx.RIPPLE && now - (lastPlayed[sfx] ?: 0L) < 30) return
        lastPlayed[sfx] = now
        val crowd = synchronized(recent) {
            while (recent.isNotEmpty() && now - recent.first() > 90) recent.removeFirst()
            recent.addLast(now)
            recent.size
        }
        val v = (s.sfxVolume * gain / sqrt(crowd.toFloat())).coerceIn(0f, 1f)
        pool.play(id, v, v, sfx.priority, 0, rate.coerceIn(0.5f, 2f))
    }

    fun pause() = pool.autoPause()
    fun resume() = pool.autoResume()
}
