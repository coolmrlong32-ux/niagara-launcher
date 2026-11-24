package com.niagara.launcher.ui.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.niagara.launcher.core.model.IconPackInfo
import com.niagara.launcher.core.model.LauncherPreferences
import com.niagara.launcher.core.theme.IosAccentBlue
import com.niagara.launcher.core.theme.IosIconBadgeBlue
import com.niagara.launcher.core.theme.IosIconBadgeGreen
import com.niagara.launcher.core.theme.IosIconBadgeOrange
import com.niagara.launcher.core.theme.IosIconBadgePurple
import com.niagara.launcher.core.theme.IosIconBadgeTeal
import com.niagara.launcher.core.theme.IosSettingsChevron
import com.niagara.launcher.core.theme.IosSettingsDivider
import com.niagara.launcher.core.theme.IosSettingsLabelPrimary
import com.niagara.launcher.core.theme.IosSettingsLabelSecondary

/**
 * iOS-style detached Bottom Sheet appearing when long-pressing empty home screen space.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopMenuBottomSheet(
    preferences: LauncherPreferences,
    installedIconPacks: List<IconPackInfo>,
    onDismissRequest: () -> Unit,
    onOpenIconPicker: () -> Unit,
    onOpenDesktopSettings: () -> Unit,
    onEnterEditMode: () -> Unit,
    onOpenDesktopPicker: () -> Unit,
    onOpenWidgetPicker: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val activePackLabel = preferences.iconPackPackage?.let { packPkg ->
        installedIconPacks.firstOrNull { it.packageName == packPkg }?.label ?: packPkg
    }
    val iconSummary = activePackLabel ?: preferences.iconStyle.displayName

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = 0.52f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xF5191A1F),
                border = BorderStroke(0.65.dp, Color.White.copy(alpha = 0.16f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 13.dp)
                    ) {
                        Text(
                            text = "主屏幕",
                            color = IosSettingsLabelPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "自定义桌面布局与图标样式",
                            color = IosSettingsLabelSecondary,
                            fontSize = 12.sp
                        )
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = IosSettingsDivider)

                    DesktopMenuRow(
                        icon = Icons.Rounded.Palette,
                        iconBg = IosIconBadgePurple,
                        title = "图标选择",
                        subtitle = iconSummary,
                        showChevron = true,
                        onClick = {
                            onDismissRequest()
                            onOpenIconPicker()
                        }
                    )

                    IosInsetDivider(startIndent = 58.dp)

                    DesktopMenuRow(
                        icon = Icons.Rounded.Tune,
                        iconBg = IosIconBadgeBlue,
                        title = "桌面设置",
                        subtitle = "图标大小 · ${preferences.desktopGridColumns}列网格 · 名称与常用栏",
                        showChevron = true,
                        onClick = {
                            onDismissRequest()
                            onOpenDesktopSettings()
                        }
                    )

                    IosInsetDivider(startIndent = 58.dp)

                    DesktopMenuRow(
                        icon = Icons.Rounded.Edit,
                        iconBg = IosIconBadgeOrange,
                        title = "编辑首页布局",
                        subtitle = "自由编排应用、小部件与区块顺序",
                        showChevron = false,
                        onClick = {
                            onDismissRequest()
                            onEnterEditMode()
                        }
                    )

                    IosInsetDivider(startIndent = 58.dp)

                    DesktopMenuRow(
                        icon = Icons.Rounded.Widgets,
                        iconBg = IosIconBadgeTeal,
                        title = "添加小部件与插件",
                        subtitle = "已放置 ${preferences.homeWidgets.size} 个 · 支持系统原生小部件",
                        showChevron = true,
                        onClick = {
                            onDismissRequest()
                            onOpenWidgetPicker()
                        }
                    )

                    IosInsetDivider(startIndent = 58.dp)

                    DesktopMenuRow(
                        icon = Icons.Rounded.Add,
                        iconBg = IosIconBadgeGreen,
                        title = "添加桌面应用",
                        subtitle = "选择应用添加到首页桌面",
                        showChevron = false,
                        onClick = {
                            onDismissRequest()
                            onOpenDesktopPicker()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                onClick = onDismissRequest,
                shape = RoundedCornerShape(18.dp),
                color = Color(0xF52A2B31),
                border = BorderStroke(0.65.dp, Color.White.copy(alpha = 0.14f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "取消",
                        color = IosAccentBlue,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun DesktopMenuRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    showChevron: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        IosSettingsIconBadge(icon = icon, backgroundColor = iconBg)
        Spacer(modifier = Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = IosSettingsLabelPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subtitle,
                color = IosSettingsLabelSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (showChevron) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = IosSettingsChevron,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
