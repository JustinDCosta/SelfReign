package com.aldrenstudios.selfreign.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Applies the calm, AMOLED-first dark theme to the whole app. There is only ever a
 * dark scheme by design (battery-conscious, mindfulness brief). The actual screen
 * background is painted from the user's selected store wallpaper (see MainActivity).
 *
 * Text uses a single typography in `sp`, so it automatically respects the device's
 * own font-size / accessibility setting — there is no in-app font option.
 */
private val AppTypography = Typography()

@Composable
fun SelfReignTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = Accent,
        onPrimary = AmoledBackground,
        primaryContainer = AccentDark,
        secondary = OceanBlue,
        tertiary = Lavender,
        error = Danger,
        background = AmoledBackground,
        onBackground = OnDarkText,
        surface = AmoledSurface,
        onSurface = OnDarkText,
        surfaceVariant = GraySurface,
        onSurfaceVariant = OnDarkSubtle
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
