package com.aldrenstudios.selfreign.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.aldrenstudios.selfreign.data.RelapseLogEntry
import com.aldrenstudios.selfreign.data.RelapseOutcome
import com.aldrenstudios.selfreign.ui.theme.Accent
import com.aldrenstudios.selfreign.ui.theme.Danger
import com.aldrenstudios.selfreign.ui.theme.Lavender
import com.aldrenstudios.selfreign.ui.theme.OceanBlue
import com.aldrenstudios.selfreign.util.TimeFormat

/** Identifies which detail sheet is open (tapped from the dashboard). */
enum class DashboardSheet { STREAK, LEVEL, HISTORY }

/**
 * Hosts whichever detail bottom sheet is currently requested. Tapping the timer,
 * the level pill, or the relapses chip opens the corresponding sheet.
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
                DashboardSheet.HISTORY -> HistorySheet(state)
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

@Composable
private fun HistorySheet(state: RecoveryState) {
    SheetTitle(stringResource(R.string.sheet_history_title))
    val entries = state.relapseLog.sortedByDescending { it.timestamp }
    if (entries.isEmpty()) {
        Text(
            text = stringResource(R.string.history_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 24.dp)
        )
    } else {
        LazyColumn(
            modifier = Modifier.heightIn(max = 420.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(entries) { entry -> HistoryItem(entry) }
        }
    }
}

@Composable
private fun HistoryItem(entry: RelapseLogEntry) {
    val (label, accent) = outcomeLabel(entry.outcome)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Spacer(Modifier.size(12.dp))
        Text(
            text = TimeFormat.dateTime(entry.timestamp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Maps a stored outcome name to a friendly label + accent color. */
@Composable
private fun outcomeLabel(outcome: String): Pair<String, Color> = when (outcome) {
    RelapseOutcome.FIRST_FORGIVENESS.name -> stringResource(R.string.outcome_first_forgiveness) to Accent
    RelapseOutcome.GRACE_STARTED.name -> stringResource(R.string.outcome_grace_started) to Lavender
    else -> stringResource(R.string.outcome_hard_lock) to Danger
}
