package com.aldrenstudios.selfreign.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Dynamic app backgrounds rendered as code-defined gradients. No binary image
 * assets are needed, keeping the APK tiny and the backgrounds resolution-independent.
 *
 * Every wallpaper anchors on true AMOLED black so battery savings and the calm,
 * minimalist aesthetic are preserved; the accent only blooms subtly from one corner.
 */
object Wallpapers {

    /** Returns the Compose [Brush] for a given store wallpaper id. */
    fun brushFor(id: String): Brush = when (id) {
        "wp_sage" -> radialFromCorner(Color(0xFF0E1A14), Accent)
        "wp_ocean" -> radialFromCorner(Color(0xFF0A1622), OceanBlue)
        "wp_lavender" -> radialFromCorner(Color(0xFF150F1E), Lavender)
        "wp_aurora" -> auroraBrush()
        "wp_cosmos" -> cosmosBrush()
        else -> Brush.verticalGradient(listOf(Color.Black, Color.Black)) // wp_black / default
    }

    // A soft accent bloom from the top-left fading into black.
    private fun radialFromCorner(mid: Color, accent: Color): Brush =
        Brush.radialGradient(
            colors = listOf(accent.copy(alpha = 0.16f), mid, Color.Black),
            center = Offset(0f, 0f),
            radius = 1600f
        )

    private fun auroraBrush(): Brush =
        Brush.linearGradient(
            colors = listOf(
                Color.Black,
                Accent.copy(alpha = 0.14f),
                OceanBlue.copy(alpha = 0.12f),
                Color.Black
            )
        )

    private fun cosmosBrush(): Brush =
        Brush.linearGradient(
            colors = listOf(
                Color.Black,
                Lavender.copy(alpha = 0.16f),
                Color(0xFF0A1622),
                Color.Black
            )
        )
}
