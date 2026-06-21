package com.aldrenstudios.selfreign.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Pushes a refresh to all placed [StreakWidget]s. Safe to call from anywhere;
 * failures (e.g. no widgets placed) are swallowed.
 */
object WidgetUpdater {

    fun requestUpdate(context: Context) {
        // Widget updates touch disk/IPC, so run off the main thread. Any failure
        // (no widgets placed, transient Glance error) is intentionally swallowed
        // since the widget is a non-critical, best-effort surface.
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                StreakWidget().updateAll(context)
            }
        }
    }
}
