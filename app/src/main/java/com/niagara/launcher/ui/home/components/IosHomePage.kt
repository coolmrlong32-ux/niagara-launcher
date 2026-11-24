package com.niagara.launcher.ui.home.components

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.provider.AlarmClock
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagara.launcher.core.model.AppItem
import com.niagara.launcher.core.model.LauncherPreferences
import com.niagara.launcher.core.theme.IosLabelShadow
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Independent iOS-style Home page featuring a compact header, Frosted Glass Favorites Card,
 * and 4x6 Squircle Desktop App Grid.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IosHomePage(
    favorites: List<AppItem>,
    desktopApps: List<AppItem>,
    isEditMode: Boolean,
    showBattery: Boolean,
    onLaunchApp: (AppItem) -> Unit,
    onEnterEditMode: () -> Unit,
    onExitEditMode: () -> Unit,
    onRemoveFavorite: (AppItem) -> Unit,
    onRemoveDesktopApp: (AppItem) -> Unit,
    onOpenFavoritesPicker: () -> Unit,
    onOpenDesktopPicker: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

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
                    if (!isEditMode) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEnterEditMode()
                    }
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 72.dp)
        ) {
            if (isEditMode) {
                IosEditModeHeader(
                    desktopCount = desktopApps.size,
                    maxDesktopCount = LauncherPreferences.MAX_DESKTOP_APPS,
                    onAddClick = onOpenDesktopPicker,
                    onDoneClick = onExitEditMode
                )
            } else {
                IosNormalHeader(
                    showBattery = showBattery,
                    onSearchClick = onSearchClick,
                    onSettingsClick = onSettingsClick
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            IosFavoritesCard(
                favorites = favorites,
                isEditMode = isEditMode,
                onLaunchApp = onLaunchApp,
                onEnterEditMode = onEnterEditMode,
                onRemoveFavorite = onRemoveFavorite,
                onOpenFavoritesPicker = onOpenFavoritesPicker
            )

            Spacer(modifier = Modifier.height(20.dp))

            IosDesktopGrid(
                desktopApps = desktopApps,
                isEditMode = isEditMode,
                onLaunchApp = onLaunchApp,
                onEnterEditMode = onEnterEditMode,
                onRemoveDesktopApp = onRemoveDesktopApp,
                onOpenDesktopPicker = onOpenDesktopPicker
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Surface(
            onClick = onAddClick,
            shape = RoundedCornerShape(50),
            color = Color.White.copy(alpha = 0.22f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "添加桌面应用",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "添加",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }

        Text(
            text = "编辑桌面 ($desktopCount/$maxDesktopCount)",
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                shadow = Shadow(color = IosLabelShadow, offset = Offset(0f, 1f), blurRadius = 3f)
            )
        )

        Surface(
            onClick = onDoneClick,
            shape = RoundedCornerShape(50),
            color = Color.White.copy(alpha = 0.28f)
        ) {
            Text(
                text = "完成",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun IosNormalHeader(
    showBattery: Boolean,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }
    var batteryPercentage by remember { mutableIntStateOf(100) }

    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("M月d日 EEEE", Locale.getDefault()) }

    LaunchedEffect(showBattery) {
        while (true) {
            val now = Date()
            currentTime = timeFormatter.format(now)
            currentDate = dateFormatter.format(now)
            if (showBattery) {
                val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                batteryPercentage = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
            }
            delay(1000)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    try {
                        context.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    } catch (_: Exception) {
                        // Ignore if alarm app unavailable
                    }
                }
        ) {
            Text(
                text = currentTime.ifEmpty { "12:00" },
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White,
                    shadow = Shadow(color = IosLabelShadow, offset = Offset(0f, 1.5f), blurRadius = 5f)
                )
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = currentDate,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.88f),
                        fontWeight = FontWeight.Medium,
                        shadow = Shadow(color = IosLabelShadow, offset = Offset(0f, 1f), blurRadius = 3f)
                    )
                )
                if (showBattery) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Outlined.BatteryFull,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "$batteryPercentage%",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.88f),
                            shadow = Shadow(color = IosLabelShadow, offset = Offset(0f, 1f), blurRadius = 3f)
                        )
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                onClick = onSearchClick,
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.16f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "搜索",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "搜索",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            Surface(
                onClick = onSettingsClick,
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.14f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = "启动器设置",
                    tint = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier
                        .padding(7.dp)
                        .size(16.dp)
                )
            }
        }
    }
}
