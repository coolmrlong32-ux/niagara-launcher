package com.niagara.launcher.core.model

import androidx.compose.runtime.Immutable

/**
 * Built-in icon contour and rendering styles supported on the launcher home screen.
 */
enum class IconStyle(
    val storageKey: String,
    val displayName: String,
    val description: String
) {
    IOS_SQUIRCLE(
        storageKey = "ios_squircle",
        displayName = "iOS 圆角矩形",
        description = "经典 iOS 连续曲率圆角"
    ),
    CIRCLE(
        storageKey = "circle",
        displayName = "圆形",
        description = "统一简洁圆形轮廓"
    ),
    ADAPTIVE(
        storageKey = "adaptive",
        displayName = "自适应图标",
        description = "跟随系统原生自适应形态"
    );

    companion object {
        fun fromStorageKey(raw: String?): IconStyle {
            return entries.firstOrNull { it.storageKey == raw } ?: IOS_SQUIRCLE
        }
    }
}

/**
 * Combined visual icon appearance configuration passed down through Compose UI.
 */
@Immutable
data class IconAppearance(
    val iconStyle: IconStyle = IconStyle.IOS_SQUIRCLE,
    val iconPackPackage: String? = null
)

/**
 * Metadata representing an installed third-party Icon Pack discovered on the device.
 */
@Immutable
data class IconPackInfo(
    val packageName: String,
    val label: String
)

/**
 * Pure Kotlin layout calculations for responsive 4-column and 5-column desktop grids.
 */
object DesktopLayoutMath {
    private const val MIN_BOUNDED_ICON_DP = 32f

    fun maxDesktopApps(
        columns: Int,
        rows: Int = LauncherPreferences.DESKTOP_GRID_ROWS
    ): Int {
        return LauncherPreferences.normalizeGridColumns(columns) * rows.coerceAtLeast(1)
    }

    fun resolveIconSizeDp(
        requestedDp: Float,
        availableCellWidthDp: Float? = null,
        badgeOverflowDp: Float = 6f
    ): Float {
        val clamped = LauncherPreferences.clampIconSizeDp(requestedDp)
        if (availableCellWidthDp == null || !availableCellWidthDp.isFinite() || availableCellWidthDp <= 0f) {
            return clamped
        }
        val maxByCell = (availableCellWidthDp - badgeOverflowDp).coerceAtLeast(MIN_BOUNDED_ICON_DP)
        return minOf(clamped, maxByCell)
    }

    fun horizontalSpacingDp(columns: Int): Float = if (columns >= 5) 4f else 8f

    fun labelFontSizeSp(columns: Int): Float = if (columns >= 5) 10.5f else 11.5f
}
