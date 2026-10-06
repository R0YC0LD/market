package com.fuse9.game

import kotlinx.serialization.Serializable

/**
 * Modes are rule presets over the same engine. Adding one is a new entry, not new code paths.
 * [mistakeLimit] of 0 means unlimited.
 */
@Serializable
enum class GameMode(val mistakeLimit: Int, val countsForStreak: Boolean, val available: Boolean) {
    CLASSIC(3, false, true),
    DAILY(3, true, true),
    HARDCORE(1, false, true),
    ZEN(0, false, true),
    TIME_PRESSURE(3, false, false),
    ENDLESS(3, false, false),
    CUSTOM(3, false, false),
}
