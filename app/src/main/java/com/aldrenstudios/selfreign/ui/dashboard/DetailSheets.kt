package com.aldrenstudios.selfreign.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aldrenstudios.selfreign.R
import com.aldrenstudios.selfreign.data.Levels
import com.aldrenstudios.selfreign.data.RecoveryState
import com.aldrenstudios.selfreign.ui.theme.Accent
import com.aldrenstudios.selfreign.ui.theme.Lavender
import com.aldrenstudios.selfreign.ui.theme.OceanBlue
import com.aldrenstudios.selfreign.util.TimeFormat

/** Identifies which detail sheet is open (tapped from the dashboard / insights). */
enum class DashboardSheet { STREAK, LEVEL }

/**
 * Hosts whichever detail bottom sheet is currently requested. Tapping the timer
 * opens the streak sheet; the level pill opens the level ladder.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardDetailSheet(
    sheet: DashboardSheet,
    state: RecoveryState,
    now: Long,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp)
        ) {
            when (sheet) {
                DashboardSheet.STREAK -> StreakSheet(state, now)
                DashboardSheet.LEVEL -> LevelSheet(state, now)
            }
        }
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 16.dp)
    )
}

@Composable
private fun StreakSheet(state: RecoveryState, now: Long) {
    SheetTitle(stringResource(R.string.sheet_streak_title))
    StatRow(stringResource(R.string.sheet_current), TimeFormat.shortDuration(state.streakMillis(now)), Accent)
    StatRow(stringResource(R.string.sheet_best), TimeFormat.shortDuration(state.bestStreakIncludingCurrent(now)), OceanBlue)
    StatRow(stringResource(R.string.sheet_started), TimeFormat.dateTime(state.streakStartTimestamp), Lavender)
}

@Composable
private fun StatRow(label: String, value: String, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, color = accent, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LevelSheet(state: RecoveryState, now: Long) {
    val currentLevel = state.effectiveLevel(now)
    SheetTitle(stringResource(R.string.sheet_level_title))
    Text(
        text = stringResource(R.string.sheet_level_current, currentLevel),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 12.dp)
    )
    Levels.all.forEach { level ->
        val reached = currentLevel >= level.level
        val isCurrent = currentLevel == level.level
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (reached) Accent.copy(alpha = 0.18f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (reached) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Accent, modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Level ${level.level} \u00b7 ${level.title}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (reached) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${level.thresholdDays} days",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isCurrent) {
                Text(
                    text = stringResource(R.string.level_current_tag),
                    style = MaterialTheme.typography.labelMedium,
                    color = Accent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
