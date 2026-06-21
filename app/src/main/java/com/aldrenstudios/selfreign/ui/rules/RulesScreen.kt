package com.aldrenstudios.selfreign.ui.rules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aldrenstudios.selfreign.R
import androidx.compose.ui.res.stringResource

/** A single rule entry rendered as a heading + body. */
private data class Rule(val heading: String, val body: String)

/**
 * The Rulebook: a cleanly formatted, scrollable explanation of the gamification
 * mechanics, grace periods, and rules of engagement.
 */
@Composable
fun RulesScreen() {
    val rules = listOf(
        Rule(
            "Leveling up",
            "Every 24 hours clean adds a day to your streak. Reaching key milestones " +
                "(Day 1, 3, 7, 14, and 30) raises your Level."
        ),
        Rule(
            "Rewards",
            "Each Level unlocks calming app wallpapers and ambient music in the Store. " +
                "Toggle anything you've unlocked on or off whenever you like."
        ),
        Rule(
            "First slip is forgiven",
            "Your first-ever relapse is fully forgiven, one time only. Your streak and " +
                "rewards are untouched so you can keep your momentum."
        ),
        Rule(
            "The grace period",
            "After that, a relapse opens a grace period equal to the time it took to reach " +
                "your current Level (reach Level 4 in 14 days and you get a 14-day grace). " +
                "Your rewards stay unlocked during this window."
        ),
        Rule(
            "Climbing back",
            "Reach your previous Level again before the grace timer ends and the rewards " +
                "become permanently yours once more."
        ),
        Rule(
            "The hard lock",
            "Relapse again during an active grace period, or let the timer run out before " +
                "you regain your Level, and those rewards lock. Earn the days back to restore them."
        ),
        Rule(
            "Your data",
            "Everything is encrypted and stored only on this device. Export a backup anytime, " +
                "and import it to restore. Nothing is ever sent to a server."
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.rules_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        rules.forEachIndexed { index, rule ->
            RuleCard(number = index + 1, rule = rule)
            Spacer(Modifier.height(12.dp))
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun RuleCard(number: Int, rule: Rule) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = rule.heading,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = rule.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
