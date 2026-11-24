package com.niagara.launcher.ui.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbCloudy
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.niagara.launcher.core.data.DeviceStatusMonitor
import com.niagara.launcher.core.model.DeviceStatusCalculator
import com.niagara.launcher.core.model.DeviceStatusSnapshot
import com.niagara.launcher.core.model.WeatherIconType
import com.niagara.launcher.core.model.WeatherSnapshot
import com.niagara.launcher.core.theme.AccentMint
import com.niagara.launcher.core.theme.AccentSoftAmber
import com.niagara.launcher.core.theme.IosAccentBlue
import com.niagara.launcher.core.theme.IosPillShape
import com.niagara.launcher.core.theme.IosSettingsCardElevated
import com.niagara.launcher.core.theme.IosSettingsCardSurface
import com.niagara.launcher.core.theme.IosSettingsLabelPrimary
import com.niagara.launcher.core.theme.IosSettingsLabelSecondary
import com.niagara.launcher.core.theme.LocalLauncherVisuals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Collects lightweight RAM & battery telemetry ONLY while the launcher is in the foreground
 * (Lifecycle.State.STARTED), automatically suspending collection when backgrounded.
 */
@Composable
fun rememberForegroundDeviceStatus(
    enabled: Boolean,
    intervalMs: Long = DeviceStatusMonitor.DEFAULT_POLL_INTERVAL_MS
): DeviceStatusSnapshot? {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val monitor = remember(context) { DeviceStatusMonitor(context.applicationContext) }
    var snapshot by remember { mutableStateOf<DeviceStatusSnapshot?>(null) }

    LaunchedEffect(enabled, lifecycleOwner, intervalMs) {
        if (!enabled) {
            snapshot = null
            return@LaunchedEffect
        }
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                snapshot = withContext(Dispatchers.Default) {
                    monitor.readSnapshot()
                }
                delay(intervalMs.coerceAtLeast(3_000L))
            }
        }
    }

    return snapshot
}

/**
 * Minimalist Niagara Glance widget displaying time, weather, date, real-time phone telemetry
 * (RAM usage, battery temperature, charge), and quick Search/Settings actions.
 */
@Composable
fun GlanceWidget(
    modifier: Modifier = Modifier,
    showBattery: Boolean = true,
    showDeviceStatus: Boolean = true,
    showWeather: Boolean = true,
    deviceStatus: DeviceStatusSnapshot? = null,
    weather: WeatherSnapshot? = null,
    weatherLocationQuery: String = "",
    isWeatherRefreshing: Boolean = false,
    onWeatherClick: () -> Unit = {},
    onWeatherLongClick: () -> Unit = {},
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val visuals = LocalLauncherVisuals.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Unified Clock + Weather minimal top row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LauncherClockDisplay(
                variant = ClockVariant.COMPACT_DRAWER,
                modifier = Modifier.weight(1f)
            )

            if (showWeather) {
                GlanceWeatherBadge(
                    weather = weather,
                    hasConfiguredLocation = weatherLocationQuery.isNotBlank(),
                    isRefreshing = isWeatherRefreshing,
                    onClick = onWeatherClick,
                    onLongClick = onWeatherLongClick,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search/Settings action row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IosGlassPill(
                    onClick = onSearchClick,
                    contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp)
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
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                IosGlassPill(
                    shape = CircleShape,
                    onClick = onSettingsClick,
                    contentPadding = PaddingValues(7.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = "启动器设置",
                        tint = visuals.textPrimary.copy(alpha = 0.92f),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        // Real-time RAM + battery temperature + battery level telemetry pill
        if (showDeviceStatus || showBattery) {
            Spacer(modifier = Modifier.height(10.dp))
            GlanceTelemetryStrip(
                status = deviceStatus,
                showMemoryAndTemp = showDeviceStatus,
                showBattery = showBattery
            )
        }
    }
}

/**
 * Compact frosted hairline weather badge displayed in both Home and Drawer Glance headers.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlanceWeatherBadge(
    weather: WeatherSnapshot?,
    hasConfiguredLocation: Boolean,
    isRefreshing: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visuals = LocalLauncherVisuals.current

    Box(
        modifier = modifier
            .clip(IosPillShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(visuals.pillTint)
            .background(visuals.pillFill)
            .border(0.6.dp, visuals.pillBorder, IosPillShape)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    strokeWidth = 1.5.dp,
                    color = visuals.accent,
                    modifier = Modifier.size(12.dp)
                )
            } else {
                Icon(
                    imageVector = resolveWeatherIcon(weather?.condition?.iconType),
                    contentDescription = "天气",
                    tint = if (weather != null) visuals.accent else visuals.textSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }

            if (weather != null) {
                Text(
                    text = weather.formattedTemperature,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = visuals.textPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.sp
                    )
                )
                Text(
                    text = "${weather.condition.label} · ${weather.locationName}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = visuals.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else if (hasConfiguredLocation) {
                Text(
                    text = if (isRefreshing) "获取天气…" else "点击刷新天气",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = visuals.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            } else {
                Text(
                    text = "设置天气",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = visuals.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

/**
 * Refined single-row translucent telemetry strip showing RAM usage, battery temperature (°C),
 * and battery percentage with gentle semantic tinting.
 */
@Composable
fun GlanceTelemetryStrip(
    status: DeviceStatusSnapshot?,
    showMemoryAndTemp: Boolean,
    showBattery: Boolean,
    modifier: Modifier = Modifier
) {
    val visuals = LocalLauncherVisuals.current
    val memory = if (showMemoryAndTemp) status?.memory else null
    val tempC = if (showMemoryAndTemp) status?.batteryTempCelsius else null
    val batteryPct = if (showBattery) status?.batteryPercent else null

    if (memory == null && tempC == null && batteryPct == null) return

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .clip(IosPillShape)
            .background(visuals.pillTint.copy(alpha = 0.72f))
            .background(Color.White.copy(alpha = 0.06f))
            .border(0.5.dp, visuals.pillBorder.copy(alpha = 0.75f), IosPillShape)
            .padding(horizontal = 11.dp, vertical = 5.dp)
    ) {
        if (memory != null) {
            TelemetryMetricItem(
                icon = Icons.Outlined.Memory,
                text = DeviceStatusCalculator.formatMemoryCompact(memory),
                tint = if (memory.usedPercent >= 85) AccentSoftAmber else visuals.textSecondary
            )
        }

        if (tempC != null) {
            if (memory != null) TelemetryDotDivider()
            TelemetryMetricItem(
                icon = Icons.Outlined.DeviceThermostat,
                text = DeviceStatusCalculator.formatTemperatureCelsius(tempC),
                tint = if (tempC >= 40f) AccentSoftAmber else visuals.textSecondary
            )
        }

        if (batteryPct != null) {
            if (memory != null || tempC != null) TelemetryDotDivider()
            val isCharging = status?.isCharging == true
            TelemetryMetricItem(
                icon = if (isCharging) Icons.Outlined.BatteryChargingFull else Icons.Outlined.BatteryFull,
                text = "$batteryPct%",
                tint = when {
                    isCharging -> AccentMint
                    batteryPct <= 15 -> AccentSoftAmber
                    else -> visuals.textSecondary
                }
            )
        }
    }
}

@Composable
private fun TelemetryMetricItem(
    icon: ImageVector,
    text: String,
    tint: Color
) {
    val visuals = LocalLauncherVisuals.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(12.5.dp)
        )
        Spacer(modifier = Modifier.width(3.5.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                color = visuals.textPrimary.copy(alpha = 0.90f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.1.sp
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun TelemetryDotDivider() {
    Box(
        modifier = Modifier
            .size(2.5.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.28f))
    )
}

private fun resolveWeatherIcon(iconType: WeatherIconType?): ImageVector {
    return when (iconType) {
        WeatherIconType.CLEAR_DAY -> Icons.Outlined.WbSunny
        WeatherIconType.CLEAR_NIGHT -> Icons.Outlined.NightsStay
        WeatherIconType.PARTLY_CLOUDY -> Icons.Outlined.WbCloudy
        WeatherIconType.CLOUDY -> Icons.Outlined.Cloud
        WeatherIconType.FOG -> Icons.Outlined.Grain
        WeatherIconType.DRIZZLE, WeatherIconType.RAIN -> Icons.Outlined.WaterDrop
        WeatherIconType.SNOW -> Icons.Outlined.AcUnit
        WeatherIconType.THUNDERSTORM -> Icons.Outlined.Thunderstorm
        null -> Icons.Outlined.WbCloudy
    }
}

/**
 * Minimalist dark dialog for configuring the Open-Meteo city name or latitude/longitude coordinates.
 */
@Composable
fun WeatherConfigDialog(
    currentQuery: String,
    cachedWeather: WeatherSnapshot?,
    isRefreshing: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSaveLocation: (String) -> Unit,
    onRefreshNow: () -> Unit
) {
    var inputText by remember(currentQuery) { mutableStateOf(currentQuery) }
    val quickCities = remember { listOf("北京", "上海", "深圳", "杭州") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IosSettingsCardSurface,
        titleContentColor = IosSettingsLabelPrimary,
        textContentColor = IosSettingsLabelSecondary,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text(
                text = "天气位置设置",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = IosSettingsLabelPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "基于 Open-Meteo 免 API Key 服务，输入城市名（如 上海、Tokyo）或经纬度（如 31.23, 121.47）。",
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    color = IosSettingsLabelSecondary
                )

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = "城市名或 纬度,经度",
                            color = IosSettingsLabelSecondary.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = IosSettingsCardElevated,
                        unfocusedContainerColor = IosSettingsCardElevated,
                        focusedTextColor = IosSettingsLabelPrimary,
                        unfocusedTextColor = IosSettingsLabelPrimary,
                        focusedBorderColor = IosAccentBlue,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    quickCities.forEach { city ->
                        Box(
                            modifier = Modifier
                                .clip(IosPillShape)
                                .background(IosSettingsCardElevated)
                                .border(0.5.dp, Color.White.copy(alpha = 0.14f), IosPillShape)
                                .clickable { inputText = city }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = city,
                                fontSize = 12.sp,
                                color = IosSettingsLabelPrimary
                            )
                        }
                    }
                }

                if (cachedWeather != null) {
                    Text(
                        text = "当前缓存：${cachedWeather.locationName} ${cachedWeather.formattedTemperature} ${cachedWeather.condition.label}",
                        fontSize = 12.sp,
                        color = AccentMint.copy(alpha = 0.9f)
                    )
                }

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        fontSize = 12.sp,
                        color = AccentSoftAmber
                    )
                }
            }
        },
        dismissButton = {
            Row {
                if (currentQuery.isNotBlank()) {
                    TextButton(onClick = onRefreshNow, enabled = !isRefreshing) {
                        Text(
                            text = if (isRefreshing) "更新中…" else "刷新",
                            color = IosSettingsLabelSecondary
                        )
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("取消", color = IosSettingsLabelSecondary)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSaveLocation(inputText.trim())
                    onDismiss()
                }
            ) {
                Text(
                    text = if (inputText.isBlank()) "清除" else "保存并更新",
                    color = IosAccentBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}
