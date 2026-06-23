package com.aldrenstudios.selfreign.ui.store

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aldrenstudios.selfreign.R
import com.aldrenstudios.selfreign.data.StoreCatalog
import com.aldrenstudios.selfreign.data.StoreItem
import com.aldrenstudios.selfreign.ui.MainViewModel
import com.aldrenstudios.selfreign.ui.theme.Accent
import com.aldrenstudios.selfreign.ui.theme.Wallpapers

/**
 * The Store: lists unlockable wallpapers with their unlock requirements. Unlocked
 * items can be applied; items unlocked only because grace is shielding them are
 * marked temporary.
 */
@Composable
fun StoreScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val effectiveLevel by viewModel.effectiveLevel.collectAsStateWithLifecycle()
    val organicLevel by viewModel.organicLevel.collectAsStateWithLifecycle()
    val graceShielding by viewModel.graceShielding.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        item { ScreenTitle(stringResource(R.string.nav_store)) }
        item { SectionHeader(stringResource(R.string.store_wallpapers)) }

        items(StoreCatalog.wallpapers, key = { it.id }) { item ->
            val unlocked = item.requiredLevel <= effectiveLevel
            StoreItemCard(
                item = item,
                unlocked = unlocked,
                applied = state.selectedWallpaperId == item.id,
                // Only "temporary" if it's actually accessible right now *because* of
                // grace (i.e. unlocked, but above the organically-earned level).
                graceTemp = unlocked && graceShielding && item.requiredLevel > organicLevel,
                leading = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Wallpapers.brushFor(item.id))
                    )
                },
                onApply = { viewModel.selectWallpaper(item.id) }
            )
        }
    }
}

@Composable
private fun ScreenTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun StoreItemCard(
    item: StoreItem,
    unlocked: Boolean,
    applied: Boolean,
    graceTemp: Boolean,
    leading: @Composable () -> Unit,
    onApply: () -> Unit
) {
    Surface(
        onClick = { if (unlocked && !applied) onApply() },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = if (applied) BorderStroke(1.dp, Accent.copy(alpha = 0.6f)) else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.alpha(if (unlocked) 1f else 0.5f)) { leading() }
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (unlocked) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Subtitle: grace note for temporarily-unlocked items, or the unlock
                // requirement for locked ones. Permanently-unlocked items show nothing.
                val subtitle = when {
                    graceTemp -> stringResource(R.string.store_grace_temp)
                    !unlocked -> stringResource(R.string.store_unlocks_at, item.requiredLevel, item.requiredDays)
                    else -> null
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (graceTemp) com.aldrenstudios.selfreign.ui.theme.Lavender
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            when {
                applied -> Icon(Icons.Filled.CheckCircle, contentDescription = stringResource(R.string.store_applied), tint = Accent)
                !unlocked -> Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> Text(
                    stringResource(R.string.store_apply),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
