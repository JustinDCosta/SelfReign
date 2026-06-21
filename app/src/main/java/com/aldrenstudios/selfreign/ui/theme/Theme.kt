package com.aldrenstudios.selfreign.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import com.aldrenstudios.selfreign.data.FontSizeOption

/**
 * Applies the calm, AMOLED-first dark theme to the whole app. There is only ever a
 * dark scheme by design (battery-conscious, mindfulness brief). The actual screen
 * background is painted from the user's selected store wallpaper (see MainActivity),
 * so this theme provides the Material color roles and the font scale.
 */
@Composable
fun SelfReignTheme(
    fontSize: FontSizeOption,
    content: @Composable () -> Unit
) {
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
        typography = buildTypography(fontSize.scale),
        content = content
    )
}

/** Convenience used only by @Preview composables. */
@Composable
fun PreviewTheme(content: @Composable () -> Unit) {
    SelfReignTheme(FontSizeOption.MEDIUM, content)
}
