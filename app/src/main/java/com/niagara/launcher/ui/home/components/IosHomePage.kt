package com.niagara.launcher.ui.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagara.launcher.core.model.AppItem
import com.niagara.launcher.core.model.DeviceStatusSnapshot
import com.niagara.launcher.core.model.LauncherPreferences
import com.niagara.launcher.core.theme.LocalLauncherVisuals
import com.niagara.launcher.core.transition.LaunchSourceBounds

/**
 * Independent iOS-style Home page featuring a refined translucent Glance header
 * (time, weather, real-time RAM & battery temperature), Frosted Glass Favorites Card,
 * and configurable 4x6 / 5x6 drag-reorderable Desktop App Grid.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IosHomePage(
    favorites: List<AppItem>,
    desktopApps: List<AppItem>,
    isEditMode: Boolean,
    preferences: LauncherPreferences,
    deviceStatus: DeviceStatusSnapshot?,
    isWeatherRefreshing: Boolean,
    onWeatherClick: () -> Unit,
    onWeatherLongClick: () -> Unit,
    onLaunchApp: (AppItem, LaunchSourceBounds?) -> Unit,
    onEnterEditMode: () -> Unit,
    onExitEditMode: () -> Unit,
    onOpenDesktopMenu: () -> Unit,
    onOpenDesktopSettings: () -> Unit,
    onRemoveFavorite: (AppItem) -> Unit,
    onRemoveDesktopApp: (AppItem) -> Unit,
    onReorderFavorites: (List<String>) -> Unit,
    onReorderDesktopApps: (List<String>) -> Unit,
    onDraggingChanged: (Boolean) -> Unit,
    onOpenFavoritesPicker: () -> Unit,
    onOpenDesktopPicker: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()
    var isGridDragging by remember { mutableStateOf(false) }

    val handleDraggingChanged: (Boolean) -> Unit = { dragging ->
        isGridDragging = dragging
        onDraggingChanged(dragging)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (isEditMode) {
                        onExitEditMode()
                    }
                },
                onLongClick = {
                    if (!isEditMode && !isGridDragging) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenDesktopMenu()
                    }
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState, enabled = !isGridDragging)
                .padding(horizontal = 16.dp)
                .padding(top = 14.dp, bottom = 76.dp)
        ) {
            if (isEditMode) {
                IosEditModeHeader(
                    desktopCount = desktopApps.size,
                    maxDesktopCount = preferences.maxDesktopApps,
                    onAddClick = onOpenDesktopPicker,
                    onDoneClick = onExitEditMode
                )
            } else {
                IosNormalHeader(
                    preferences = preferences,
                    deviceStatus = deviceStatus,
                    isWeatherRefreshing = isWeatherRefreshing,
                    onWeatherClick = onWeatherClick,
                    onWeatherLongClick = onWeatherLongClick,
                    onSearchClick = onSearchClick,
                    onSettingsClick = onOpenDesktopSettings
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            AnimatedVisibility(
                visible = preferences.showFavoritesBar,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    IosFavoritesCard(
                        favorites = favorites,
                        isEditMode = isEditMode,
                        onLaunchApp = onLaunchApp,
                        onEnterEditMode = onEnterEditMode,
                        onRemoveFavorite = onRemoveFavorite,
                        onOpenFavoritesPicker = onOpenFavoritesPicker,
                        onReorderFavorites = onReorderFavorites,
                        onDraggingChanged = handleDraggingChanged,
                        gridColumns = preferences.desktopGridColumns,
                        iconSizeDp = preferences.desktopIconSizeDp,
                        showAppLabels = preferences.showAppLabels,
                        iconAppearance = preferences.iconAppearance
                    )

                    Spacer(modifier = Modifier.height(22.dp))
                }
            }

            IosDesktopGrid(
                desktopApps = desktopApps,
                isEditMode = isEditMode,
                onLaunchApp = onLaunchApp,
                onEnterEditMode = onEnterEditMode,
                onOpenDesktopMenu = onOpenDesktopMenu,
                onRemoveDesktopApp = onRemoveDesktopApp,
                onOpenDesktopPicker = onOpenDesktopPicker,
                onReorderDesktopApps = onReorderDesktopApps,
                onDraggingChanged = handleDraggingChanged,
                gridColumns = preferences.desktopGridColumns,
                maxDesktopApps = preferences.maxDesktopApps,
                iconSizeDp = preferences.desktopIconSizeDp,
                showAppLabels = preferences.showAppLabels,
                iconAppearance = preferences.iconAppearance
            )
        }
    }
}

@Composable
private fun IosEditModeHeader(
    desktopCount: Int,
    maxDesktopCount: Int,
    onAddClick: () -> Unit,
    onDoneClick: () -> Unit
) {
    val visuals = LocalLauncherVisuals.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IosGlassPill(
            onClick = onAddClick,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "添加桌面应用",
                tint = visuals.textPrimary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "添加",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = visuals.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        Text(
            text = "拖动排序 ($desktopCount/$maxDesktopCount)",
            style = MaterialTheme.typography.labelSmall.copy(
                color = visuals.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                shadow = Shadow(color = visuals.labelShadow, offset = Offset(0f, 1f), blurRadius = 3f)
            )
        )

        IosGlassPill(
            onClick = onDoneClick,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = "完成",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = visuals.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
private fun IosNormalHeader(
    preferences: LauncherPreferences,
    deviceStatus: DeviceStatusSnapshot?,
    isWeatherRefreshing: Boolean,
    onWeatherClick: () -> Unit,
    onWeatherLongClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val visuals = LocalLauncherVisuals.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LauncherClockDisplay(
                variant = ClockVariant.HERO_HOME,
                modifier = Modifier.weight(1f)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IosGlassPill(
                    onClick = onSearchClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "搜索",
                        tint = visuals.textPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "搜索",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = visuals.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                IosGlassPill(
                    shape = CircleShape,
                    onClick = onSettingsClick,
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = "启动器设置",
                        tint = visuals.textPrimary.copy(alpha = 0.92f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Glance weather & real-time phone status row
        if (preferences.showWeatherInGlance || preferences.showDeviceStatusInGlance || preferences.showBatteryInGlance) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (preferences.showWeatherInGlance) {
                    GlanceWeatherBadge(
                        weather = preferences.cachedWeather,
                        hasConfiguredLocation = preferences.weatherLocationQuery.isNotBlank(),
                        isRefreshing = isWeatherRefreshing,
                        onClick = onWeatherClick,
                        onLongClick = onWeatherLongClick
                    )
                }

                if (preferences.showDeviceStatusInGlance || preferences.showBatteryInGlance) {
                    GlanceTelemetryStrip(
                        status = deviceStatus,
                        showMemoryAndTemp = preferences.showDeviceStatusInGlance,
                        showBattery = preferences.showBatteryInGlance
                    )
                }
            }
        }
    }
}
