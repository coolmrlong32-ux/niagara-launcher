package com.niagara.launcher.core.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Unified visual design tokens shared across the Home page, App Drawer, Glance, and overlays.
 * Calibrated for high legibility and restrained depth on both bright and dark wallpapers.
 */
@Immutable
data class LauncherVisualTokens(
    val scrimTop: Color = WallpaperScrimTop,
    val scrimMiddle: Color = WallpaperScrimMiddle,
    val scrimBottom: Color = WallpaperScrimBottom,
    val glassTint: Color = IosGlassTintDark,
    val glassFillTop: Color = IosGlassFillTop,
    val glassFillBottom: Color = IosGlassFillBottom,
    val glassBorderTop: Color = IosGlassBorderTop,
    val glassBorderBottom: Color = IosGlassBorderBottom,
    val pillTint: Color = GlassPillTint,
    val pillFill: Color = GlassPillFill,
    val pillBorder: Color = GlassPillBorder,
    val textPrimary: Color = TextPrimaryDark,
    val textSecondary: Color = TextSecondaryDark,
    val textTertiary: Color = TextTertiaryDark,
    val labelShadow: Color = IosLabelShadow,
    val accent: Color = AccentSkyBlue
)

val LocalLauncherVisuals = staticCompositionLocalOf { LauncherVisualTokens() }

private val DarkLauncherVisuals = LauncherVisualTokens()

// In light system theme, wallpapers are often brighter; deepen glass tint slightly while keeping frosted clarity.
private val LightWallpaperAwareVisuals = LauncherVisualTokens(
    scrimTop = Color(0x610A0D16),
    scrimMiddle = Color(0x360C101B),
    scrimBottom = Color(0x6B090C14),
    glassTint = Color(0x78141824),
    glassFillTop = Color(0x2EFFFFFF),
    glassFillBottom = Color(0x14FFFFFF),
    glassBorderTop = Color(0x54FFFFFF),
    glassBorderBottom = Color(0x1CFFFFFF),
    pillTint = Color(0x63131722),
    pillFill = Color(0x21FFFFFF),
    pillBorder = Color(0x38FFFFFF),
    accent = AccentIceBlue
)

private val DarkColorScheme = darkColorScheme(
    primary = AccentSkyBlue,
    secondary = AccentTeal,
    tertiary = AccentWarmGold,
    primaryContainer = Color(0xFF1D293B),
    onPrimaryContainer = Color(0xFFEAF4FF),
    background = Color.Transparent,
    surface = DarkSurface,
    onPrimary = Color(0xFF07131E),
    onSecondary = Color(0xFF061619),
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2B7CE9),
    secondary = Color(0xFF289EB0),
    tertiary = Color(0xFFB7842A),
    primaryContainer = Color(0xFF1D293B),
    onPrimaryContainer = Color(0xFFF0F6FF),
    background = Color.Transparent,
    surface = Color(0xCCFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

@Composable
fun NiagaraLauncherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val launcherVisuals = if (darkTheme) DarkLauncherVisuals else LightWallpaperAwareVisuals

    CompositionLocalProvider(LocalLauncherVisuals provides launcherVisuals) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
