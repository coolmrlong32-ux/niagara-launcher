package com.niagara.launcher.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagara.launcher.core.model.AppItem
import com.niagara.launcher.core.model.IconPackInfo
import com.niagara.launcher.core.model.IconStyle
import com.niagara.launcher.core.model.LauncherPreferences
import com.niagara.launcher.core.theme.IosAccentBlue
import com.niagara.launcher.core.theme.IosIconBadgeBlue
import com.niagara.launcher.core.theme.IosIconBadgeGreen
import com.niagara.launcher.core.theme.IosIconBadgeOrange
import com.niagara.launcher.core.theme.IosIconBadgePurple
import com.niagara.launcher.core.theme.IosSettingsBackground
import com.niagara.launcher.core.theme.IosSettingsLabelPrimary
import com.niagara.launcher.core.theme.IosSettingsLabelSecondary
import com.niagara.launcher.core.theme.IosSquircleShape

/**
 * Full-screen iOS Settings Inset Grouped List screen for selecting built-in icon styles
 * and installed third-party Icon Packs.
 */
@Composable
fun IconPickerScreen(
    preferences: LauncherPreferences,
    previewApps: List<AppItem>,
    installedIconPacks: List<IconPackInfo>,
    isRefreshingIconPacks: Boolean,
    showBackToSettings: Boolean,
    onSelectIconStyle: (IconStyle) -> Unit,
    onSelectIconPack: (String?) -> Unit,
    onRefreshIconPacks: () -> Unit,
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IosSettingsBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        IosNavigationBar(
            title = "图标选择",
            leadingText = if (showBackToSettings) "桌面设置" else null,
            onLeadingClick = if (showBackToSettings) onBack else null,
            trailingText = "完成",
            onTrailingClick = onDone
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
        ) {
            IosSettingsSectionHeader(title = "实时预览")

            IosInsetGroupedCard {
                IconPreviewBanner(
                    preferences = preferences,
                    previewApps = previewApps
                )
            }

            IosSettingsSectionHeader(title = "内置图标样式")

            IosInsetGroupedCard {
                IconStyle.entries.forEachIndexed { index, style ->
                    if (index > 0) {
                        IosInsetDivider(startIndent = 64.dp)
                    }
                    IconStyleOptionRow(
                        style = style,
                        selected = preferences.iconStyle == style,
                        onClick = { onSelectIconStyle(style) }
                    )
                }
            }

            IosSettingsSectionHeader(
                title = "第三方图标包",
                trailingAction = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = !isRefreshingIconPacks, onClick = onRefreshIconPacks)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "刷新",
                            tint = IosAccentBlue,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "刷新",
                            color = IosAccentBlue,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            )

            IosInsetGroupedCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp)
                        .clickable { onSelectIconPack(null) }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    IosSettingsIconBadge(
                        icon = Icons.Rounded.Apps,
                        backgroundColor = IosIconBadgeBlue,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "系统默认图标",
                            color = IosSettingsLabelPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = "使用应用原始系统图标",
                            color = IosSettingsLabelSecondary,
                            fontSize = 12.sp
                        )
                    }
                    if (preferences.iconPackPackage == null) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "已选中",
                            tint = IosAccentBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (isRefreshingIconPacks) {
                    IosInsetDivider(startIndent = 62.dp)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        CircularProgressIndicator(
                            color = IosAccentBlue,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "正在扫描已安装的第三方图标包...",
                            color = IosSettingsLabelSecondary,
                            fontSize = 14.sp
                        )
                    }
                } else if (installedIconPacks.isEmpty()) {
                    IosInsetDivider(startIndent = 16.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = "未检测到已安装的第三方图标包",
                            color = IosSettingsLabelPrimary.copy(alpha = 0.85f),
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "安装支持 ADW / Nova / Apex 标准的图标包应用后，点击右上角「刷新」即可实时应用。",
                            color = IosSettingsLabelSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                } else {
                    installedIconPacks.forEach { pack ->
                        IosInsetDivider(startIndent = 62.dp)
                        val isSelected = preferences.iconPackPackage == pack.packageName
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .clickable { onSelectIconPack(pack.packageName) }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            PackageAppIcon(
                                packageName = pack.packageName,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pack.label,
                                    color = IosSettingsLabelPrimary,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = pack.packageName,
                                    color = IosSettingsLabelSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "已选中",
                                    tint = IosAccentBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            IosSettingsSectionFooter(
                text = "第三方图标包将解析 appfilter.xml 并实时应用到首页桌面与常用应用图标；未包含的应用将自动应用图标包遮罩或回退系统图标。"
            )
        }
    }
}

@Composable
private fun IconPreviewBanner(
    preferences: LauncherPreferences,
    previewApps: List<AppItem>
) {
    val sampleApps = previewApps.take(4)
    val fallbackColors = listOf(IosIconBadgeBlue, IosIconBadgePurple, IosIconBadgeGreen, IosIconBadgeOrange)

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1F2430), Color(0xFF161922))
                )
            )
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        for (i in 0 until 4) {
            val app = sampleApps.getOrNull(i)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                if (app != null) {
                    AppIcon(
                        app = app,
                        iconStyle = preferences.iconStyle,
                        iconPackPackage = preferences.iconPackPackage,
                        modifier = Modifier.size(52.dp)
                    )
                } else {
                    val shape = resolveIconShape(preferences.iconStyle)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(shape)
                            .background(fallbackColors[i % fallbackColors.size])
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Apps,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = app?.label ?: "示例 ${i + 1}",
                    color = Color.White,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun IconStyleOptionRow(
    style: IconStyle,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        IconStyleSampleBadge(style = style)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = style.displayName,
                color = IosSettingsLabelPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = style.description,
                color = IosSettingsLabelSecondary,
                fontSize = 12.sp
            )
        }
        if (selected) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "已选中",
                tint = IosAccentBlue,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun IconStyleSampleBadge(style: IconStyle) {
    when (style) {
        IconStyle.IOS_SQUIRCLE -> {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(IosSquircleShape())
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF0A84FF), Color(0xFF5E5CE6))
                        )
                    )
                    .border(0.5.dp, Color.White.copy(alpha = 0.25f), IosSquircleShape())
            )
        }

        IconStyle.CIRCLE -> {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF30D158), Color(0xFF0A84FF))
                        )
                    )
                    .border(0.5.dp, Color.White.copy(alpha = 0.25f), CircleShape)
            )
        }

        IconStyle.ADAPTIVE -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF2C2C2E))
                    .border(0.8.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
            ) {
                Icon(
                    imageVector = Icons.Rounded.Android,
                    contentDescription = null,
                    tint = Color(0xFF30D158),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
