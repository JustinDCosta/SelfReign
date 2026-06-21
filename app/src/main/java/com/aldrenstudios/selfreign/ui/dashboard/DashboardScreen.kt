package com.aldrenstudios.selfreign.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aldrenstudios.selfreign.R
import com.aldrenstudios.selfreign.data.Levels
import com.aldrenstudios.selfreign.data.RelapseOutcome
import com.aldrenstudios.selfreign.ui.MainViewModel
import com.aldrenstudios.selfreign.ui.components.ProgressRing
import com.aldrenstudios.selfreign.ui.theme.Accent
import com.aldrenstudios.selfreign.ui.theme.Lavender
import com.aldrenstudios.selfreign.ui.theme.OceanBlue
import com.aldrenstudios.selfreign.util.TimeFormat

/**
 * Dashboard: the focal screen. A circular progress ring (clean-time clock in the
 * center) is the hero; below it sit level/streak stat chips, an optional grace
 * banner, and the guarded relapse action.
 */
@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val now by viewModel.now.collectAsStateWithLifecycle()
    val relapseResult by viewModel.relapseResult.collectAsStateWithLifecycle()
    var confirm by remember { mutableStateOf(false) }
    var openSheet by remember { mutableStateOf<DashboardSheet?>(null) }
    var showPanic by remember { mutableStateOf(false) }
    // Index into the quote list; tapping the quote advances it.
    var quoteIndex by remember { mutableIntStateOf((now / 86_400_000L).toInt()) }

    val streak = state.streakMillis(now)
    val level = state.effectiveLevel(now)
    val current = Levels.byNumber(level)
    val next = Levels.next(level)

    val fraction = if (next != null) {
        val span = (next.thresholdMillis - current.thresholdMillis).coerceAtLeast(1)
        ((streak - current.thresholdMillis).toFloat() / span).coerceIn(0f, 1f)
    } else 1f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))

        // Clickable motivational quote (tap to cycle to the next one)
        QuoteCard(
            quote = com.aldrenstudios.selfreign.util.Quotes.byIndex(quoteIndex),
            onClick = {
                viewModel.click()
                quoteIndex++
            }
        )

        Spacer(Modifier.height(24.dp))

        // Hero: progress ring with the live clock inside (tap -> streak sheet)
        ProgressRing(
            progress = fraction,
            brush = Brush.sweepGradient(listOf(OceanBlue, Accent, Lavender, OceanBlue)),
            trackColor = MaterialTheme.colorScheme.surface,
            glowColor = Color.Transparent,
            modifier = Modifier.clip(CircleShape).clickable {
                viewModel.click()
                openSheet = DashboardSheet.STREAK
            }
        ) {
            ClockContent(streakMillis = streak)
        }

        Spacer(Modifier.height(20.dp))

        // Progress caption
        Text(
            text = if (next != null) {
                val remaining = (next.thresholdMillis - streak).coerceAtLeast(0)
                "${TimeFormat.shortDuration(remaining)} ${stringResource(R.string.next_level_in)} \u00b7 ${next.title}"
            } else stringResource(R.string.max_level_reached),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        // Stat chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StatChip(
                icon = Icons.Filled.EmojiEvents,
                value = "Level $level",
                label = current.title,
                accent = Accent,
                onClick = {
                    viewModel.click()
                    openSheet = DashboardSheet.LEVEL
                },
                modifier = Modifier.weight(1f)
            )
            StatChip(
                icon = Icons.Filled.Refresh,
                value = state.relapseCount.toString(),
                label = stringResource(R.string.relapse_count),
                accent = OceanBlue,
                onClick = {
                    viewModel.click()
                    openSheet = DashboardSheet.HISTORY
                },
                modifier = Modifier.weight(1f)
            )
        }

        // Money reclaimed chip (only when a daily cost has been configured)
        if (state.costPerDayCents > 0) {
            Spacer(Modifier.height(14.dp))
            StatChip(
                icon = Icons.Filled.Savings,
                value = formatMoney(state.moneySavedCents(now), state.currencySymbol),
                label = stringResource(R.string.stat_saved),
                accent = Lavender,
                onClick = {
                    viewModel.click()
                    openSheet = DashboardSheet.HISTORY
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Grace banner (only while shielding)
        if (state.isGraceShielding(now)) {
            Spacer(Modifier.height(16.dp))
            GraceBanner(
                remainingMillis = state.graceRemainingMillis(now),
                protectedLevel = state.graceProtectedLevel
            )
        }

        Spacer(Modifier.height(28.dp))

        // Urge-surfing helper
        PanicButton(onClick = {
            viewModel.click()
            showPanic = true
        })

        Spacer(Modifier.height(12.dp))

        // Relapse action
        RelapseButton(onClick = { confirm = true })

        Spacer(Modifier.height(24.dp))
    }

    // Detail bottom sheets for tappable stats.
    openSheet?.let { which ->
        DashboardDetailSheet(
            sheet = which,
            state = state,
            now = now,
            onDismiss = { openSheet = null }
        )
    }

    if (showPanic) {
        com.aldrenstudios.selfreign.ui.panic.PanicSheet(onDismiss = { showPanic = false })
    }

    if (confirm) {
        RelapseLogDialog(
            onConfirm = { note, trigger ->
                confirm = false
                viewModel.confirmRelapse(note, trigger)
            },
            onDismiss = { confirm = false }
        )
    }

    relapseResult?.let { result ->
        OutcomeDialog(
            outcome = result.outcome,
            protectedLevel = result.protectedLevel,
            onDismiss = { viewModel.dismissRelapseResult() }
        )
    }
}

@Composable
private fun QuoteCard(quote: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "\u201C$quote\u201D",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )
    }
}

@Composable
private fun ClockContent(streakMillis: Long) {
    val p = TimeFormat.parts(streakMillis)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = p.days.toString(),
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = if (p.days == 1L) "DAY" else "DAYS",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 3.sp
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "%02d:%02d:%02d".format(p.hours, p.minutes, p.seconds),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun StatChip(
    icon: ImageVector,
    value: String,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun GraceBanner(remainingMillis: Long, protectedLevel: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Lavender.copy(alpha = 0.10f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Lavender.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Shield,
                contentDescription = null,
                tint = Lavender,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.grace_active),
                    style = MaterialTheme.typography.titleSmall,
                    color = Lavender,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${TimeFormat.shortDuration(remainingMillis)} ${
                        stringResource(R.string.grace_remaining, protectedLevel)
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PanicButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Accent.copy(alpha = 0.14f),
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.Spa,
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.panic_button),
                style = MaterialTheme.typography.titleMedium,
                color = Accent,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun RelapseButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.Refresh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.restart),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold
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

@Composable
private fun OutcomeDialog(outcome: RelapseOutcome, protectedLevel: Int, onDismiss: () -> Unit) {
    val (titleRes, body) = when (outcome) {
        RelapseOutcome.FIRST_FORGIVENESS ->
            R.string.forgiveness_title to stringResource(R.string.forgiveness_body)
        RelapseOutcome.GRACE_STARTED ->
            R.string.grace_started_title to stringResource(R.string.grace_started_body, protectedLevel)
        RelapseOutcome.HARD_LOCK ->
            R.string.hard_lock_title to stringResource(R.string.hard_lock_body)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.got_it)) }
        }
    )
}
