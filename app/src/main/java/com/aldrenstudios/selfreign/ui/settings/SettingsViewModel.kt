package com.aldrenstudios.selfreign.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aldrenstudios.selfreign.data.FontSizeOption
import com.aldrenstudios.selfreign.data.SettingsRepository
import com.aldrenstudios.selfreign.data.UserSettings
import com.aldrenstudios.selfreign.util.ReminderWorker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Reads and updates lightweight appearance/notification preferences. */
class SettingsViewModel(
    private val repo: SettingsRepository,
    private val appContext: Context
) : ViewModel() {

    val settings: StateFlow<UserSettings> =
        repo.settings.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            UserSettings()
        )

    fun setFontSize(option: FontSizeOption) = viewModelScope.launch { repo.setFontSize(option) }

    fun setReminders(enabled: Boolean) = viewModelScope.launch {
        repo.setRemindersEnabled(enabled)
        // Translate the preference into an actual scheduled (or cancelled) job.
        if (enabled) ReminderWorker.schedule(appContext) else ReminderWorker.cancel(appContext)
    }

    class Factory(
        private val repo: SettingsRepository,
        private val appContext: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(repo, appContext) as T
    }
}
