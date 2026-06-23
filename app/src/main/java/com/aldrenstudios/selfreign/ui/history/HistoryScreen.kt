package com.aldrenstudios.selfreign.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aldrenstudios.selfreign.R
import com.aldrenstudios.selfreign.data.RelapseLogEntry
import com.aldrenstudios.selfreign.data.RelapseOutcome
import com.aldrenstudios.selfreign.ui.MainViewModel
import com.aldrenstudios.selfreign.ui.theme.Accent
import com.aldrenstudios.selfreign.ui.theme.Danger
import com.aldrenstudios.selfreign.ui.theme.Lavender
import com.aldrenstudios.selfreign.util.TimeFormat

/**
 * Full-screen relapse history. Reached from the dashboard's "Total relapses"
 * chip and the Insights "Total relapses" card. Replaces the old bottom sheet so
 * the log gets a dedicated page with its own back navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val entries = state.relapseLog.sortedByDescending { it.timestamp }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.history_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
            ) {
                items(entries, key = { it.timestamp }) { entry -> HistoryItem(entry) }
            }
        }
    }
}

@Composable
private fun HistoryItem(entry: RelapseLogEntry) {
    val (label, accent) = outcomeLabel(entry.outcome)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
        // Optional trigger + note details, when present.
        val details = listOfNotNull(
            entry.trigger?.let { com.aldrenstudios.selfreign.data.Trigger.fromName(it)?.label },
            entry.note?.takeIf { it.isNotBlank() }
        )
        if (details.isNotEmpty()) {
            Spacer(Modifier.size(6.dp))
            Text(
                text = details.joinToString(" \u00b7 "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp)
            )
        }
    }
}

/** Maps a stored outcome name to a friendly label + accent color. */
@Composable
private fun outcomeLabel(outcome: String): Pair<String, Color> = when (outcome) {
    RelapseOutcome.FIRST_FORGIVENESS.name -> stringResource(R.string.outcome_first_forgiveness) to Accent
    RelapseOutcome.GRACE_STARTED.name -> stringResource(R.string.outcome_grace_started) to Lavender
    else -> stringResource(R.string.outcome_hard_lock) to Danger
}
