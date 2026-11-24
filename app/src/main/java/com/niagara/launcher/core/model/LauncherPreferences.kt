package com.niagara.launcher.core.model

/**
 * User configuration and customization settings.
 */
data class LauncherPreferences(
    val favoriteKeys: List<String> = emptyList(),
    val desktopAppKeys: List<String> = emptyList(),
    val hiddenKeys: Set<String> = emptySet(),
    val isRightHanded: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val showBatteryInGlance: Boolean = true
) {
    companion object {
        const val FAVORITES_GRID_COLUMNS = 4
        const val DESKTOP_GRID_COLUMNS = 4
        const val DESKTOP_GRID_ROWS = 6
        const val MAX_DESKTOP_APPS = DESKTOP_GRID_COLUMNS * DESKTOP_GRID_ROWS
    }
}
