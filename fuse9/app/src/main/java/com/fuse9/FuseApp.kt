package com.fuse9

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.fuse9.audio.AudioManager
import com.fuse9.game.PuzzleSource
import com.fuse9.haptics.HapticManager
import com.fuse9.persistence.SaveRepository
import com.fuse9.settings.SettingsRepository
import com.fuse9.stats.StatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Manual dependency container: a handful of long-lived services, no framework. */
class AppContainer(app: Application) {
    val scope = CoroutineScope(SupervisorJob())
    val settings = SettingsRepository(app)
    val saves = SaveRepository(app.filesDir)
    val stats = StatsRepository(app.filesDir)
    val audio = AudioManager(app)
    val haptics = HapticManager(app)
    val puzzles = PuzzleSource(scope)

    init {
        scope.launch {
            settings.settings.collect {
                audio.settings = it
                haptics.enabled = it.haptics
            }
        }
    }
}

class FuseApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) = container.audio.pause()
            override fun onStart(owner: LifecycleOwner) = container.audio.resume()
        })
    }
}
