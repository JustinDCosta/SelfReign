package com.aldrenstudios.selfreign.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle

/**
 * Builds a [Typography] scaled by [scale] so the font-size setting can grow or
 * shrink all text consistently while preserving the type hierarchy.
 */
fun buildTypography(scale: Float): Typography {
    val base = Typography()
    fun TextStyle.scaled() = copy(fontSize = fontSize * scale, lineHeight = lineHeight * scale)
    return base.copy(
        displayLarge = base.displayLarge.scaled(),
        displayMedium = base.displayMedium.scaled(),
        displaySmall = base.displaySmall.scaled(),
        headlineLarge = base.headlineLarge.scaled(),
        headlineMedium = base.headlineMedium.scaled(),
        headlineSmall = base.headlineSmall.scaled(),
        titleLarge = base.titleLarge.scaled(),
        titleMedium = base.titleMedium.scaled(),
        titleSmall = base.titleSmall.scaled(),
        bodyLarge = base.bodyLarge.scaled(),
        bodyMedium = base.bodyMedium.scaled(),
        bodySmall = base.bodySmall.scaled(),
        labelLarge = base.labelLarge.scaled(),
        labelMedium = base.labelMedium.scaled(),
        labelSmall = base.labelSmall.scaled()
    )
}
