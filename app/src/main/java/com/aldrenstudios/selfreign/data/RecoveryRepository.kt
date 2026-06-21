package com.aldrenstudios.selfreign.data

import com.aldrenstudios.selfreign.util.PinHasher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for the gamified recovery state. Wraps the encrypted
 * [RecoveryStateStore] and exposes the state as an observable [StateFlow] so the
 * whole UI reacts to changes (relapses, unlocks, customization, grace timers).
 *
 * Grace-period expiry is evaluated lazily via [refresh] (call on app open / tick)
 * so the model stays correct without a background service.
 */
class RecoveryRepository(private val store: RecoveryStateStore) {

    private val _state = MutableStateFlow(store.load())
    val state: StateFlow<RecoveryState> = _state.asStateFlow()

    init {
        // Apply the persisted custom milestone thresholds to the global ladder.
        Levels.configure(_state.value.levelThresholds)
    }

    private fun update(newState: RecoveryState) {
        store.save(newState)
        _state.value = newState
    }

    /** Re-evaluates time-based transitions (grace success / expiry) at [now]. */
    fun refresh(now: Long = System.currentTimeMillis()) {
        val resolved = RelapseEngine.evaluateExpiry(_state.value, now)
        if (resolved != _state.value) update(resolved)
    }

    /**
     * Processes a relapse and persists the result. An optional [note] and [trigger]
     * are stored with the history entry. Returns the outcome for the UI.
     */
    fun relapse(
        now: Long = System.currentTimeMillis(),
        note: String? = null,
        trigger: String? = null
    ): RelapseResult {
        val result = RelapseEngine.processRelapse(_state.value, now, note, trigger)
        update(result.newState)
        return result
    }

    fun completeOnboarding() = update(_state.value.copy(onboardingComplete = true))

    fun selectWallpaper(id: String) = update(_state.value.copy(selectedWallpaperId = id))

    fun selectMusic(id: String?) = update(_state.value.copy(selectedMusicId = id))

    fun setMusicEnabled(enabled: Boolean) = update(_state.value.copy(musicEnabled = enabled))

    fun setSoundsEnabled(enabled: Boolean) = update(_state.value.copy(soundsEnabled = enabled))

    fun setHapticsEnabled(enabled: Boolean) = update(_state.value.copy(hapticsEnabled = enabled))

    /** Updates the custom milestone day-thresholds (sanitised) and reconfigures the ladder. */
    fun setLevelThresholds(thresholds: List<Int>) {
        val clean = Levels.sanitize(thresholds)
        Levels.configure(clean)
        update(_state.value.copy(levelThresholds = clean))
    }

    fun resetLevelThresholds() = setLevelThresholds(Levels.defaultThresholds)

    /** Sets the estimated daily cost (in cents) and currency for the money-saved stat. */
    fun setCostPerDay(cents: Int, currency: String) =
        update(_state.value.copy(costPerDayCents = cents.coerceAtLeast(0), currencySymbol = currency.ifBlank { "$" }))

    // --- App lock ---
    /**
     * Enables the lock with a required PIN. The PIN must be 4-6 digits; invalid
     * input is rejected here (the security boundary) and the call becomes a no-op,
     * so a malformed credential can never be persisted.
     */
    fun setPin(pin: String) {
        if (!pin.matches(Regex("^\\d{4,6}$"))) return
        update(_state.value.copy(appLockEnabled = true, pinHash = PinHasher.hash(pin)))
    }

    /** Turns the lock (and any biometric option) off and clears the PIN. */
    fun disableAppLock() = update(
        _state.value.copy(appLockEnabled = false, biometricEnabled = false, pinHash = null)
    )

    /** Toggles biometric unlock. Only meaningful while a PIN lock is set. */
    fun setBiometricEnabled(enabled: Boolean) =
        update(_state.value.copy(biometricEnabled = enabled))

    fun verifyPin(pin: String): Boolean = PinHasher.verify(pin, _state.value.pinHash)

    fun hasPin(): Boolean = !_state.value.pinHash.isNullOrBlank()

    /** Overwrites the entire state (used by backup import). Persisted immediately. */
    fun overwrite(state: RecoveryState) {
        Levels.configure(state.levelThresholds)
        update(state)
    }

    /** Current snapshot, convenient for non-reactive callers (backup export). */
    fun snapshot(): RecoveryState = _state.value
}
