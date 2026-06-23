package com.aldrenstudios.selfreign.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aldrenstudios.selfreign.R
import com.aldrenstudios.selfreign.ui.dashboard.DashboardScreen
import com.aldrenstudios.selfreign.ui.history.HistoryScreen
import com.aldrenstudios.selfreign.ui.insights.InsightsScreen
import com.aldrenstudios.selfreign.ui.rules.RulesScreen
import com.aldrenstudios.selfreign.ui.settings.SettingsScreen
import com.aldrenstudios.selfreign.ui.settings.SettingsViewModel
import com.aldrenstudios.selfreign.ui.store.StoreScreen

/** Sub-page route (not part of the bottom navigation bar). */
private const val HISTORY_ROUTE = "history"

/** The five top-level destinations. */
private enum class Dest(val route: String, val labelRes: Int, val icon: ImageVector) {
    DASHBOARD("dashboard", R.string.nav_home, Icons.Filled.Home),
    INSIGHTS("insights", R.string.nav_insights, Icons.Filled.Insights),
    STORE("store", R.string.nav_store, Icons.Filled.Store),
    RULES("rules", R.string.nav_rules, Icons.AutoMirrored.Filled.MenuBook),
    SETTINGS("settings", R.string.nav_settings, Icons.Filled.Settings)
}

/**
 * Hosts the bottom navigation bar and the navigation graph. The shared
 * [MainViewModel] drives the dashboard, store, and settings; the [SettingsViewModel]
 * (provided by the activity) supplies the lightweight DataStore-backed prefs.
 */
@Composable
fun AppNavigation(settingsViewModel: SettingsViewModel, mainViewModel: MainViewModel) {
    val navController = rememberNavController()

    Scaffold(
        containerColor = Color.Transparent, // let the wallpaper show through
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                Dest.values().forEach { dest ->
                    val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            mainViewModel.click()
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = null) },
                        label = { Text(stringResource(dest.labelRes)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Dest.DASHBOARD.route,
            modifier = Modifier.padding(innerPadding),
            // Smooth fade-through with a subtle scale for a calm, premium feel.
            enterTransition = { fadeIn(tween(220)) + scaleIn(initialScale = 0.98f, animationSpec = tween(220)) },
            exitTransition = { fadeOut(tween(150)) },
            popEnterTransition = { fadeIn(tween(220)) + scaleIn(initialScale = 0.98f, animationSpec = tween(220)) },
            popExitTransition = { fadeOut(tween(150)) }
        ) {
            composable(Dest.DASHBOARD.route) {
                DashboardScreen(
                    viewModel = mainViewModel,
                    onOpenHistory = { navController.navigate(HISTORY_ROUTE) }
                )
            }
            composable(Dest.INSIGHTS.route) {
                InsightsScreen(
                    viewModel = mainViewModel,
                    onOpenHistory = { navController.navigate(HISTORY_ROUTE) }
                )
            }
            composable(Dest.STORE.route) { StoreScreen(mainViewModel) }
            composable(Dest.RULES.route) { RulesScreen() }
            composable(Dest.SETTINGS.route) { SettingsScreen(settingsViewModel, mainViewModel) }
            composable(HISTORY_ROUTE) {
                HistoryScreen(mainViewModel, onBack = { navController.popBackStack() })
            }
        }
    }
}
