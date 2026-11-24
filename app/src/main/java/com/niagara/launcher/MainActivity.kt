package com.niagara.launcher

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.niagara.launcher.core.data.LocalAppIconCache
import com.niagara.launcher.core.model.HomeWidgetCodec
import com.niagara.launcher.core.theme.NiagaraLauncherTheme
import com.niagara.launcher.core.transition.HomeReturnAnimationGuard
import com.niagara.launcher.core.transition.LauncherIntentAction
import com.niagara.launcher.core.transition.LauncherIntentRouter
import com.niagara.launcher.core.widget.LauncherAppWidgetHost
import com.niagara.launcher.core.widget.SystemWidgetManager
import com.niagara.launcher.core.widget.SystemWidgetProviderEntry
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

    private val appWidgetHost by lazy { LauncherAppWidgetHost(applicationContext) }
    private val appWidgetManager by lazy {
        try {
            AppWidgetManager.getInstance(applicationContext)
        } catch (_: Exception) {
            null
        }
    }

    private var pendingBindWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private var pendingBindProviderInfo: AppWidgetProviderInfo? = null
    private var pendingBindEntry: SystemWidgetProviderEntry? = null

    private val bindWidgetLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val widgetId = pendingBindWidgetId
            val info = pendingBindProviderInfo
            val entry = pendingBindEntry
            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID && info != null && entry != null) {
                handleBoundWidget(widgetId, info, entry)
            } else {
                clearPendingSystemWidget(deleteId = true)
            }
        } else {
            clearPendingSystemWidget(deleteId = true)
        }
    }

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
                        appWidgetHost = appWidgetHost,
                        appWidgetManager = appWidgetManager,
                        onRequestAddSystemWidget = ::requestAddSystemWidget,
                        onDeleteSystemWidgetId = ::deleteSystemWidgetId,
                        onExternalAppLaunched = { returnAnimationGuard.markBackgrounded() }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        isActivityStarted = true
        try {
            appWidgetHost.startListening()
        } catch (_: Exception) {
            // Safe guard if host service unavailable
        }
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
        try {
            appWidgetHost.stopListening()
        } catch (_: Exception) {
            // Safe guard
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CONFIGURE_APPWIDGET) {
            if (resultCode == RESULT_OK) {
                commitPendingSystemWidget()
            } else {
                clearPendingSystemWidget(deleteId = true)
            }
        }
    }

    private fun requestAddSystemWidget(entry: SystemWidgetProviderEntry) {
        if (viewModel.uiState.value.preferences.homeWidgets.size >= HomeWidgetCodec.MAX_HOME_WIDGETS) {
            viewModel.notifyWidgetsFull()
            return
        }

        clearPendingSystemWidget(deleteId = true)

        val info = SystemWidgetManager.findProviderInfo(this, entry.providerPackage, entry.providerClass)
            ?: return
        val manager = appWidgetManager ?: return
        val appWidgetId = try {
            appWidgetHost.allocateAppWidgetId()
        } catch (_: Exception) {
            return
        }

        pendingBindWidgetId = appWidgetId
        pendingBindProviderInfo = info
        pendingBindEntry = entry

        val bound = try {
            manager.bindAppWidgetIdIfAllowed(appWidgetId, info.profile, info.provider, null)
        } catch (_: Exception) {
            false
        }

        if (bound) {
            handleBoundWidget(appWidgetId, info, entry)
        } else {
            try {
                val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.profile)
                }
                bindWidgetLauncher.launch(bindIntent)
            } catch (_: Exception) {
                clearPendingSystemWidget(deleteId = true)
            }
        }
    }

    private fun handleBoundWidget(
        appWidgetId: Int,
        providerInfo: AppWidgetProviderInfo,
        entry: SystemWidgetProviderEntry
    ) {
        if (providerInfo.configure != null) {
            try {
                appWidgetHost.startAppWidgetConfigureActivityForResult(
                    this,
                    appWidgetId,
                    0,
                    REQUEST_CONFIGURE_APPWIDGET,
                    null
                )
                return
            } catch (_: Exception) {
                // Fall back to direct commit if configure activity is inaccessible
            }
        }
        commitPendingSystemWidget(overrideId = appWidgetId, overrideEntry = entry)
    }

    private fun commitPendingSystemWidget(
        overrideId: Int? = null,
        overrideEntry: SystemWidgetProviderEntry? = null
    ) {
        val widgetId = overrideId ?: pendingBindWidgetId
        val entry = overrideEntry ?: pendingBindEntry
        clearPendingSystemWidget(deleteId = false)
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID && entry != null) {
            viewModel.addSystemWidget(
                appWidgetId = widgetId,
                providerPackage = entry.providerPackage,
                providerClass = entry.providerClass,
                label = entry.widgetLabel,
                heightDp = entry.recommendedHeightDp,
                onRejected = { deleteSystemWidgetId(widgetId) }
            )
        }
    }

    private fun clearPendingSystemWidget(deleteId: Boolean) {
        val staleId = pendingBindWidgetId
        pendingBindWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        pendingBindProviderInfo = null
        pendingBindEntry = null
        if (deleteId && staleId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            deleteSystemWidgetId(staleId)
        }
    }

    private fun deleteSystemWidgetId(appWidgetId: Int) {
        try {
            appWidgetHost.deleteAppWidgetId(appWidgetId)
        } catch (_: Exception) {
            // Safe guard
        }
    }

    private fun maybeTriggerHomeReturnTransition() {
        if (returnAnimationGuard.tryConsumeReturnTrigger()) {
            returnToHomeSignal.value = returnToHomeSignal.value + 1
        }
    }

    companion object {
        private const val REQUEST_CONFIGURE_APPWIDGET = 2048
    }
}
