package com.fuse9.ui.game

import com.fuse9.audio.AudioManager
import com.fuse9.audio.Sfx
import com.fuse9.game.GameEvent
import com.fuse9.haptics.Haptic
import com.fuse9.haptics.HapticManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Sound and touch for each event, timed to the board's animation of the same event.
 * Sounds that belong later in an animation (a unit completing after the digit lands, the
 * win chord when the seals retire) are scheduled, not stacked.
 */
class Feedback(private val audio: AudioManager, private val haptics: HapticManager, private val scope: CoroutineScope) {
    fun on(events: List<GameEvent>) {
        val important = events.any { it is GameEvent.Won || it is GameEvent.Lost }
        for (e in events) when (e) {
            is GameEvent.Selected -> { audio.play(Sfx.SELECT); haptics.perform(Haptic.SELECT) }
            is GameEvent.Placed -> { audio.play(Sfx.PLACE); haptics.perform(Haptic.PLACE) }
            is GameEvent.WrongDigit, is GameEvent.WrongSeal -> { audio.play(Sfx.WRONG); haptics.perform(Haptic.WRONG) }
            is GameEvent.Defused -> { audio.play(Sfx.DISARM); haptics.perform(Haptic.DISARM) }
            is GameEvent.Tripped -> { audio.play(Sfx.TRIP); haptics.perform(Haptic.TRIP) }
            is GameEvent.NoteToggled -> { audio.play(Sfx.NOTE, rate = 0.94f + e.digit * 0.015f); haptics.perform(Haptic.NOTE) }
            is GameEvent.SuspectToggled -> audio.play(Sfx.SUSPECT)
            is GameEvent.Ripple -> scope.launch {
                e.cells.forEachIndexed { i, _ ->
                    delay(if (i == 0) 60 else 38)
                    audio.play(Sfx.RIPPLE, rate = 0.92f + i * 0.045f, gain = 0.8f)
                }
            }
            is GameEvent.UnitCompleted -> if (!important) scope.launch {
                delay(140)
                audio.play(Sfx.UNIT, rate = if (e.unit >= 18) 1f else 1.12f)
                haptics.perform(Haptic.UNIT)
            }
            is GameEvent.Undone -> audio.play(Sfx.UNDO)
            GameEvent.Won -> scope.launch {
                delay(720)
                audio.play(Sfx.COMPLETE)
                haptics.perform(Haptic.COMPLETE)
            }
            GameEvent.Lost -> scope.launch { delay(380); audio.play(Sfx.LOSE) }
            is GameEvent.ModeChanged -> audio.play(Sfx.TAP)
            GameEvent.Rejected -> Unit
        }
    }

    fun tap() = audio.play(Sfx.TAP)
    fun hint() = audio.play(Sfx.HINT)
}
