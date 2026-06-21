package com.aldrenstudios.selfreign.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.aldrenstudios.selfreign.HabitApp
import com.aldrenstudios.selfreign.MainActivity
import com.aldrenstudios.selfreign.data.Levels
import com.aldrenstudios.selfreign.util.TimeFormat

/**
 * A minimalist home-screen widget showing the current clean-day count and level.
 * Tapping it opens the app. Rendered with Glance (Compose for widgets).
 */
class StreakWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as HabitApp
        val state = app.recoveryRepository.snapshot()
        val now = System.currentTimeMillis()
        val days = TimeFormat.parts(state.streakMillis(now)).days
        val level = state.effectiveLevel(now)
        val levelTitle = Levels.byNumber(level).title

        provideContent {
            GlanceTheme {
                WidgetContent(days = days, level = level, levelTitle = levelTitle)
            }
        }
    }

    @Composable
    private fun WidgetContent(days: Long, level: Int, levelTitle: String) {
        val accent = ColorProvider(Color(0xFF81C784))
        val white = ColorProvider(Color(0xFFECEFF1))
        val subtle = ColorProvider(Color(0xFF9AA0A6))

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFF000000)))
                .padding(16.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = days.toString(),
                style = TextStyle(color = accent, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                text = if (days == 1L) "DAY CLEAN" else "DAYS CLEAN",
                style = TextStyle(color = white, fontWeight = FontWeight.Medium)
            )
            Text(
                text = "Level $level \u00b7 $levelTitle",
                style = TextStyle(color = subtle, fontSize = 12.sp)
            )
        }
    }
}

/** The manifest-registered receiver that hosts [StreakWidget]. */
class StreakWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StreakWidget()
}
