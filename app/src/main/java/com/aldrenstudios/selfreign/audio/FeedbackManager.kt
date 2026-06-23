package com.aldrenstudios.selfreign.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Logical UI feedback events. Each maps to a distinct haptic pattern. */
enum class FeedbackEvent {
    CLICK,
    LEVEL_UP,
    RELAPSE
}

/**
 * Centralised haptic feedback. Honours the user's [setHapticsEnabled] toggle and
 * never crashes: any failure (missing vibrator / permission) is swallowed.
 *
 * (UI sound effects were removed: the app ships no audio assets, so the option
 * was non-functional.)
 */
class FeedbackManager(private val context: Context) {

    private var hapticsEnabled = false

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mgr = context.getSystemService(VibratorManager::class.java)
            mgr?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun setHapticsEnabled(enabled: Boolean) { hapticsEnabled = enabled }

    /** Plays the haptic for [event], respecting the user toggle. */
    fun play(event: FeedbackEvent) {
        if (!hapticsEnabled) return
        try {
            vibrate(event)
        } catch (_: Exception) { /* ignore haptic failures (e.g. missing vibrator) */ }
    }

    private fun vibrate(event: FeedbackEvent) {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return
        // Distinct, gentle patterns per event type.
        val durations = when (event) {
            FeedbackEvent.CLICK -> longArrayOf(0, 12)
            FeedbackEvent.LEVEL_UP -> longArrayOf(0, 18, 60, 30)
            FeedbackEvent.RELAPSE -> longArrayOf(0, 35)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createWaveform(durations, -1))
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(durations, -1)
        }
    }

    /** No native resources to release now that sound playback is gone. */
    fun release() { /* no-op */ }
}
