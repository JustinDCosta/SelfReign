package com.aldrenstudios.selfreign

import android.app.Application
import com.aldrenstudios.selfreign.audio.FeedbackManager
import com.aldrenstudios.selfreign.data.RecoveryRepository
import com.aldrenstudios.selfreign.data.RecoveryStateStore
import com.aldrenstudios.selfreign.data.SettingsRepository
import com.aldrenstudios.selfreign.util.Notifications

/**
 * Application subclass acting as a tiny manual dependency container.
 * For an app this size this is simpler and lighter than a full DI framework.
 */
class HabitApp : Application() {

    // --- Recovery / gamification (encrypted state machine) ---
    val recoveryRepository: RecoveryRepository by lazy {
        RecoveryRepository(RecoveryStateStore(this))
    }

    // --- "Juiciness": haptics + UI sounds ---
    val feedbackManager: FeedbackManager by lazy {
        FeedbackManager(this)
    }

    // --- Lightweight appearance prefs (theme/font/reminders) ---
    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(this)
    }

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannel(this)
        // Resolve grace-period transitions that may have elapsed while the app was closed.
        recoveryRepository.refresh()
    }
}
