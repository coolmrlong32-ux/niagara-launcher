package com.niagara.launcher.ui.home.components

import android.appwidget.AppWidgetManager
import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagara.launcher.core.gesture.DoubleTapLockGuard
import com.niagara.launcher.core.gesture.TapOutcome
import com.niagara.launcher.core.model.AppItem
import com.niagara.launcher.core.model.DeviceStatusSnapshot
import com.niagara.launcher.core.model.HomeSectionType
import com.niagara.launcher.core.model.HomeWidgetItem
import com.niagara.launcher.core.model.LauncherPreferences
import com.niagara.launcher.core.theme.LocalLauncherVisuals
import com.niagara.launcher.core.transition.LaunchSourceBounds
import com.niagara.launcher.core.widget.LauncherAppWidgetHost

/**
 * Independent iOS-style Home page featuring:
 * - Redesigned flagship hero clock & minimalist Search/Settings controls
 * - Modular editable sections (System & Built-in Widgets, Favorites Card, 4x6/5x6 Desktop Grid)
 * - Free section reordering, app drag-and-drop sorting, widget resizing, and anti-accidental-touch double-tap lock
 */
@Composable
fun IosHomePage(
    favorites: List<AppItem>,
    desktopApps: List<AppItem>,
    isEditMode: Boolean,
    preferences: LauncherPreferences,
    deviceStatus: DeviceStatusSnapshot?,
    isWeatherRefreshing: Boolean,
    appWidgetHost: LauncherAppWidgetHost? = null,
    appWidgetManager: AppWidgetManager? = null,
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
    onOpenWidgetPicker: () -> Unit = {},
    onRemoveWidget: (String) -> Unit = {},
    onMoveWidgetUp: (String) -> Unit = {},
    onMoveWidgetDown: (String) -> Unit = {},
    onResizeWidget: (String, Int) -> Unit = { _, _ -> },
    onMoveSection: (HomeSectionType, Int) -> Unit = { _, _ -> },
    onSearchClick: () -> Unit,
    isOverlayBlocking: Boolean = false,
    onDoubleTapLock: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    var isGridDragging by remember { mutableStateOf(false) }
    val doubleTapGuard = remember { DoubleTapLockGuard() }
    val maxTapDistancePx = with(density) { 28.dp.toPx() }

    LaunchedEffect(isEditMode, isGridDragging, isOverlayBlocking) {
        if (isEditMode || isGridDragging || isOverlayBlocking) {
            doubleTapGuard.reset()
        }
    }

    val latestIsEditMode by rememberUpdatedState(isEditMode)
    val latestIsGridDragging by rememberUpdatedState(isGridDragging)
    val latestIsOverlayBlocking by rememberUpdatedState(isOverlayBlocking)
    val latestDoubleTapLockEnabled by rememberUpdatedState(preferences.doubleTapToLockEnabled)
    val latestOnExitEditMode by rememberUpdatedState(onExitEditMode)
    val latestOnOpenDesktopMenu by rememberUpdatedState(onOpenDesktopMenu)
    val latestOnDoubleTapLock by rememberUpdatedState(onDoubleTapLock)

    val handleDraggingChanged: (Boolean) -> Unit = { dragging ->
        if (dragging) doubleTapGuard.reset()
        isGridDragging = dragging
        onDraggingChanged(dragging)
    }

    val guardedLaunchApp: (AppItem, LaunchSourceBounds?) -> Unit = { app, bounds ->
        doubleTapGuard.reset()
        onLaunchApp(app, bounds)
    }

    val visibleSections = remember(
        preferences.homeSectionOrder,
        preferences.homeWidgets,
        preferences.showFavoritesBar,
        isEditMode
    ) {
        preferences.homeSectionOrder.filter { section ->
            when (section) {
                HomeSectionType.WIDGETS -> isEditMode || preferences.homeWidgets.isNotEmpty()
                HomeSectionType.FAVORITES -> preferences.showFavoritesBar
                HomeSectionType.DESKTOP_APPS -> true
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(maxTapDistancePx) {
                detectTapGestures(
                    onTap = { offset ->
                        if (latestIsEditMode) {
                            doubleTapGuard.reset()
                            latestOnExitEditMode()
                        } else {
                            val outcome = doubleTapGuard.onEmptySpaceTap(
                                x = offset.x,
                                y = offset.y,
                                maxDistancePx = maxTapDistancePx,
                                nowMs = SystemClock.uptimeMillis(),
                                enabled = latestDoubleTapLockEnabled,
                                isBlockedByMode = latestIsEditMode || latestIsGridDragging || latestIsOverlayBlocking
                            )
                            if (outcome == TapOutcome.DOUBLE_TAP_LOCK) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                latestOnDoubleTapLock()
                            }
                        }
                    },
                    onLongPress = {
                        doubleTapGuard.reset()
                        if (!latestIsEditMode && !latestIsGridDragging && !latestIsOverlayBlocking) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            latestOnOpenDesktopMenu()
                        }
                    }
                )
            }
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
                    onAddWidgetClick = {
                        doubleTapGuard.reset()
                        onOpenWidgetPicker()
                    },
                    onAddFavoriteClick = {
                        doubleTapGuard.reset()
                        onOpenFavoritesPicker()
                    },
                    onAddDesktopAppClick = {
                        doubleTapGuard.reset()
                        onOpenDesktopPicker()
                    },
                    onDoneClick = {
                        doubleTapGuard.reset()
                        onExitEditMode()
                    }
                )
            } else {
                IosNormalHeader(
                    onSearchClick = {
                        doubleTapGuard.reset()
                        onSearchClick()
                    },
                    onSettingsClick = {
                        doubleTapGuard.reset()
                        onOpenDesktopSettings()
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            visibleSections.forEachIndexed { secIdx, section ->
                key(section.storageKey) {
                    val canMoveSectionUp = secIdx > 0
                    val canMoveSectionDown = secIdx < visibleSections.lastIndex
                    val fullOrder = preferences.homeSectionOrder

                    if (isEditMode) {
                        EditableSectionHeader(
                            title = section.displayName,
                            canMoveUp = canMoveSectionUp,
                            canMoveDown = canMoveSectionDown,
                            onMoveUp = {
                                val target = visibleSections[secIdx - 1]
                                val delta = fullOrder.indexOf(target) - fullOrder.indexOf(section)
                                onMoveSection(section, delta)
                            },
                            onMoveDown = {
                                val target = visibleSections[secIdx + 1]
                                val delta = fullOrder.indexOf(target) - fullOrder.indexOf(section)
                                onMoveSection(section, delta)
                            }
                        )
                    }

                    when (section) {
                        HomeSectionType.WIDGETS -> {
                            HomeWidgetsSection(
                                widgets = preferences.homeWidgets,
                                isEditMode = isEditMode,
                                preferences = preferences,
                                deviceStatus = deviceStatus,
                                isWeatherRefreshing = isWeatherRefreshing,
                                appWidgetHost = appWidgetHost,
                                appWidgetManager = appWidgetManager,
                                onWeatherClick = {
                                    doubleTapGuard.reset()
                                    onWeatherClick()
                                },
                                onWeatherLongClick = {
                                    doubleTapGuard.reset()
                                    onWeatherLongClick()
                                },
                                onSearchClick = {
                                    doubleTapGuard.reset()
                                    onSearchClick()
                                },
                                onOpenWidgetPicker = {
                                    doubleTapGuard.reset()
                                    onOpenWidgetPicker()
                                },
                                onRemoveWidget = onRemoveWidget,
                                onMoveWidgetUp = onMoveWidgetUp,
                                onMoveWidgetDown = onMoveWidgetDown,
                                onResizeWidget = onResizeWidget
                            )
                        }

                        HomeSectionType.FAVORITES -> {
                            AnimatedVisibility(
                                visible = preferences.showFavoritesBar,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                IosFavoritesCard(
                                    favorites = favorites,
                                    isEditMode = isEditMode,
                                    onLaunchApp = guardedLaunchApp,
                                    onEnterEditMode = {
                                        doubleTapGuard.reset()
                                        onEnterEditMode()
                                    },
                                    onRemoveFavorite = onRemoveFavorite,
                                    onOpenFavoritesPicker = {
                                        doubleTapGuard.reset()
                                        onOpenFavoritesPicker()
                                    },
                                    onReorderFavorites = onReorderFavorites,
                                    onDraggingChanged = handleDraggingChanged,
                                    gridColumns = preferences.desktopGridColumns,
                                    iconSizeDp = preferences.desktopIconSizeDp,
                                    showAppLabels = preferences.showAppLabels,
                                    iconAppearance = preferences.iconAppearance
                                )
                            }
                        }

                        HomeSectionType.DESKTOP_APPS -> {
                            IosDesktopGrid(
                                desktopApps = desktopApps,
                                isEditMode = isEditMode,
                                onLaunchApp = guardedLaunchApp,
                                onEnterEditMode = {
                                    doubleTapGuard.reset()
                                    onEnterEditMode()
                                },
                                onOpenDesktopMenu = {
                                    doubleTapGuard.reset()
                                    onOpenDesktopMenu()
                                },
                                onRemoveDesktopApp = onRemoveDesktopApp,
                                onOpenDesktopPicker = {
                                    doubleTapGuard.reset()
                                    onOpenDesktopPicker()
                                },
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

                    if (secIdx < visibleSections.lastIndex) {
                        Spacer(modifier = Modifier.height(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeWidgetsSection(
    widgets: List<HomeWidgetItem>,
    isEditMode: Boolean,
    preferences: LauncherPreferences,
    deviceStatus: DeviceStatusSnapshot?,
    isWeatherRefreshing: Boolean,
    appWidgetHost: LauncherAppWidgetHost?,
    appWidgetManager: AppWidgetManager?,
    onWeatherClick: () -> Unit,
    onWeatherLongClick: () -> Unit,
    onSearchClick: () -> Unit,
    onOpenWidgetPicker: () -> Unit,
    onRemoveWidget: (String) -> Unit,
    onMoveWidgetUp: (String) -> Unit,
    onMoveWidgetDown: (String) -> Unit,
    onResizeWidget: (String, Int) -> Unit
) {
    val visuals = LocalLauncherVisuals.current

    if (widgets.isEmpty()) {
        if (isEditMode) {
            IosFrostedGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
                onClick = onOpenWidgetPicker
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Widgets,
                        contentDescription = null,
                        tint = visuals.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "+ 添加系统或内置小部件",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = visuals.textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
        return
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        widgets.forEachIndexed { index, item ->
            key(item.id) {
                EditableWidgetWrapper(
                    item = item,
                    isEditMode = isEditMode,
                    canMoveUp = index > 0,
                    canMoveDown = index < widgets.lastIndex,
                    onRemove = { onRemoveWidget(item.id) },
                    onMoveUp = { onMoveWidgetUp(item.id) },
                    onMoveDown = { onMoveWidgetDown(item.id) },
                    onResize = { newHeight -> onResizeWidget(item.id, newHeight) }
                ) { visualHeightDp ->
                    when (item) {
                        is HomeWidgetItem.BuiltIn -> {
                            BuiltInWidgetCard(
                                item = item,
                                preferences = preferences,
                                deviceStatus = deviceStatus,
                                isWeatherRefreshing = isWeatherRefreshing,
                                onWeatherClick = onWeatherClick,
                                onWeatherLongClick = onWeatherLongClick,
                                onSearchClick = onSearchClick
                            )
                        }

                        is HomeWidgetItem.SystemAppWidget -> {
                            SystemAppWidgetHostCard(
                                item = item,
                                appWidgetHost = appWidgetHost,
                                appWidgetManager = appWidgetManager,
                                visualHeightDp = visualHeightDp,
                                onRemoveMissing = { onRemoveWidget(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditableSectionHeader(
    title: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val visuals = LocalLauncherVisuals.current
    val haptic = LocalHapticFeedback.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                color = visuals.textSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                shadow = Shadow(color = visuals.labelShadow, offset = Offset(0f, 1f), blurRadius = 3f)
            )
        )

        IosGlassPill(
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowUp,
                contentDescription = "上移区块",
                tint = if (canMoveUp) visuals.textPrimary else visuals.textTertiary.copy(alpha = 0.35f),
                modifier = Modifier
                    .size(16.dp)
                    .clickable(enabled = canMoveUp) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onMoveUp()
                    }
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = "下移区块",
                tint = if (canMoveDown) visuals.textPrimary else visuals.textTertiary.copy(alpha = 0.35f),
                modifier = Modifier
                    .size(16.dp)
                    .clickable(enabled = canMoveDown) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onMoveDown()
                    }
            )
        }
    }
}

@Composable
private fun IosEditModeHeader(
    desktopCount: Int,
    maxDesktopCount: Int,
    onAddWidgetClick: () -> Unit,
    onAddFavoriteClick: () -> Unit,
    onAddDesktopAppClick: () -> Unit,
    onDoneClick: () -> Unit
) {
    val visuals = LocalLauncherVisuals.current

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "编辑首页",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = visuals.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        shadow = Shadow(color = visuals.labelShadow, offset = Offset(0f, 1.2f), blurRadius = 4f)
                    )
                )
                Text(
                    text = "自由增删、拖动应用或调整区块与插件 ($desktopCount/$maxDesktopCount)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = visuals.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        shadow = Shadow(color = visuals.labelShadow, offset = Offset(0f, 1f), blurRadius = 3f)
                    )
                )
            }

            IosGlassPill(
                onClick = onDoneClick,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "完成",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = visuals.accent,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            EditQuickAddPill(
                label = "添加插件",
                onClick = onAddWidgetClick,
                modifier = Modifier.weight(1f)
            )
            EditQuickAddPill(
                label = "常用应用",
                onClick = onAddFavoriteClick,
                modifier = Modifier.weight(1f)
            )
            EditQuickAddPill(
                label = "桌面应用",
                onClick = onAddDesktopAppClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun EditQuickAddPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visuals = LocalLauncherVisuals.current
    IosGlassPill(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = label,
                tint = visuals.accent,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = visuals.textPrimary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun IosNormalHeader(
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val visuals = LocalLauncherVisuals.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
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
}
