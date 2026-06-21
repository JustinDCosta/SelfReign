package com.aldrenstudios.selfreign.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aldrenstudios.selfreign.HabitApp
import com.aldrenstudios.selfreign.audio.AmbientAudioService
import com.aldrenstudios.selfreign.audio.FeedbackEvent
import com.aldrenstudios.selfreign.data.BackupManager
import com.aldrenstudios.selfreign.data.RecoveryState
import com.aldrenstudios.selfreign.data.RelapseResult
import com.aldrenstudios.selfreign.data.StoreCatalog
import com.aldrenstudios.selfreign.widget.WidgetUpdater
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Shared ViewModel (activity-scoped) for the gamified recovery experience. It owns
 * the live recovery state, the per-second clock tick, relapse processing with
 * feedback, store customization (which also drives the audio service), level-up
 * celebrations, the backup bridge, and app-lock session state.
 */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val application get() = getApplication<HabitApp>()
    private val recovery = application.recoveryRepository
    private val feedback = application.feedbackManager

    /** Persisted recovery/customization state. */
    val state: StateFlow<RecoveryState> = recovery.state

    /** A clock that ticks every second so durations and progress animate live. */
    private val _now = MutableStateFlow(System.currentTimeMillis())
    val now: StateFlow<Long> = _now.asStateFlow()

    /** One-shot relapse outcome to surface in a dialog; cleared once shown. */
    private val _relapseResult = MutableStateFlow<RelapseResult?>(null)
    val relapseResult: StateFlow<RelapseResult?> = _relapseResult.asStateFlow()

    /** When non-null, a level-up celebration for this level should be shown. */
    private val _celebrationLevel = MutableStateFlow<Int?>(null)
    val celebrationLevel: StateFlow<Int?> = _celebrationLevel.asStateFlow()

    /** App-lock session: true once the user has authenticated this session. */
    private val _unlocked = MutableStateFlow(!state.value.appLockEnabled)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    // Tracks the highest level seen so we can detect a fresh level-up for celebration.
    private var lastSeenLevel: Int = state.value.effectiveLevel(System.currentTimeMillis())

    init {
        viewModelScope.launch {
            while (true) {
                val t = System.currentTimeMillis()
                _now.value = t
                recovery.refresh(t)

                // Detect an organic level-up to trigger a celebration.
                val lvl = state.value.effectiveLevel(t)
                if (lvl > lastSeenLevel) {
                    _celebrationLevel.value = lvl
                    feedback.play(FeedbackEvent.LEVEL_UP)
                }
                lastSeenLevel = lvl

                delay(1000)
            }
        }
        state.value.let {
            feedback.setSoundsEnabled(it.soundsEnabled)
            feedback.setHapticsEnabled(it.hapticsEnabled)
        }
    }

    // --- App lock ---
    fun unlock() { _unlocked.value = true }
    fun lock() { if (state.value.appLockEnabled) _unlocked.value = false }
    fun verifyPin(pin: String): Boolean = recovery.verifyPin(pin).also { if (it) unlock() }
    fun isLockEnabled(): Boolean = state.value.appLockEnabled
    fun hasPin(): Boolean = recovery.hasPin()

    /** Sets/changes the unlock PIN (turns the lock on). */
    fun setPin(pin: String) {
        recovery.setPin(pin)
        _unlocked.value = true
    }

    fun disableAppLock() {
        recovery.disableAppLock()
        _unlocked.value = true
    }

    fun setBiometricEnabled(enabled: Boolean) = recovery.setBiometricEnabled(enabled)

    // --- Relapse (with optional note + trigger) ---
    fun confirmRelapse(note: String? = null, trigger: String? = null) {
        val result = recovery.relapse(note = note, trigger = trigger)
        _relapseResult.value = result
        feedback.play(FeedbackEvent.RELAPSE)
        // A relapse can drop the level; resync our celebration baseline.
        lastSeenLevel = result.newState.effectiveLevel(System.currentTimeMillis())
        WidgetUpdater.requestUpdate(application)
    }

    fun dismissRelapseResult() { _relapseResult.value = null }

    fun dismissCelebration() { _celebrationLevel.value = null }

    // --- Onboarding ---
    fun completeOnboarding() {
        feedback.play(FeedbackEvent.CLICK)
        recovery.completeOnboarding()
    }

    // --- Feedback passthrough ---
    fun click() = feedback.play(FeedbackEvent.CLICK)

    // --- Customization / Store ---
    fun selectWallpaper(id: String) {
        feedback.play(FeedbackEvent.CLICK)
        recovery.selectWallpaper(id)
    }

    fun selectMusic(id: String?) {
        feedback.play(FeedbackEvent.CLICK)
        recovery.selectMusic(id)
        syncAudio()
    }

    fun setMusicEnabled(enabled: Boolean) {
        recovery.setMusicEnabled(enabled)
        syncAudio()
    }

    fun setSoundsEnabled(enabled: Boolean) {
        recovery.setSoundsEnabled(enabled)
        feedback.setSoundsEnabled(enabled)
    }

    fun setHapticsEnabled(enabled: Boolean) {
        recovery.setHapticsEnabled(enabled)
        feedback.setHapticsEnabled(enabled)
    }

    // --- Milestones / money ---
    fun setLevelThresholds(thresholds: List<Int>) = recovery.setLevelThresholds(thresholds)
    fun resetLevelThresholds() = recovery.resetLevelThresholds()
    fun setCostPerDay(cents: Int, currency: String) = recovery.setCostPerDay(cents, currency)

    private fun syncAudio() {
        val s = state.value
        val item = s.selectedMusicId?.let { StoreCatalog.byId(it) }
        val unlocked = item != null && s.isUnlocked(item, System.currentTimeMillis())
        if (s.musicEnabled && item?.rawResName != null && unlocked) {
            AmbientAudioService.play(application, item.rawResName)
        } else {
            AmbientAudioService.stop(application)
        }
    }

    // --- Backup ---
    fun exportJson(): String = BackupManager.export(recovery.snapshot())

    fun importJson(raw: String): String? =
        when (val result = BackupManager.import(raw)) {
            is BackupManager.ImportResult.Success -> {
                // Preserve THIS device's app-lock settings across a restore. The backup
                // never carries the PIN/lock state (see BackupManager), so we re-apply
                // the current device-local credential to the imported state. This avoids
                // both lock-out and importing a foreign PIN.
                val current = recovery.snapshot()
                val merged = result.state.copy(
                    appLockEnabled = current.appLockEnabled,
                    biometricEnabled = current.biometricEnabled,
                    pinHash = current.pinHash
                )
                recovery.overwrite(merged)
                feedback.setSoundsEnabled(merged.soundsEnabled)
                feedback.setHapticsEnabled(merged.hapticsEnabled)
                lastSeenLevel = merged.effectiveLevel(System.currentTimeMillis())
                _unlocked.value = !merged.appLockEnabled
                syncAudio()
                WidgetUpdater.requestUpdate(application)
                null
            }
            is BackupManager.ImportResult.Error -> result.reason
        }

    override fun onCleared() {
        feedback.release()
        super.onCleared()
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(app) as T
    }
}
