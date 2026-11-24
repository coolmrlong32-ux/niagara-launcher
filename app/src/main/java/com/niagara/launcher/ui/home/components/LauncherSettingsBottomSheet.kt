package com.niagara.launcher.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Swipe
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WbCloudy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagara.launcher.core.model.AppItem
import com.niagara.launcher.core.model.LauncherPreferences

/**
 * Minimalist settings & hidden apps management sheet matching Niagara's dark translucent aesthetic.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherSettingsBottomSheet(
    preferences: LauncherPreferences,
    hiddenApps: List<AppItem>,
    onDismissRequest: () -> Unit,
    onRightHandedChange: (Boolean) -> Unit,
    onHapticFeedbackChange: (Boolean) -> Unit,
    onShowBatteryChange: (Boolean) -> Unit,
    onShowDeviceStatusChange: (Boolean) -> Unit = {},
    onShowWeatherChange: (Boolean) -> Unit = {},
    onDoubleTapToLockChange: (Boolean) -> Unit = {},
    onOpenScreenLockGuide: (() -> Unit)? = null,
    onConfigureWeatherClick: (() -> Unit)? = null,
    onUnhideApp: (AppItem) -> Unit,
    onOpenDesktopSettings: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val lockStatus = rememberScreenLockPermissionStatus()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFF1E1F24),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                text = "启动器设置",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(8.dp))

            if (onOpenDesktopSettings != null) {
                Surface(
                    onClick = {
                        onDismissRequest()
                        onOpenDesktopSettings()
                    },
                    color = Color.White.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.GridView,
                            contentDescription = "桌面设置",
                            tint = Color(0xFF0A84FF),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "桌面设置与图标选择",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = "图标大小、4/5列网格、名称显示与第三方图标包",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.55f),
                                    fontSize = 12.sp
                                )
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.55f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            SettingSwitchRow(
                icon = Icons.Outlined.Swipe,
                title = "左侧边缘隐形字母手势",
                subtitle = if (preferences.isRightHanded) {
                    "字母列表仅在右侧显示，左右两侧边缘均可上下滑动选字母"
                } else {
                    "仅在屏幕右侧字母条区域滑动选字母"
                },
                checked = preferences.isRightHanded,
                onCheckedChange = onRightHandedChange
            )

            SettingSwitchRow(
                icon = Icons.Outlined.Vibration,
                title = "字母滑动触觉反馈",
                subtitle = "滑动经过字母边界时触发轻微震动",
                checked = preferences.hapticFeedbackEnabled,
                onCheckedChange = onHapticFeedbackChange
            )

            SettingSwitchRow(
                icon = Icons.Outlined.Lock,
                title = "双击空白区域熄屏",
                subtitle = "仅在空白区域触发，带防误触与间隔校验",
                checked = preferences.doubleTapToLockEnabled,
                onCheckedChange = onDoubleTapToLockChange
            )

            if (onOpenScreenLockGuide != null) {
                Surface(
                    onClick = {
                        onDismissRequest()
                        onOpenScreenLockGuide()
                    },
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = "锁屏权限引导",
                            tint = if (lockStatus.hasAnyPermission) Color(0xFF63E2A0) else Color(0xFFFFB86B),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "熄屏权限配置与引导",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = when {
                                    lockStatus.accessibilityEnabled -> "已授权无障碍锁屏（支持指纹解锁）"
                                    lockStatus.deviceAdminEnabled -> "已授权设备管理器锁屏"
                                    else -> "尚未授权 · 点击查看无障碍开启引导"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.58f),
                                    fontSize = 12.sp
                                )
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.55f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            SettingSwitchRow(
                icon = Icons.Outlined.BatteryFull,
                title = "顶部显示电量百分比",
                subtitle = "在 Glance 状态栏展示当前电池电量",
                checked = preferences.showBatteryInGlance,
                onCheckedChange = onShowBatteryChange
            )

            SettingSwitchRow(
                icon = Icons.Outlined.Memory,
                title = "顶部显示内存与电池温度",
                subtitle = "仅前台可见时低频刷新，退后台自动停止采集",
                checked = preferences.showDeviceStatusInGlance,
                onCheckedChange = onShowDeviceStatusChange
            )

            SettingSwitchRow(
                icon = Icons.Outlined.WbCloudy,
                title = "顶部显示实时天气",
                subtitle = "使用 Open-Meteo 免密钥方案并本地缓存",
                checked = preferences.showWeatherInGlance,
                onCheckedChange = onShowWeatherChange
            )

            if (preferences.showWeatherInGlance && onConfigureWeatherClick != null) {
                val weatherSummary = preferences.cachedWeather?.let {
                    "${it.locationName} · ${it.formattedTemperature} ${it.condition.label}"
                } ?: preferences.weatherLocationQuery.ifBlank { "未配置（点击输入城市或经纬度）" }

                Surface(
                    onClick = onConfigureWeatherClick,
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = "天气位置",
                            tint = Color(0xFF64D2FF),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "天气位置",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = weatherSummary,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.58f),
                                    fontSize = 12.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.55f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "已隐藏应用 (${hiddenApps.size})",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.SemiBold
                    )
                )
                if (hiddenApps.isNotEmpty()) {
                    Text(
                        text = "点击右侧恢复显示",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.45f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (hiddenApps.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.04f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "暂无已隐藏的应用。长按任意应用并选择「隐藏应用」即可将其从主列表收纳。",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.5f),
                            lineHeight = 18.sp
                        ),
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                ) {
                    items(items = hiddenApps, key = { it.key }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIcon(app = app, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = app.packageName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White.copy(alpha = 0.45f)
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            TextButton(onClick = { onUnhideApp(app) }) {
                                Icon(
                                    imageVector = Icons.Outlined.Visibility,
                                    contentDescription = "恢复显示",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("恢复")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 12.sp
                    )
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}
