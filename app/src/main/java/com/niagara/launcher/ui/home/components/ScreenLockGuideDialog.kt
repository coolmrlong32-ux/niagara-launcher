package com.niagara.launcher.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.niagara.launcher.core.system.ScreenLockController
import com.niagara.launcher.core.system.ScreenLockPermissionStatus
import com.niagara.launcher.core.theme.AccentMint
import com.niagara.launcher.core.theme.AccentSoftAmber
import com.niagara.launcher.core.theme.IosAccentBlue
import com.niagara.launcher.core.theme.IosSettingsCardElevated
import com.niagara.launcher.core.theme.IosSettingsCardSurface
import com.niagara.launcher.core.theme.IosSettingsLabelPrimary
import com.niagara.launcher.core.theme.IosSettingsLabelSecondary

/**
 * Observes lock-screen permission status and automatically refreshes whenever returning
 * from Android system settings.
 */
@Composable
fun rememberScreenLockPermissionStatus(): ScreenLockPermissionStatus {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var status by remember(context) {
        mutableStateOf(ScreenLockController.getPermissionStatus(context))
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                status = ScreenLockController.getPermissionStatus(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return status
}

/**
 * Guided setup dialog explaining how to grant system Accessibility (recommended)
 * or Device Admin permission for compliant double-tap screen locking.
 */
@Composable
fun ScreenLockGuideDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val status = rememberScreenLockPermissionStatus()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IosSettingsCardSurface,
        titleContentColor = IosSettingsLabelPrimary,
        textContentColor = IosSettingsLabelSecondary,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(IosAccentBlue.copy(alpha = 0.18f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = IosAccentBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "双击空白区域熄屏",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = IosSettingsLabelPrimary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "为了在锁屏后依然支持指纹或面容解锁，推荐启用无障碍锁屏服务（仅执行系统锁屏动作，不读取任何屏幕内容）。",
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = IosSettingsLabelSecondary
                )

                PermissionStatusCard(
                    title = if (status.hasAnyPermission) "已授权熄屏权限" else "尚未授权熄屏权限",
                    detail = when {
                        status.accessibilityEnabled -> "无障碍锁屏服务运行中（支持指纹/面容解锁）"
                        status.deviceAdminEnabled -> "设备管理器锁屏已激活"
                        else -> "请在下方开启「Niagara Launcher 双击熄屏」服务"
                    },
                    isReady = status.hasAnyPermission
                )

                if (status.supportsAccessibilityLock) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(IosSettingsCardElevated)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "开启步骤（推荐无障碍）",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IosSettingsLabelPrimary
                        )
                        Text(
                            text = "1. 点击下方「前往无障碍设置」\n2. 在已下载的服务中找到「Niagara Launcher 双击熄屏」\n3. 开启开关并确认授权后返回桌面即可",
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = IosSettingsLabelSecondary
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (status.supportsAccessibilityLock) {
                        Button(
                            onClick = { ScreenLockController.openAccessibilitySettings(context) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IosAccentBlue,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccessibilityNew,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (status.accessibilityEnabled) "查看无障碍设置" else "前往无障碍设置（推荐）",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { ScreenLockController.openDeviceAdminActivation(context) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AdminPanelSettings,
                            contentDescription = null,
                            tint = IosSettingsLabelPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (status.deviceAdminEnabled) "设备管理器已激活" else "改用设备管理器锁屏（备用）",
                            fontSize = 13.sp,
                            color = IosSettingsLabelPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = if (status.hasAnyPermission) "完成" else "稍后设置",
                    color = IosAccentBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

@Composable
private fun PermissionStatusCard(
    title: String,
    detail: String,
    isReady: Boolean
) {
    val accent = if (isReady) AccentMint else AccentSoftAmber
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.12f))
            .border(0.6.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Icon(
            imageVector = if (isReady) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = IosSettingsLabelPrimary
            )
            Text(
                text = detail,
                fontSize = 11.5.sp,
                color = IosSettingsLabelSecondary
            )
        }
    }
}
