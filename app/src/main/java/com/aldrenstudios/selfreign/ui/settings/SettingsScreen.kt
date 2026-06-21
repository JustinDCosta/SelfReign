package com.aldrenstudios.selfreign.ui.settings

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aldrenstudios.selfreign.R
import com.aldrenstudios.selfreign.data.FontSizeOption
import com.aldrenstudios.selfreign.ui.MainViewModel
import com.aldrenstudios.selfreign.util.BackupIo

/**
 * Settings: font size, feedback (sounds/haptics), background music, reminders,
 * encrypted backup export/import, and a privacy statement.
 *
 * Uses two ViewModels: [SettingsViewModel] for the lightweight DataStore prefs
 * (font, reminders) and the shared [MainViewModel] for recovery-state-backed
 * toggles (feedback, music) and the backup bridge.
 */
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    mainViewModel: MainViewModel
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val recovery by mainViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result handled implicitly */ }

    // Export: create a document and write the backup JSON into it.
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val ok = BackupIo.writeToUri(context, uri, mainViewModel.exportJson())
            Toast.makeText(
                context,
                if (ok) "Backup saved." else "Export failed.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Import: pick a JSON file, read it, validate + apply.
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val raw = BackupIo.readFromUri(context, uri)
            val error = if (raw == null) "Could not read file." else mainViewModel.importJson(raw)
            Toast.makeText(
                context,
                error ?: context.getString(R.string.import_success),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = stringResource(R.string.nav_settings),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )

        // --- Font size ---
        SettingsSection(title = stringResource(R.string.settings_font_size)) {
            FontSizeOption.values().forEach { option ->
                RadioRow(
                    label = option.displayName(),
                    selected = settings.fontSize == option,
                    onClick = { settingsViewModel.setFontSize(option) }
                )
            }
        }

        // --- Feedback (sounds + haptics) ---
        SettingsSection(title = stringResource(R.string.settings_feedback)) {
            ToggleRow(
                label = stringResource(R.string.settings_sounds),
                checked = recovery.soundsEnabled,
                onCheckedChange = { mainViewModel.setSoundsEnabled(it) }
            )
            ToggleRow(
                label = stringResource(R.string.settings_haptics),
                checked = recovery.hapticsEnabled,
                onCheckedChange = { mainViewModel.setHapticsEnabled(it) }
            )
            ToggleRow(
                label = stringResource(R.string.settings_music),
                checked = recovery.musicEnabled,
                onCheckedChange = { mainViewModel.setMusicEnabled(it) }
            )
        }

        // --- Notifications ---
        SettingsSection(title = stringResource(R.string.settings_notifications)) {
            ToggleRow(
                label = stringResource(R.string.settings_reminders),
                checked = settings.remindersEnabled,
                onCheckedChange = { enabled ->
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                    settingsViewModel.setReminders(enabled)
                }
            )
        }

        // --- Backup & restore ---
        SettingsSection(title = stringResource(R.string.settings_data)) {
            OutlinedButton(
                onClick = {
                    mainViewModel.click()
                    exportLauncher.launch("reclaim-backup.json")
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.settings_export)) }

            OutlinedButton(
                onClick = {
                    mainViewModel.click()
                    importLauncher.launch("application/json")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) { Text(stringResource(R.string.settings_import)) }
        }

        // --- Milestones (custom day thresholds) ---
        SettingsSection(title = stringResource(R.string.settings_milestones)) {
            MilestoneEditor(
                thresholds = recovery.levelThresholds,
                onSave = { mainViewModel.setLevelThresholds(it) },
                onReset = { mainViewModel.resetLevelThresholds() }
            )
        }

        // --- Money saved ---
        SettingsSection(title = stringResource(R.string.settings_money)) {
            MoneyEditor(
                costPerDayCents = recovery.costPerDayCents,
                currency = recovery.currencySymbol,
                onSave = { cents, cur -> mainViewModel.setCostPerDay(cents, cur) }
            )
        }

        // --- Security / app lock ---
        SettingsSection(title = stringResource(R.string.settings_security)) {
            SecurityEditor(
                lockEnabled = recovery.appLockEnabled,
                biometricEnabled = recovery.biometricEnabled,
                hasPin = recovery.pinHash != null,
                onSetPin = { pin -> mainViewModel.setPin(pin) },
                onDisable = { mainViewModel.disableAppLock() },
                onBiometricChange = { mainViewModel.setBiometricEnabled(it) }
            )
        }

        // --- Privacy ---
        SettingsSection(title = stringResource(R.string.settings_privacy)) {
            Text(
                text = stringResource(R.string.settings_privacy_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Editor for the six custom milestone day-thresholds (level 0 is fixed at 0). */
@Composable
private fun MilestoneEditor(
    thresholds: List<Int>,
    onSave: (List<Int>) -> Unit,
    onReset: () -> Unit
) {
    // Editable text per level (skip level 0 which is always 0).
    val fields = remember(thresholds) {
        mutableStateListOf<String>().apply {
            thresholds.forEachIndexed { i, v -> if (i > 0) add(v.toString()) }
        }
    }
    Column {
        fields.forEachIndexed { idx, value ->
            val level = idx + 1
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Level $level",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { new ->
                        if (new.length <= 4 && new.all { it.isDigit() }) fields[idx] = new
                    },
                    singleLine = true,
                    suffix = { Text("d") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(110.dp)
                )
            }
        }
        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    val parsed = listOf(0) + fields.map { it.toIntOrNull() ?: 0 }
                    onSave(parsed)
                },
                modifier = Modifier.weight(1f)
            ) { Text(stringResource(R.string.settings_save)) }
            OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_milestones_reset))
            }
        }
    }
}

/** Editor for the estimated daily cost (money-saved feature). */
@Composable
private fun MoneyEditor(
    costPerDayCents: Int,
    currency: String,
    onSave: (cents: Int, currency: String) -> Unit
) {
    var amount by remember(costPerDayCents) {
        mutableStateOf(if (costPerDayCents > 0) (costPerDayCents / 100.0).toString() else "")
    }
    var symbol by remember(currency) { mutableStateOf(currency) }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = symbol,
                onValueChange = { if (it.length <= 3) symbol = it },
                label = { Text(stringResource(R.string.settings_currency)) },
                singleLine = true,
                modifier = Modifier.width(96.dp)
            )
            OutlinedTextField(
                value = amount,
                onValueChange = { new ->
                    // Guard against overflow: cap length and allow a single decimal point.
                    if (new.length <= 7 && new.count { it == '.' } <= 1 && new.all { it.isDigit() || it == '.' }) {
                        amount = new
                    }
                },
                label = { Text(stringResource(R.string.settings_cost_per_day)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }
        OutlinedButton(
            onClick = {
                // Clamp to a sane range so the stored cents value can never overflow Int.
                val dollars = (amount.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100_000.0)
                val cents = (dollars * 100).toInt()
                onSave(cents, symbol.ifBlank { "$" })
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) { Text(stringResource(R.string.settings_save)) }
    }
}

/** Editor for the optional app lock with an optional PIN. */
@Composable
private fun SecurityEditor(
    lockEnabled: Boolean,
    biometricEnabled: Boolean,
    hasPin: Boolean,
    onSetPin: (pin: String) -> Unit,
    onDisable: () -> Unit,
    onBiometricChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    // When the lock toggle is turned on, reveal the PIN entry until a PIN exists.
    var showPinEntry by remember { mutableStateOf(false) }

    Column {
        // 1. Require PIN to unlock
        ToggleRow(
            label = stringResource(R.string.settings_require_pin),
            checked = lockEnabled,
            onCheckedChange = { enabled ->
                if (enabled) {
                    showPinEntry = true
                } else {
                    onDisable()
                    showPinEntry = false
                    pin = ""
                }
            }
        )

        // 2. PIN entry: shown while enabling (no PIN yet) or when changing it.
        if (showPinEntry || (lockEnabled && !hasPin)) {
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pin = it },
                label = { Text(if (hasPin) stringResource(R.string.settings_change_pin) else stringResource(R.string.settings_set_pin)) },
                placeholder = { Text(stringResource(R.string.settings_pin_hint)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
            OutlinedButton(
                onClick = {
                    if (pin.length >= 4) {
                        onSetPin(pin)
                        pin = ""
                        showPinEntry = false
                        Toast.makeText(context, context.getString(R.string.settings_pin_saved), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, context.getString(R.string.settings_pin_too_short), Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) { Text(stringResource(R.string.settings_save)) }
        } else if (lockEnabled && hasPin) {
            // Offer changing the PIN without re-enabling.
            OutlinedButton(
                onClick = { showPinEntry = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) { Text(stringResource(R.string.settings_change_pin)) }
        }

        // 3. Biometric add-on, only relevant once a PIN lock is active.
        if (lockEnabled && hasPin) {
            ToggleRow(
                label = stringResource(R.string.settings_use_biometric),
                checked = biometricEnabled,
                onCheckedChange = onBiometricChange
            )
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

private fun FontSizeOption.displayName(): String = when (this) {
    FontSizeOption.SMALL -> "Small"
    FontSizeOption.MEDIUM -> "Medium"
    FontSizeOption.LARGE -> "Large"
}
