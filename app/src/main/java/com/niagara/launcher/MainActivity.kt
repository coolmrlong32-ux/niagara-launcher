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
import com.niagara.launcher.ui.home.HomeScreen
import com.niagara.launcher.ui.home.HomeViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels {
        val app = application as NiagaraApplication
        HomeViewModel.Factory(app.appRepository, app.preferencesRepository)
    }

    private val returnAnimationGuard = HomeReturnAnimationGuard()
    private val returnToHomeSignal = MutableStateFlow(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val iconCache = (application as NiagaraApplication).appRepository.iconCache

        setContent {
            val returnPulse by returnToHomeSignal.collectAsState()
            NiagaraLauncherTheme {
                CompositionLocalProvider(LocalAppIconCache provides iconCache) {
                    HomeScreen(
                        viewModel = viewModel,
                        returnToHomePulse = returnPulse,
                        onExternalAppLaunched = { returnAnimationGuard.markBackgrounded() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        maybeTriggerHomeReturnTransition()
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
        returnAnimationGuard.markBackgrounded()
    }

    private fun maybeTriggerHomeReturnTransition() {
        if (returnAnimationGuard.tryConsumeReturnTrigger()) {
            returnToHomeSignal.value = returnToHomeSignal.value + 1
        }
    }
}
