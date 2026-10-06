package com.fuse9.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ThemeChoice { SYSTEM, LIGHT, DARK }
enum class CountStyle { PIPS, NUMERALS }

data class Settings(
    val sound: Boolean = true,
    val sfxVolume: Float = 0.8f,
    val haptics: Boolean = true,
    val theme: ThemeChoice = ThemeChoice.SYSTEM,
    val highContrast: Boolean = false,
    val countStyle: CountStyle = CountStyle.PIPS,
    val autoCleanNotes: Boolean = true,
    val highlightSameDigit: Boolean = true,
    val showTimer: Boolean = true,
)

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<Settings> = _settings

    private fun read() = Settings(
        sound = prefs.getBoolean("sound", true),
        sfxVolume = prefs.getFloat("sfxVolume", 0.8f),
        haptics = prefs.getBoolean("haptics", true),
        theme = runCatching { ThemeChoice.valueOf(prefs.getString("theme", null) ?: "SYSTEM") }.getOrDefault(ThemeChoice.SYSTEM),
        highContrast = prefs.getBoolean("highContrast", false),
        countStyle = runCatching { CountStyle.valueOf(prefs.getString("countStyle", null) ?: "PIPS") }.getOrDefault(CountStyle.PIPS),
        autoCleanNotes = prefs.getBoolean("autoCleanNotes", true),
        highlightSameDigit = prefs.getBoolean("highlightSameDigit", true),
        showTimer = prefs.getBoolean("showTimer", true),
    )

    fun update(transform: (Settings) -> Settings) {
        val s = transform(_settings.value)
        _settings.value = s
        prefs.edit()
            .putBoolean("sound", s.sound)
            .putFloat("sfxVolume", s.sfxVolume)
            .putBoolean("haptics", s.haptics)
            .putString("theme", s.theme.name)
            .putBoolean("highContrast", s.highContrast)
            .putString("countStyle", s.countStyle.name)
            .putBoolean("autoCleanNotes", s.autoCleanNotes)
            .putBoolean("highlightSameDigit", s.highlightSameDigit)
            .putBoolean("showTimer", s.showTimer)
            .apply()
    }
}
