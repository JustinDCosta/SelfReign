package com.aldrenstudios.selfreign.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray

/**
 * Encrypted persistence for [RecoveryState].
 *
 * Uses Jetpack Security's [EncryptedSharedPreferences] (AES-256) backed by a key
 * held in the Android Keystore. All recovery data therefore lives encrypted at rest
 * and never leaves the device. If the secure store cannot be created on a given
 * device (rare keystore issues), it falls back to standard prefs so the app still
 * functions rather than crashing.
 */
class RecoveryStateStore(context: Context) {

    private val prefs: SharedPreferences = runCatching {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "recovery_state_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as SharedPreferences
    }.getOrElse {
        context.getSharedPreferences("recovery_state_fallback", Context.MODE_PRIVATE)
    }

    fun load(): RecoveryState = with(prefs) {
        RecoveryState(
            streakStartTimestamp = getLong(K_STREAK_START, System.currentTimeMillis()),
            relapseCount = getInt(K_RELAPSE_COUNT, 0),
            hasUsedFirstForgiveness = getBoolean(K_FIRST_FORGIVE, false),
            bestStreakMillis = getLong(K_BEST_STREAK, 0L),
            relapseLog = RelapseLogCodec.fromJsonArray(
                getString(K_RELAPSE_LOG, null)?.let { runCatching { JSONArray(it) }.getOrNull() }
            ),
            graceActive = getBoolean(K_GRACE_ACTIVE, false),
            graceStartTimestamp = getLong(K_GRACE_START, 0L),
            graceEndTimestamp = getLong(K_GRACE_END, 0L),
            graceProtectedLevel = getInt(K_GRACE_LEVEL, 0),
            onboardingComplete = getBoolean(K_ONBOARDING, false),
            selectedWallpaperId = getString(K_WALLPAPER, "wp_black") ?: "wp_black",
            selectedMusicId = getString(K_MUSIC, null),
            musicEnabled = getBoolean(K_MUSIC_ON, false),
            soundsEnabled = getBoolean(K_SOUNDS_ON, true),
            hapticsEnabled = getBoolean(K_HAPTICS_ON, true),
            levelThresholds = Levels.sanitize(
                getString(K_THRESHOLDS, null)
                    ?.split(",")
                    ?.mapNotNull { it.trim().toIntOrNull() }
                    ?: Levels.defaultThresholds
            ),
            costPerDayCents = getInt(K_COST_PER_DAY, 0),
            currencySymbol = getString(K_CURRENCY, "$") ?: "$",
            appLockEnabled = getBoolean(K_LOCK_ON, false),
            biometricEnabled = getBoolean(K_BIOMETRIC_ON, false),
            pinHash = getString(K_PIN_HASH, null)
        )
    }

    fun save(state: RecoveryState) {
        prefs.edit().apply {
            putLong(K_STREAK_START, state.streakStartTimestamp)
            putInt(K_RELAPSE_COUNT, state.relapseCount)
            putBoolean(K_FIRST_FORGIVE, state.hasUsedFirstForgiveness)
            putLong(K_BEST_STREAK, state.bestStreakMillis)
            putString(K_RELAPSE_LOG, RelapseLogCodec.toJsonArray(state.relapseLog).toString())
            putBoolean(K_GRACE_ACTIVE, state.graceActive)
            putLong(K_GRACE_START, state.graceStartTimestamp)
            putLong(K_GRACE_END, state.graceEndTimestamp)
            putInt(K_GRACE_LEVEL, state.graceProtectedLevel)
            putBoolean(K_ONBOARDING, state.onboardingComplete)
            putString(K_WALLPAPER, state.selectedWallpaperId)
            putString(K_MUSIC, state.selectedMusicId)
            putBoolean(K_MUSIC_ON, state.musicEnabled)
            putBoolean(K_SOUNDS_ON, state.soundsEnabled)
            putBoolean(K_HAPTICS_ON, state.hapticsEnabled)
            putString(K_THRESHOLDS, state.levelThresholds.joinToString(","))
            putInt(K_COST_PER_DAY, state.costPerDayCents)
            putString(K_CURRENCY, state.currencySymbol)
            putBoolean(K_LOCK_ON, state.appLockEnabled)
            putBoolean(K_BIOMETRIC_ON, state.biometricEnabled)
            putString(K_PIN_HASH, state.pinHash)
        }.apply()
    }

    private companion object {
        const val K_STREAK_START = "streak_start"
        const val K_RELAPSE_COUNT = "relapse_count"
        const val K_FIRST_FORGIVE = "first_forgiveness"
        const val K_BEST_STREAK = "best_streak"
        const val K_RELAPSE_LOG = "relapse_log"
        const val K_GRACE_ACTIVE = "grace_active"
        const val K_GRACE_START = "grace_start"
        const val K_GRACE_END = "grace_end"
        const val K_GRACE_LEVEL = "grace_level"
        const val K_ONBOARDING = "onboarding_complete"
        const val K_WALLPAPER = "wallpaper_id"
        const val K_MUSIC = "music_id"
        const val K_MUSIC_ON = "music_enabled"
        const val K_SOUNDS_ON = "sounds_enabled"
        const val K_HAPTICS_ON = "haptics_enabled"
        const val K_THRESHOLDS = "level_thresholds"
        const val K_COST_PER_DAY = "cost_per_day_cents"
        const val K_CURRENCY = "currency_symbol"
        const val K_LOCK_ON = "app_lock_enabled"
        const val K_BIOMETRIC_ON = "biometric_enabled"
        const val K_PIN_HASH = "pin_hash"
    }
}
