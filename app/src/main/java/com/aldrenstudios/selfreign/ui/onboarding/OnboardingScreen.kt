package com.aldrenstudios.selfreign.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aldrenstudios.selfreign.R

private data class OnbPage(val titleRes: Int, val bodyRes: Int)

/**
 * Two-phase onboarding:
 *  1. A black "Welcome" screen that fades in with a single elegant CTA.
 *  2. A 3-step paginated walkthrough that slides/fades between pages.
 *
 * Calls [onFinished] when the user completes (or skips) the walkthrough.
 */
@Composable
fun OnboardingScreen(onClick: () -> Unit, onFinished: () -> Unit) {
    var started by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        // Phase 1: Welcome + CTA
        AnimatedVisibility(
            visible = !started,
            enter = fadeIn(tween(900)),
            exit = fadeOut(tween(400))
        ) {
            WelcomePane(onCta = {
                onClick()
                started = true
            })
        }

        // Phase 2: Walkthrough carousel
        AnimatedVisibility(
            visible = started,
            enter = fadeIn(tween(500, delayMillis = 200))
        ) {
            Walkthrough(onClick = onClick, onFinished = onFinished)
        }
    }
}

@Composable
private fun WelcomePane(onCta: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)
    ) {
        // Small accent mark for a calm, branded touch.
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(R.string.welcome),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 44.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 4.sp
        )
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onCta,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black
            ),
            modifier = Modifier.height(54.dp)
        ) {
            Text(
                text = stringResource(R.string.reclaim_cta),
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
private fun Walkthrough(onClick: () -> Unit, onFinished: () -> Unit) {
    val pages = listOf(
        OnbPage(R.string.onb_1_title, R.string.onb_1_body),
        OnbPage(R.string.onb_2_title, R.string.onb_2_body),
        OnbPage(R.string.onb_3_title, R.string.onb_3_body)
    )
    var index by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Skip
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { onClick(); onFinished() }) {
                Text(stringResource(R.string.onboarding_skip), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Animated page content
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            AnimatedContentPage(index, pages)
        }

        // Indicators + advance button
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pages.indices.forEach { i ->
                    val active = i == index
                    Box(
                        modifier = Modifier
                            .size(if (active) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    onClick()
                    if (index < pages.lastIndex) index++ else onFinished()
                },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = stringResource(
                        if (index < pages.lastIndex) R.string.onboarding_next
                        else R.string.onboarding_start
                    ),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun AnimatedContentPage(index: Int, pages: List<OnbPage>) {
    androidx.compose.animation.AnimatedContent(
        targetState = index,
        transitionSpec = {
            (slideInHorizontally(tween(400, easing = LinearOutSlowInEasing)) { it / 3 } + fadeIn(tween(400)))
                .togetherWith(slideOutHorizontally(tween(300)) { -it / 3 } + fadeOut(tween(300)))
        },
        label = "onboardingPage"
    ) { i ->
        val page = pages[i]
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(page.titleRes),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(page.bodyRes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
        }
    }
}
