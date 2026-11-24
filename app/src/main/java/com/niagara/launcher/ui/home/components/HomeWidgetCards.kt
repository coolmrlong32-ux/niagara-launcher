package com.niagara.launcher.ui.home.components

import android.appwidget.AppWidgetManager
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.niagara.launcher.core.model.BuiltInWidgetKind
import com.niagara.launcher.core.model.DeviceStatusCalculator
import com.niagara.launcher.core.model.DeviceStatusSnapshot
import com.niagara.launcher.core.model.HomeWidgetCodec
import com.niagara.launcher.core.model.HomeWidgetItem
import com.niagara.launcher.core.model.LauncherPreferences
import com.niagara.launcher.core.model.WidgetSizePreset
import com.niagara.launcher.core.theme.AccentMint
import com.niagara.launcher.core.theme.AccentSoftAmber
import com.niagara.launcher.core.theme.IosPillShape
import com.niagara.launcher.core.theme.IosRemoveBadgeBg
import com.niagara.launcher.core.theme.IosRemoveBadgeIcon
import com.niagara.launcher.core.theme.IosRoundedCardSquircleShape
import com.niagara.launcher.core.theme.LauncherMotion
import com.niagara.launcher.core.theme.LocalLauncherVisuals
import com.niagara.launcher.core.widget.LauncherAppWidgetHost
import com.niagara.launcher.core.widget.LauncherAppWidgetHostView
import com.niagara.launcher.core.widget.SystemWidgetManager
import kotlin.math.roundToInt

/**
 * Editable wrapper around both Built-in and System Android AppWidgets on Page 0,
 * offering smooth height animation, reorder controls, size stepping/presets, and removal.
 */
@Composable
fun EditableWidgetWrapper(
    item: HomeWidgetItem,
    isEditMode: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onResize: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (visualHeightDp: Dp) -> Unit
) {
    val visuals = LocalLauncherVisuals.current
    val haptic = LocalHapticFeedback.current
    val animatedHeight by animateDpAsState(
        targetValue = item.heightDp.dp,
        animationSpec = LauncherMotion.DpFluidSpring,
        label = "widgetHeight_${item.id}"
    )
    val cardShape = remember { IosRoundedCardSquircleShape(24.dp) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = animatedHeight)
        ) {
            content(animatedHeight)

            if (isEditMode) {
                // Touch-intercepting overlay prevents accidental launch into widget host apps during layout editing
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(cardShape)
                        .background(Color.Black.copy(alpha = 0.24f))
                        .border(1.dp, visuals.accent.copy(alpha = 0.58f), cardShape)
                        .pointerInput(item.id) {
                            detectTapGestures(onTap = {})
                        }
                )
            }
        }

        if (isEditMode) {
            // Top-start remove badge matching desktop icon edit chrome
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-4).dp, y = (-4).dp)
                    .size(23.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(IosRemoveBadgeBg)
                    .border(0.6.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onRemove()
                    }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = "移除插件",
                    tint = IosRemoveBadgeIcon,
                    modifier = Modifier.size(15.dp)
                )
            }

            // Top-end compact move & resize control pill
            IosGlassPill(
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "上移",
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
                    contentDescription = "下移",
                    tint = if (canMoveDown) visuals.textPrimary else visuals.textTertiary.copy(alpha = 0.35f),
                    modifier = Modifier
                        .size(16.dp)
                        .clickable(enabled = canMoveDown) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onMoveDown()
                        }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .width(0.6.dp)
                        .height(12.dp)
                        .background(Color.White.copy(alpha = 0.25f))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = "减小高度",
                    tint = if (item.heightDp > HomeWidgetCodec.MIN_WIDGET_HEIGHT_DP) {
                        visuals.textPrimary
                    } else {
                        visuals.textTertiary.copy(alpha = 0.35f)
                    },
                    modifier = Modifier
                        .size(15.dp)
                        .clickable(enabled = item.heightDp > HomeWidgetCodec.MIN_WIDGET_HEIGHT_DP) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onResize(item.heightDp - HomeWidgetCodec.RESIZE_STEP_DP)
                        }
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${item.heightDp}dp",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = visuals.accent,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val nextPreset = when {
                            item.heightDp < WidgetSizePreset.MEDIUM.heightDp - 12 -> WidgetSizePreset.MEDIUM.heightDp
                            item.heightDp < WidgetSizePreset.LARGE.heightDp - 12 -> WidgetSizePreset.LARGE.heightDp
                            else -> WidgetSizePreset.COMPACT.heightDp
                        }
                        onResize(nextPreset)
                    }
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "增加高度",
                    tint = if (item.heightDp < HomeWidgetCodec.MAX_WIDGET_HEIGHT_DP) {
                        visuals.textPrimary
                    } else {
                        visuals.textTertiary.copy(alpha = 0.35f)
                    },
                    modifier = Modifier
                        .size(15.dp)
                        .clickable(enabled = item.heightDp < HomeWidgetCodec.MAX_WIDGET_HEIGHT_DP) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onResize(item.heightDp + HomeWidgetCodec.RESIZE_STEP_DP)
                        }
                )
            }
        }
    }
}

/**
 * Renders one of the launcher's built-in frosted glass widget cards on Page 0.
 */
@Composable
fun BuiltInWidgetCard(
    item: HomeWidgetItem.BuiltIn,
    preferences: LauncherPreferences,
    deviceStatus: DeviceStatusSnapshot?,
    isWeatherRefreshing: Boolean,
    onWeatherClick: () -> Unit,
    onWeatherLongClick: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visuals = LocalLauncherVisuals.current

    when (item.kind) {
        BuiltInWidgetKind.WEATHER -> {
            val weather = preferences.cachedWeather
            IosFrostedGlassCard(
                modifier = modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                onClick = onWeatherClick,
                onLongClick = onWeatherLongClick
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = (item.heightDp - 28).coerceAtLeast(48).dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(visuals.accent.copy(alpha = 0.16f))
                                .border(0.6.dp, visuals.accent.copy(alpha = 0.35f), CircleShape)
                        ) {
                            if (isWeatherRefreshing) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color = visuals.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = resolveWeatherIcon(weather?.condition?.iconType),
                                    contentDescription = "天气",
                                    tint = visuals.accent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = weather?.locationName ?: preferences.weatherLocationQuery.ifBlank { "配置实时天气" },
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = visuals.textPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = weather?.condition?.label ?: if (preferences.weatherLocationQuery.isBlank()) {
                                    "点击设置城市或经纬度 · 长按修改"
                                } else {
                                    "点击刷新 · 长按切换位置"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = visuals.textSecondary,
                                    fontSize = 12.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (weather != null) {
                        Text(
                            text = weather.formattedTemperature,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Light,
                                color = visuals.textPrimary
                            )
                        )
                    }
                }
            }
        }

        BuiltInWidgetKind.DEVICE_STATUS -> {
            val memory = deviceStatus?.memory
            val tempC = deviceStatus?.batteryTempCelsius
            val batteryPct = deviceStatus?.batteryPercent
            val isCharging = deviceStatus?.isCharging == true

            IosFrostedGlassCard(
                modifier = modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                onClick = {}
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = (item.heightDp - 28).coerceAtLeast(48).dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Memory,
                                contentDescription = null,
                                tint = visuals.accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (memory != null) {
                                    "内存 ${DeviceStatusCalculator.formatMemoryCompact(memory)}"
                                } else {
                                    "正在读取设备状态…"
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = visuals.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (tempC != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeviceThermostat,
                                        contentDescription = null,
                                        tint = if (tempC >= 40f) AccentSoftAmber else visuals.textSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = DeviceStatusCalculator.formatTemperatureCelsius(tempC),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = visuals.textSecondary,
                                            fontSize = 11.5.sp
                                        )
                                    )
                                }
                            }

                            if (batteryPct != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isCharging) Icons.Outlined.BatteryChargingFull else Icons.Outlined.BatteryFull,
                                        contentDescription = null,
                                        tint = when {
                                            isCharging -> AccentMint
                                            batteryPct <= 15 -> AccentSoftAmber
                                            else -> visuals.textSecondary
                                        },
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "$batteryPct%",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = visuals.textPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }
                        }
                    }

                    if (memory != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { (memory.usedPercent / 100f).coerceIn(0f, 1f) },
                            color = if (memory.usedPercent >= 85) AccentSoftAmber else visuals.accent,
                            trackColor = Color.White.copy(alpha = 0.14f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(CircleShape)
                        )
                    }
                }
            }
        }

        BuiltInWidgetKind.QUICK_SEARCH -> {
            IosFrostedGlassCard(
                modifier = modifier.fillMaxWidth(),
                cornerRadius = 22.dp,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                onClick = onSearchClick
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = (item.heightDp - 28).coerceAtLeast(36).dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "搜索",
                        tint = visuals.accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "搜索应用、拼音首字母或网页…",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = visuals.textSecondary,
                            fontSize = 14.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(IosPillShape)
                            .background(visuals.pillTint)
                            .border(0.5.dp, visuals.pillBorder, IosPillShape)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "直达",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = visuals.textPrimary,
                                fontSize = 10.5.sp
                            )
                        )
                    }
                }
            }
        }

        BuiltInWidgetKind.DIGITAL_CLOCK -> {
            IosFrostedGlassCard(
                modifier = modifier.fillMaxWidth(),
                cornerRadius = 26.dp,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                onClick = {}
            ) {
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = (item.heightDp - 32).coerceAtLeast(64).dp)
                ) {
                    LauncherClockDisplay(
                        variant = ClockVariant.WIDGET_CARD
                    )
                }
            }
        }
    }
}

/**
 * Mounts an actual Android AppWidget via [LauncherAppWidgetHost] inside a frosted frame,
 * keeping size hints synchronized across width and user-selected widget height.
 */
@Composable
fun SystemAppWidgetHostCard(
    item: HomeWidgetItem.SystemAppWidget,
    appWidgetHost: LauncherAppWidgetHost?,
    appWidgetManager: AppWidgetManager?,
    visualHeightDp: Dp,
    onRemoveMissing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val visuals = LocalLauncherVisuals.current
    val shape = remember { IosRoundedCardSquircleShape(24.dp) }

    val providerInfo = remember(item.providerPackage, item.providerClass, item.appWidgetId, appWidgetManager) {
        appWidgetManager?.let {
            try {
                it.getAppWidgetInfo(item.appWidgetId)
            } catch (_: Exception) {
                null
            }
        } ?: SystemWidgetManager.findProviderInfo(context, item.providerPackage, item.providerClass)
    }

    if (appWidgetHost == null || providerInfo == null) {
        IosFrostedGlassCard(
            modifier = modifier.fillMaxWidth(),
            cornerRadius = 24.dp,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            onClick = {}
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Widgets,
                        contentDescription = null,
                        tint = visuals.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "小部件不可用（${item.label}）",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = visuals.textSecondary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IosGlassPill(
                    onClick = onRemoveMissing,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "移除",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = visuals.textPrimary,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
        return
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(visualHeightDp)
            .shadow(
                elevation = 8.dp,
                shape = shape,
                ambientColor = visuals.cardShadowAmbient,
                spotColor = visuals.cardShadowSpot
            )
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.16f))
            .border(0.65.dp, visuals.glassBorderTop.copy(alpha = 0.55f), shape)
    ) {
        val widthDp = maxWidth.value.roundToInt().coerceAtLeast(120)

        key(item.id, item.appWidgetId) {
            AndroidView(
                factory = { ctx ->
                    appWidgetHost.createView(ctx, item.appWidgetId, providerInfo).apply {
                        (this as? LauncherAppWidgetHostView)?.applySizeDp(widthDp, item.heightDp)
                    }
                },
                update = { view ->
                    (view as? LauncherAppWidgetHostView)?.applySizeDp(widthDp, item.heightDp)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
