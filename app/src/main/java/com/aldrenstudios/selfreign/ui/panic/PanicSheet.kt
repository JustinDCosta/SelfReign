package com.aldrenstudios.selfreign.ui.panic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aldrenstudios.selfreign.R
import com.aldrenstudios.selfreign.ui.theme.Accent
import com.aldrenstudios.selfreign.ui.theme.OceanBlue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The three phases of a box-breathing-style cycle, each with a duration. */
private enum class BreathPhase(val labelRes: Int, val seconds: Int) {
    INHALE(R.string.panic_breathe_in, 4),
    HOLD(R.string.panic_hold, 4),
    EXHALE(R.string.panic_breathe_out, 4)
}

// Circle scale at the fully-exhaled (small) and fully-inhaled (large) extremes.
private const val EXHALE_SCALE = 0.55f
private const val INHALE_SCALE = 1f

/**
 * Urge-surfing helper: a guided breathing exercise shown when the user taps the
 * "I'm craving" button. A circle expands on the inhale, stays expanded on the hold,
 * and contracts on the exhale, cycling inhale -> hold -> exhale repeatedly. A
 * countdown shows the seconds remaining in the current phase, synced to the animation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanicSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.panic_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.panic_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(36.dp))

            BreathingGuide()

            Spacer(Modifier.height(36.dp))
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.panic_done))
            }
        }
    }
}

/**
 * Drives the breath phase state machine. The circle starts fully exhaled (small)
 * so the very first inhale visibly expands it. Each phase animates the scale to its
 * target over the phase duration while a 1 Hz countdown ticks in parallel, then it
 * advances: inhale -> hold -> exhale, looping.
 */
@Composable
private fun BreathingGuide() {
    val scale = remember { Animatable(EXHALE_SCALE) }
    var phase by remember { mutableStateOf(BreathPhase.INHALE) }
    var remaining by remember { mutableIntStateOf(BreathPhase.INHALE.seconds) }

    LaunchedEffect(Unit) {
        scale.snapTo(EXHALE_SCALE)
        while (true) {
            for (p in BreathPhase.entries) {
                phase = p
                remaining = p.seconds
                // Count the seconds down in parallel with the scale animation.
                val ticker = launch {
                    while (remaining > 1) {
                        delay(1000)
                        remaining--
                    }
                }
                when (p) {
                    BreathPhase.INHALE ->
                        scale.animateTo(INHALE_SCALE, tween(p.seconds * 1000, easing = LinearEasing))
                    BreathPhase.HOLD ->
                        delay(p.seconds * 1000L)
                    BreathPhase.EXHALE ->
                        scale.animateTo(EXHALE_SCALE, tween(p.seconds * 1000, easing = LinearEasing))
                }
                ticker.cancel()
            }
        }
    }

    Box(
        modifier = Modifier.size(260.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .scale(scale.value)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Accent.copy(alpha = 0.35f), OceanBlue.copy(alpha = 0.10f))
                    )
                )
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(phase.labelRes),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = remaining.toString(),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Accent
            )
        }
    }
}
