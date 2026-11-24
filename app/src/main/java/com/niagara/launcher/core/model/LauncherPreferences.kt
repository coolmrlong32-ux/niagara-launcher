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
    val showBatteryInGlance: Boolean = true,
    val showDeviceStatusInGlance: Boolean = true,
    val showWeatherInGlance: Boolean = true,
    val weatherLocationQuery: String = "",
    val weatherResolvedQuery: String = "",
    val cachedWeather: WeatherSnapshot? = null,
    val desktopIconSizeDp: Float = DEFAULT_ICON_SIZE_DP,
    val desktopGridColumns: Int = DESKTOP_GRID_COLUMNS,
    val showAppLabels: Boolean = true,
    val showFavoritesBar: Boolean = true,
    val iconStyle: IconStyle = IconStyle.IOS_SQUIRCLE,
    val iconPackPackage: String? = null
) {
    val maxDesktopApps: Int
        get() = normalizeGridColumns(desktopGridColumns) * DESKTOP_GRID_ROWS

    val iconAppearance: IconAppearance
        get() = IconAppearance(iconStyle = iconStyle, iconPackPackage = iconPackPackage)

    companion object {
        const val FAVORITES_GRID_COLUMNS = 4
        const val DESKTOP_GRID_COLUMNS = 4
        const val DESKTOP_GRID_COLUMNS_COMPACT = 5
        const val DESKTOP_GRID_ROWS = 6
        const val MAX_DESKTOP_APPS = DESKTOP_GRID_COLUMNS * DESKTOP_GRID_ROWS
        const val MAX_DESKTOP_APPS_SUPPORTED = DESKTOP_GRID_COLUMNS_COMPACT * DESKTOP_GRID_ROWS

        const val MIN_ICON_SIZE_DP = 42f
        const val DEFAULT_ICON_SIZE_DP = 56f
        const val MAX_ICON_SIZE_DP = 64f

        fun normalizeGridColumns(columns: Int): Int {
            return if (columns == DESKTOP_GRID_COLUMNS_COMPACT) {
                DESKTOP_GRID_COLUMNS_COMPACT
            } else {
                DESKTOP_GRID_COLUMNS
            }
        }

        fun clampIconSizeDp(sizeDp: Float): Float {
            return if (sizeDp.isFinite()) {
                sizeDp.coerceIn(MIN_ICON_SIZE_DP, MAX_ICON_SIZE_DP)
            } else {
                DEFAULT_ICON_SIZE_DP
            }
        }
    }
}
