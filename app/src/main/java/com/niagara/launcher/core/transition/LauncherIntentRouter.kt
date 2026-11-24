package com.niagara.launcher.core.transition

import android.content.Intent

/**
 * Resolved UI action when [com.niagara.launcher.MainActivity.onNewIntent] is triggered.
 */
enum class LauncherIntentAction {
    STAY_HOME,
    RETURN_HOME_PAGE,
    OPEN_SETTINGS
}

/**
 * Pure Kotlin routing helper distinguishing Home-button presses from explicit taps on the
 * launcher's own app icon while already on the desktop.
 */
object LauncherIntentRouter {
    fun resolveOnNewIntent(
        hasHomeCategory: Boolean,
        hasLauncherCategory: Boolean,
        isAlreadyActive: Boolean
    ): LauncherIntentAction {
        return when {
            hasHomeCategory -> LauncherIntentAction.RETURN_HOME_PAGE
            hasLauncherCategory && isAlreadyActive -> LauncherIntentAction.OPEN_SETTINGS
            else -> LauncherIntentAction.STAY_HOME
        }
    }

    fun resolveOnNewIntent(intent: Intent?, isAlreadyActive: Boolean): LauncherIntentAction {
        val hasHome = intent?.hasCategory(Intent.CATEGORY_HOME) == true
        val hasLauncher = intent?.hasCategory(Intent.CATEGORY_LAUNCHER) == true
        return resolveOnNewIntent(
            hasHomeCategory = hasHome,
            hasLauncherCategory = hasLauncher,
            isAlreadyActive = isAlreadyActive
        )
    }
}
