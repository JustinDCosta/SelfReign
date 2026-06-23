package com.aldrenstudios.selfreign

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aldrenstudios.selfreign.ui.AppNavigation
import com.aldrenstudios.selfreign.ui.MainViewModel
import com.aldrenstudios.selfreign.ui.celebrate.CelebrationOverlay
import com.aldrenstudios.selfreign.ui.lock.LockScreen
import com.aldrenstudios.selfreign.ui.onboarding.OnboardingScreen
import com.aldrenstudios.selfreign.ui.settings.SettingsViewModel
import com.aldrenstudios.selfreign.ui.theme.SelfReignTheme
import com.aldrenstudios.selfreign.ui.theme.Wallpapers

/**
 * Single-activity host. Applies the calm dark theme + font scale, paints the
 * selected store wallpaper as the global background, and switches between the
 * lock gate, onboarding flow, and the main app based on persisted state.
 *
 * Extends [FragmentActivity] so the BiometricPrompt (app lock) can attach.
 */
class MainActivity : FragmentActivity() {

    // Activity-scoped so all screens share one recovery/feedback/audio instance.
    private val mainViewModel: MainViewModel by viewModels { MainViewModel.Factory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val app = application as HabitApp

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(app.settingsRepository, app)
            )
            val recovery by mainViewModel.state.collectAsStateWithLifecycle()
            val unlocked by mainViewModel.unlocked.collectAsStateWithLifecycle()
            val celebrationLevel by mainViewModel.celebrationLevel.collectAsStateWithLifecycle()

            SelfReignTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Wallpapers.brushFor(recovery.selectedWallpaperId))
                ) {
                    when {
                        // 1. Locked: gate everything behind authentication.
                        recovery.appLockEnabled && !unlocked -> {
                            LockScreen(
                                hasPin = mainViewModel.hasPin(),
                                biometricEnabled = recovery.biometricEnabled,
                                verifyPin = { mainViewModel.verifyPin(it) },
                                onUnlocked = { mainViewModel.unlock() }
                            )
                        }
                        // 2. First run: onboarding.
                        !recovery.onboardingComplete -> {
                            OnboardingScreen(
                                onClick = { mainViewModel.click() },
                                onFinished = { mainViewModel.completeOnboarding() }
                            )
                        }
                        // 3. Main app.
                        else -> {
                            AppNavigation(settingsViewModel, mainViewModel)
                        }
                    }

                    // Level-up celebration floats above whatever is shown.
                    celebrationLevel?.let { level ->
                        CelebrationOverlay(level = level, onDismiss = { mainViewModel.dismissCelebration() })
                    }
                }
            }
        }
    }

    /** Re-lock when the app goes to the background, if app lock is on. */
    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) mainViewModel.lock()
    }
}
