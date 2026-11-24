package com.niagara.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.niagara.launcher.core.data.LocalAppIconCache
import com.niagara.launcher.core.theme.NiagaraLauncherTheme
import com.niagara.launcher.core.transition.HomeReturnAnimationGuard
import com.niagara.launcher.core.transition.LauncherIntentAction
import com.niagara.launcher.core.transition.LauncherIntentRouter
import com.niagara.launcher.ui.home.HomeScreen
import com.niagara.launcher.ui.home.HomeViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels {
        val app = application as NiagaraApplication
        HomeViewModel.Factory(
            appRepository = app.appRepository,
            preferencesRepository = app.preferencesRepository,
            weatherRepository = app.weatherRepository
        )
    }

    private val returnAnimationGuard = HomeReturnAnimationGuard()
    private val returnToHomeSignal = MutableStateFlow(0)
    private val openSettingsSignal = MutableStateFlow(0)
    private val returnToPageZeroSignal = MutableStateFlow(0)
    private var isActivityStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val iconCache = (application as NiagaraApplication).appRepository.iconCache

        setContent {
            val returnPulse by returnToHomeSignal.collectAsState()
            val openSettingsPulse by openSettingsSignal.collectAsState()
            val returnToPageZeroPulse by returnToPageZeroSignal.collectAsState()
            NiagaraLauncherTheme {
                CompositionLocalProvider(LocalAppIconCache provides iconCache) {
                    HomeScreen(
                        viewModel = viewModel,
                        returnToHomePulse = returnPulse,
                        openSettingsPulse = openSettingsPulse,
                        returnToPageZeroPulse = returnToPageZeroPulse,
                        onExternalAppLaunched = { returnAnimationGuard.markBackgrounded() }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        isActivityStarted = true
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        when (LauncherIntentRouter.resolveOnNewIntent(intent, isAlreadyActive = isActivityStarted)) {
            LauncherIntentAction.OPEN_SETTINGS -> {
                openSettingsSignal.value = openSettingsSignal.value + 1
            }
            LauncherIntentAction.RETURN_HOME_PAGE -> {
                returnToPageZeroSignal.value = returnToPageZeroSignal.value + 1
                maybeTriggerHomeReturnTransition()
            }
            LauncherIntentAction.STAY_HOME -> {
                maybeTriggerHomeReturnTransition()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        maybeTriggerHomeReturnTransition()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            maybeTriggerHomeReturnTransition()
        }
    }

    override fun onStop() {
        super.onStop()
        isActivityStarted = false
        returnAnimationGuard.markBackgrounded()
    }

    private fun maybeTriggerHomeReturnTransition() {
        if (returnAnimationGuard.tryConsumeReturnTrigger()) {
            returnToHomeSignal.value = returnToHomeSignal.value + 1
        }
    }
}
