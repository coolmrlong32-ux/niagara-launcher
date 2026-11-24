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
    val glassHighlightTop: Color = IosGlassHighlightTop,
    val cardShadowAmbient: Color = CardShadowAmbient,
    val cardShadowSpot: Color = CardShadowSpot,
    val dockSurface: Color = DockSurfaceDark,
    val dockBorder: Color = DockBorderDark,
    val pillTint: Color = GlassPillTint,
    val pillFill: Color = GlassPillFill,
    val pillBorder: Color = GlassPillBorder,
    val textPrimary: Color = TextPrimaryDark,
    val textSecondary: Color = TextSecondaryDark,
    val textTertiary: Color = TextTertiaryDark,
    val clockColonTint: Color = ClockColonTintDark,
    val clockDateSecondary: Color = ClockDateSecondaryDark,
    val labelShadow: Color = IosLabelShadow,
    val accent: Color = AccentSkyBlue
)

val LocalLauncherVisuals = staticCompositionLocalOf { LauncherVisualTokens() }

private val DarkLauncherVisuals = LauncherVisualTokens()

// In light system theme, wallpapers are often brighter; deepen glass tint slightly while keeping frosted clarity.
private val LightWallpaperAwareVisuals = LauncherVisualTokens(
    scrimTop = Color(0x660A0D16),
    scrimMiddle = Color(0x3B0C101B),
    scrimBottom = Color(0x70090C14),
    glassTint = Color(0x82131724),
    glassFillTop = Color(0x30FFFFFF),
    glassFillBottom = Color(0x16FFFFFF),
    glassBorderTop = Color(0x59FFFFFF),
    glassBorderBottom = Color(0x21FFFFFF),
    glassHighlightTop = Color(0x3DFFFFFF),
    cardShadowAmbient = Color(0x54000000),
    cardShadowSpot = Color(0x78000000),
    dockSurface = Color(0x94131724),
    dockBorder = Color(0x42FFFFFF),
    pillTint = Color(0x6B131722),
    pillFill = Color(0x24FFFFFF),
    pillBorder = Color(0x3DFFFFFF),
    clockColonTint = Color(0xD979C2FF),
    clockDateSecondary = Color(0xD4E8EEF8),
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
