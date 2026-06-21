package com.aldrenstudios.selfreign.ui.lock

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.aldrenstudios.selfreign.R

/**
 * Full-screen lock gate. Offers biometric / device-credential unlock (when
 * available) and/or a PIN fallback. Calls [onUnlocked] when authentication
 * succeeds.
 *
 * @param hasPin       Whether a PIN has been configured (enables the PIN field).
 * @param verifyPin    Validates an entered PIN.
 */
@Composable
fun LockScreen(
    hasPin: Boolean,
    biometricEnabled: Boolean,
    verifyPin: (String) -> Boolean,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    // Attempt biometric / device-credential auth automatically on entry, but only
    // if the user opted in.
    LaunchedEffect(Unit) {
        if (!biometricEnabled) return@LaunchedEffect
        val activity = context as? FragmentActivity ?: return@LaunchedEffect
        val manager = BiometricManager.from(context)
        val authenticators = deviceAuthenticators()
        if (manager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS) {
            val prompt = BiometricPrompt(
                activity,
                androidx.core.content.ContextCompat.getMainExecutor(context),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onUnlocked()
                    }
                }
            )
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(context.getString(R.string.lock_biometric_title))
                .setSubtitle(context.getString(R.string.lock_biometric_subtitle))
                .setAllowedAuthenticators(authenticators)
                .build()
            runCatching { prompt.authenticate(info) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text("\uD83D\uDD12", fontSize = 24.sp)
            }
            Spacer(Modifier.size(20.dp))
            Text(
                text = stringResource(R.string.lock_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )

            if (hasPin) {
                Spacer(Modifier.size(24.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                            pin = it
                            error = false
                        }
                    },
                    label = { Text(stringResource(R.string.lock_pin_prompt)) },
                    singleLine = true,
                    isError = error,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )
                if (error) {
                    Text(
                        text = stringResource(R.string.lock_pin_wrong),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Spacer(Modifier.size(16.dp))
                Button(
                    onClick = {
                        if (verifyPin(pin)) onUnlocked() else { error = true; pin = "" }
                    },
                    enabled = pin.length >= 4
                ) {
                    Text(stringResource(R.string.lock_unlock))
                }
            }
        }
    }
}

/** Allowed authenticators: strong/weak biometrics plus device PIN/pattern/password. */
private fun deviceAuthenticators(): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    } else {
        BiometricManager.Authenticators.BIOMETRIC_WEAK
    }
