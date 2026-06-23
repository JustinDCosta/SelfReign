package com.aldrenstudios.selfreign.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aldrenstudios.selfreign.R
import com.aldrenstudios.selfreign.data.Trigger
import com.aldrenstudios.selfreign.ui.MainViewModel
import com.aldrenstudios.selfreign.ui.dashboard.DashboardDetailSheet
import com.aldrenstudios.selfreign.ui.dashboard.DashboardSheet
import com.aldrenstudios.selfreign.ui.theme.Accent
import com.aldrenstudios.selfreign.ui.theme.Lavender
import com.aldrenstudios.selfreign.ui.theme.OceanBlue
import com.aldrenstudios.selfreign.util.TimeFormat

/**
 * Insights: high-level stats (relapses, best streak, money reclaimed) plus a
 * breakdown of the most common relapse triggers as a simple horizontal bar chart.
 */
@Composable
fun InsightsScreen(viewModel: MainViewModel, onOpenHistory: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val now by viewModel.now.collectAsStateWithLifecycle()
    var showStreak by remember { mutableStateOf(false) }

    // Tally tagged triggers, most frequent first. Memoized on the log so it isn't
    // recomputed on every clock tick.
    val triggerCounts = remember(state.relapseLog) {
        state.relapseLog
            .mapNotNull { it.trigger }
            .mapNotNull { Trigger.fromName(it) }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.insights_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                icon = Icons.Filled.Refresh,
                value = state.relapseCount.toString(),
                label = stringResource(R.string.insights_total_relapses),
                accent = OceanBlue,
                onClick = {
                    viewModel.click()
                    onOpenHistory()
                },
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                icon = Icons.Filled.Whatshot,
                value = TimeFormat.shortDuration(state.bestStreakIncludingCurrent(now)),
                label = stringResource(R.string.insights_best_streak),
                accent = Accent,
                onClick = {
                    viewModel.click()
                    showStreak = true
                },
                modifier = Modifier.weight(1f)
            )
        }

        // Money reclaimed (only meaningful when a daily cost is set).
        if (state.costPerDayCents > 0) {
            val saved = state.moneySavedCents(now)
            MetricCard(
                icon = Icons.Filled.Savings,
                value = formatMoney(saved, state.currencySymbol),
                label = stringResource(R.string.insights_money_saved),
                accent = Lavender,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Triggers breakdown.
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = stringResource(R.string.insights_triggers),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))
                if (triggerCounts.isEmpty()) {
                    Text(
                        text = stringResource(R.string.insights_no_triggers),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val max = triggerCounts.maxOf { it.second }
                    triggerCounts.forEach { (trigger, count) ->
                        TriggerBar(label = trigger.label, count = count, fraction = count.toFloat() / max)
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }

    // Tapping "Best streak" opens the shared streak detail sheet.
    if (showStreak) {
        DashboardDetailSheet(
            sheet = DashboardSheet.STREAK,
            state = state,
            now = now,
            onDismiss = { showStreak = false }
        )
    }
}

@Composable
private fun MetricCard(
    icon: ImageVector,
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val cardModifier = modifier
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = cardModifier,
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface
        ) { MetricCardContent(icon, value, label, accent) }
    } else {
        Surface(
            modifier = cardModifier,
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface
        ) { MetricCardContent(icon, value, label, accent) }
    }
}

@Composable
private fun MetricCardContent(
    icon: ImageVector,
    value: String,
    label: String,
    accent: Color
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TriggerBar(label: String, count: Int, fraction: Float) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(count.toString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0.05f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Accent)
            )
        }
    }
}

/** Formats cents into a currency string, e.g. 12345 -> "$123.45". */
private fun formatMoney(cents: Long, symbol: String): String {
    val whole = cents / 100
    val frac = (cents % 100).toInt()
    return "%s%,d.%02d".format(symbol, whole, frac)
}
