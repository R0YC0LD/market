package com.fuse9.game

import kotlinx.serialization.Serializable

/**
 * Modes are rule presets over the same engine. Adding one is a new entry, not new code paths.
 * [mistakeLimit] of 0 means unlimited.
 */
@Serializable
enum class GameMode(val label: String, val mistakeLimit: Int, val countsForStreak: Boolean, val available: Boolean) {
    CLASSIC("Classic", 3, false, true),
    DAILY("Daily", 3, true, true),
    HARDCORE("Hardcore", 1, false, true),
    ZEN("Zen", 0, false, true),
    TIME_PRESSURE("Time Pressure", 3, false, false),
    ENDLESS("Endless", 3, false, false),
    CUSTOM("Custom", 3, false, false);

    val description: String
        get() = when (this) {
            CLASSIC -> "Three strikes. The standard board."
            DAILY -> "One board for everyone, every day."
            HARDCORE -> "A single strike ends the board."
            ZEN -> "No strike limit. No clock pressure."
            TIME_PRESSURE -> "Coming later."
            ENDLESS -> "Coming later."
            CUSTOM -> "Coming later."
        }
}
