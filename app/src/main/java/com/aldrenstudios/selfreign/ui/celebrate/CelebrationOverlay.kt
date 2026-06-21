package com.aldrenstudios.selfreign.ui.celebrate

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aldrenstudios.selfreign.R
import com.aldrenstudios.selfreign.data.Levels
import com.aldrenstudios.selfreign.ui.theme.Accent
import com.aldrenstudios.selfreign.ui.theme.Lavender
import com.aldrenstudios.selfreign.ui.theme.OceanBlue
import kotlin.random.Random

/**
 * Full-screen level-up celebration: a falling confetti burst over a dimmed scrim,
 * with the new level and its title. Shown once when a level is reached.
 */
@Composable
fun CelebrationOverlay(level: Int, onDismiss: () -> Unit) {
    val title = Levels.byNumber(level).title

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f)),
        contentAlignment = Alignment.Center
    ) {
        Confetti()

        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "\uD83C\uDF89",
                style = MaterialTheme.typography.displayMedium
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResourceLevel(level),
                style = MaterialTheme.typography.headlineLarge,
                color = Accent,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.celebrate_body, title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
            Button(onClick = onDismiss) {
                Text(androidx.compose.ui.res.stringResource(R.string.celebrate_cta))
            }
        }
    }
}

@Composable
private fun stringResourceLevel(level: Int): String =
    androidx.compose.ui.res.stringResource(R.string.celebrate_title, level)

private data class Particle(
    val x: Float,
    val color: Color,
    val delay: Float,
    val drift: Float,
    val sizePx: Float
)

@Composable
private fun Confetti() {
    val colors = listOf(Accent, OceanBlue, Lavender, Color(0xFFF4D35E))
    val particles = remember {
        List(60) {
            Particle(
                x = Random.nextFloat(),
                color = colors[Random.nextInt(colors.size)],
                delay = Random.nextFloat() * 0.4f,
                drift = (Random.nextFloat() - 0.5f) * 0.2f,
                sizePx = 8f + Random.nextFloat() * 10f
            )
        }
    }

    // Kick the animation from 0 -> 1 once the overlay appears.
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 2200, easing = LinearEasing),
        label = "fall"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { p ->
            val t = ((progress - p.delay) / (1f - p.delay)).coerceIn(0f, 1f)
            val y = t * size.height * 1.1f
            val x = (p.x + p.drift * t) * size.width
            drawCircle(
                color = p.color.copy(alpha = (1f - t).coerceIn(0f, 1f)),
                radius = p.sizePx,
                center = Offset(x, y)
            )
        }
    }
}
