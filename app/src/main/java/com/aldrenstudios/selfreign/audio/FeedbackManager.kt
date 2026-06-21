package com.aldrenstudios.selfreign.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Logical UI feedback events. Each maps to an optional sound + a haptic pattern. */
enum class FeedbackEvent(val rawResName: String) {
    CLICK("sfx_click"),
    LEVEL_UP("sfx_levelup"),
    RELAPSE("sfx_relapse")
}

/**
 * Centralised "juiciness" service: subtle haptics + soft UI sound effects.
 *
 * Sound files are looked up from res/raw *by name at runtime*, so the project
 * compiles and runs with no audio assets present. Drop matching files into
 * res/raw (e.g. sfx_click.ogg) and they are picked up automatically. Missing
 * assets simply produce no sound - never a crash.
 *
 * Both channels honour the user's [setSoundsEnabled] / [setHapticsEnabled] toggles.
 */
class FeedbackManager(private val context: Context) {

    private var soundsEnabled = true
    private var hapticsEnabled = true

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    // Lazily resolved sound ids per event (null = asset absent).
    private val soundIds = mutableMapOf<FeedbackEvent, Int?>()

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mgr = context.getSystemService(VibratorManager::class.java)
            mgr?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun setSoundsEnabled(enabled: Boolean) { soundsEnabled = enabled }
    fun setHapticsEnabled(enabled: Boolean) { hapticsEnabled = enabled }

    /** Plays the sound + haptic for [event], respecting user toggles and asset availability. */
    fun play(event: FeedbackEvent) {
        // Feedback is non-essential polish: never let it crash the app.
        try {
            if (soundsEnabled) playSound(event)
        } catch (_: Exception) { /* ignore sound failures */ }
        try {
            if (hapticsEnabled) vibrate(event)
        } catch (_: Exception) { /* ignore haptic failures (e.g. missing permission) */ }
    }

    private fun playSound(event: FeedbackEvent) {
        val id = soundIds.getOrPut(event) { loadRaw(event.rawResName) } ?: return
        soundPool.play(id, 0.6f, 0.6f, 1, 0, 1f)
    }

    /** Resolves a res/raw resource id by name, returning null if it does not exist. */
    private fun loadRaw(name: String): Int? {
        @Suppress("DiscouragedApi")
        val resId = context.resources.getIdentifier(name, "raw", context.packageName)
        return if (resId != 0) soundPool.load(context, resId, 1) else null
    }

    private fun vibrate(event: FeedbackEvent) {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return
        // Distinct, gentle patterns per event type.
        val durations = when (event) {
            FeedbackEvent.CLICK -> longArrayOf(0, 15)
            FeedbackEvent.LEVEL_UP -> longArrayOf(0, 20, 60, 35)
            FeedbackEvent.RELAPSE -> longArrayOf(0, 40)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createWaveform(durations, -1))
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(durations, -1)
        }
    }

    fun release() {
        soundPool.release()
    }
}
