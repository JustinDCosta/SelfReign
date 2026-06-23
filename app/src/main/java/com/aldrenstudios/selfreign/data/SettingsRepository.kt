package com.aldrenstudios.selfreign.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Immutable snapshot of lightweight notification preferences. (Appearance is driven
 * by the unlockable store wallpapers; text scales with the device's own font-size
 * setting, so there is no in-app font option.)
 */
data class UserSettings(
    val remindersEnabled: Boolean = false
)

// Single DataStore instance scoped to the application context.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Reads and writes lightweight, non-sensitive UI preferences using Jetpack DataStore.
 * Stored locally on the device only. (Sensitive recovery state lives in the
 * encrypted RecoveryStateStore instead.)
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val REMINDERS = booleanPreferencesKey("reminders_enabled")
    }

    /** Reactive stream of the current settings. */
    val settings: Flow<UserSettings> = context.dataStore.data
        .catch { e ->
            // DataStore surfaces disk read failures as IOException. Recover by emitting
            // empty preferences (callers then fall back to defaults) instead of letting
            // the exception propagate and crash collectors. Re-throw anything unexpected.
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            UserSettings(
                remindersEnabled = prefs[Keys.REMINDERS] ?: false
            )
        }

    suspend fun setRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REMINDERS] = enabled }
    }
}
