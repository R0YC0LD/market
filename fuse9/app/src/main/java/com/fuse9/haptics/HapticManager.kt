package com.fuse9.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

enum class Haptic { SELECT, PLACE, WRONG, DISARM, TRIP, UNIT, COMPLETE, NOTE }

/**
 * Each moment gets its own texture so the hands can tell them apart: a placement is a crisp
 * click, a defusal is click → rise → tick in time with its sound, a strike is a dull thud.
 * Uses composition primitives where the device supports them, amplitude one-shots otherwise.
 */
class HapticManager(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    @Volatile var enabled: Boolean = true

    private val primitives: Boolean = Build.VERSION.SDK_INT >= 31 && vibrator?.let {
        it.areAllPrimitivesSupported(
            VibrationEffect.Composition.PRIMITIVE_CLICK,
            VibrationEffect.Composition.PRIMITIVE_TICK,
            VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
            VibrationEffect.Composition.PRIMITIVE_THUD,
            VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
        )
    } == true

    fun perform(h: Haptic) {
        val v = vibrator ?: return
        if (!enabled || !v.hasVibrator()) return
        runCatching { v.vibrate(if (primitives) composed(h) else fallback(h)) }
    }

    private fun composed(h: Haptic): VibrationEffect {
        val c = VibrationEffect.startComposition()
        when (h) {
            Haptic.SELECT -> c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.35f)
            Haptic.NOTE -> c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.25f)
            Haptic.PLACE -> c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.55f)
            Haptic.WRONG -> c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 0.45f)
            Haptic.DISARM -> c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.4f)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f, 60)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.35f, 30)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.7f, 60)
            Haptic.TRIP -> c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 0.9f)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.5f, 90)
            Haptic.UNIT -> c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.4f)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.25f, 50)
            Haptic.COMPLETE -> c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.3f)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.45f, 110)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.6f, 110)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.8f, 140)
        }
        return c.compose()
    }

    private fun fallback(h: Haptic): VibrationEffect = when (h) {
        Haptic.SELECT, Haptic.NOTE -> VibrationEffect.createOneShot(6, 40)
        Haptic.PLACE -> VibrationEffect.createOneShot(12, 110)
        Haptic.WRONG -> VibrationEffect.createOneShot(28, 90)
        Haptic.DISARM -> VibrationEffect.createWaveform(longArrayOf(0, 10, 60, 8, 70, 18), intArrayOf(0, 120, 0, 90, 0, 180), -1)
        Haptic.TRIP -> VibrationEffect.createWaveform(longArrayOf(0, 45, 80, 14), intArrayOf(0, 200, 0, 80), -1)
        Haptic.UNIT -> VibrationEffect.createWaveform(longArrayOf(0, 8, 50, 6), intArrayOf(0, 90, 0, 60), -1)
        Haptic.COMPLETE -> VibrationEffect.createWaveform(longArrayOf(0, 8, 110, 8, 110, 10, 140, 20), intArrayOf(0, 60, 0, 90, 0, 120, 0, 200), -1)
    }
}
