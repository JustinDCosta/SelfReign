package com.aldrenstudios.selfreign.ui.dashboard

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aldrenstudios.selfreign.R
import com.aldrenstudios.selfreign.data.Trigger

/**
 * Confirmation dialog for logging a relapse. Captures an optional trigger tag and
 * an optional free-text note describing what triggered it, then reports them back.
 */
@Composable
fun RelapseLogDialog(
    onConfirm: (note: String?, trigger: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var note by remember { mutableStateOf("") }
    var selectedTrigger by remember { mutableStateOf<Trigger?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.log_relapse_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.log_relapse_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.relapse_trigger_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                // Horizontally scrollable trigger chips.
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    Trigger.entries.forEach { trigger ->
                        FilterChip(
                            selected = selectedTrigger == trigger,
                            onClick = {
                                selectedTrigger = if (selectedTrigger == trigger) null else trigger
                            },
                            label = { Text(trigger.label) }
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text(stringResource(R.string.relapse_note_hint)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 88.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(note.takeIf { it.isNotBlank() }, selectedTrigger?.name)
            }) { Text(stringResource(R.string.confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
