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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoSizeSelectLarge
import androidx.compose.material.icons.rounded.PhotoSizeSelectSmall
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.niagara.launcher.core.model.DesktopLayoutMath
import com.niagara.launcher.core.model.IconPackInfo
import com.niagara.launcher.core.model.LauncherPreferences
import com.niagara.launcher.core.theme.IosAccentBlue
import com.niagara.launcher.core.theme.IosIconBadgeBlue
import com.niagara.launcher.core.theme.IosIconBadgeGray
import com.niagara.launcher.core.theme.IosIconBadgeGreen
import com.niagara.launcher.core.theme.IosIconBadgeIndigo
import com.niagara.launcher.core.theme.IosIconBadgeOrange
import com.niagara.launcher.core.theme.IosIconBadgePurple
import com.niagara.launcher.core.theme.IosIconBadgeTeal
import com.niagara.launcher.core.theme.IosSettingsBackground
import com.niagara.launcher.core.theme.IosSettingsCardElevated
import com.niagara.launcher.core.theme.IosSettingsLabelPrimary
import com.niagara.launcher.core.theme.IosSettingsLabelSecondary
import kotlin.math.roundToInt

/**
 * Full-screen iOS Settings Inset Grouped List screen for customizing home screen layout and icons.
 */
@Composable
fun DesktopSettingsScreen(
    preferences: LauncherPreferences,
    previewApps: List<AppItem>,
    installedIconPacks: List<IconPackInfo>,
    hiddenAppsCount: Int,
    onDismiss: () -> Unit,
    onIconSizePreview: (Float) -> Unit,
    onIconSizeCommit: (Float) -> Unit,
    onGridColumnsChange: (Int) -> Unit,
    onShowAppLabelsChange: (Boolean) -> Unit,
    onShowFavoritesBarChange: (Boolean) -> Unit,
    onOpenIconPicker: () -> Unit,
    onEnterEditMode: () -> Unit,
    onOpenLauncherSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activePackLabel = preferences.iconPackPackage?.let { packPkg ->
        installedIconPacks.firstOrNull { it.packageName == packPkg }?.label ?: packPkg
    }
    val activeIconSummary = activePackLabel ?: preferences.iconStyle.displayName

    var latestSliderValue by remember(preferences.desktopIconSizeDp) {
        mutableFloatStateOf(preferences.desktopIconSizeDp)
    }

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
            title = "桌面设置",
            trailingText = "完成",
            onTrailingClick = onDismiss
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
                DesktopSettingsPreviewCard(
                    preferences = preferences,
                    previewApps = previewApps
                )
            }

            IosSettingsSectionHeader(title = "布局与显示")

            IosInsetGroupedCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IosSettingsIconBadge(
                            icon = Icons.Rounded.AspectRatio,
                            backgroundColor = IosIconBadgeBlue
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "图标大小",
                            color = IosSettingsLabelPrimary,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${preferences.desktopIconSizeDp.roundToInt()} dp",
                            color = IosSettingsLabelSecondary,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoSizeSelectSmall,
                            contentDescription = null,
                            tint = IosSettingsLabelSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Slider(
                            value = preferences.desktopIconSizeDp,
                            onValueChange = { nextSize ->
                                latestSliderValue = nextSize
                                onIconSizePreview(nextSize)
                            },
                            onValueChangeFinished = {
                                onIconSizeCommit(latestSliderValue)
                            },
                            valueRange = LauncherPreferences.MIN_ICON_SIZE_DP..LauncherPreferences.MAX_ICON_SIZE_DP,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = IosAccentBlue,
                                inactiveTrackColor = IosSettingsCardElevated
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )
                        Icon(
                            imageVector = Icons.Rounded.PhotoSizeSelectLarge,
                            contentDescription = null,
                            tint = IosSettingsLabelSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                IosInsetDivider()

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    IosSettingsIconBadge(
                        icon = Icons.Rounded.GridView,
                        backgroundColor = IosIconBadgeIndigo
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "网格列数",
                        color = IosSettingsLabelPrimary,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IosSegmentedControl(
                        options = listOf("4列", "5列"),
                        selectedIndex = if (preferences.desktopGridColumns == LauncherPreferences.DESKTOP_GRID_COLUMNS_COMPACT) 1 else 0,
                        onOptionSelected = { index ->
                            onGridColumnsChange(
                                if (index == 1) LauncherPreferences.DESKTOP_GRID_COLUMNS_COMPACT
                                else LauncherPreferences.DESKTOP_GRID_COLUMNS
                            )
                        }
                    )
                }

                IosInsetDivider()

                IosSettingsSwitchRow(
                    icon = Icons.Rounded.TextFields,
                    iconBg = IosIconBadgeTeal,
                    title = "显示应用名称",
                    checked = preferences.showAppLabels,
                    onCheckedChange = onShowAppLabelsChange
                )

                IosInsetDivider()

                IosSettingsSwitchRow(
                    icon = Icons.Rounded.Star,
                    iconBg = IosIconBadgeOrange,
                    title = "常用应用栏",
                    checked = preferences.showFavoritesBar,
                    onCheckedChange = onShowFavoritesBarChange
                )
            }

            IosSettingsSectionFooter(
                text = "切换网格列数将实时调整首页每行图标数（4列上限 24 个 / 5列上限 30 个）。"
            )

            IosSettingsSectionHeader(title = "图标外观")

            IosInsetGroupedCard {
                IosSettingsNavigationRow(
                    icon = Icons.Rounded.Palette,
                    iconBg = IosIconBadgePurple,
                    title = "图标选择",
                    valueText = activeIconSummary,
                    onClick = onOpenIconPicker
                )
            }

            IosSettingsSectionHeader(title = "快捷操作")

            IosInsetGroupedCard {
                IosSettingsNavigationRow(
                    icon = Icons.Rounded.Edit,
                    iconBg = IosIconBadgeGreen,
                    title = "编辑桌面图标",
                    onClick = onEnterEditMode
                )

                IosInsetDivider()

                IosSettingsNavigationRow(
                    icon = Icons.Rounded.Tune,
                    iconBg = IosIconBadgeGray,
                    title = "更多启动器设置与隐藏应用",
                    valueText = if (hiddenAppsCount > 0) "${hiddenAppsCount} 个已隐藏" else null,
                    onClick = onOpenLauncherSettings
                )
            }
        }
    }
}

@Composable
private fun DesktopSettingsPreviewCard(
    preferences: LauncherPreferences,
    previewApps: List<AppItem>
) {
    val columns = preferences.desktopGridColumns
    val spacing = DesktopLayoutMath.horizontalSpacingDp(columns).dp
    val labelFontSize = DesktopLayoutMath.labelFontSizeSp(columns).sp
    val previewIconDp = DesktopLayoutMath.resolveIconSizeDp(
        requestedDp = preferences.desktopIconSizeDp,
        availableCellWidthDp = if (columns >= 5) 56f else 68f
    ).dp
    val placeholderColors = listOf(
        IosIconBadgeBlue,
        IosIconBadgePurple,
        IosIconBadgeGreen,
        IosIconBadgeOrange,
        IosIconBadgeTeal
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1F2738),
                        Color(0xFF151923)
                    )
                )
            )
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            for (index in 0 until columns) {
                val app = previewApps.getOrNull(index)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    if (app != null) {
                        AppIcon(
                            app = app,
                            iconStyle = preferences.iconStyle,
                            iconPackPackage = preferences.iconPackPackage,
                            modifier = Modifier.size(previewIconDp)
                        )
                    } else {
                        val shape = resolveIconShape(preferences.iconStyle)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(previewIconDp)
                                .clip(shape)
                                .background(placeholderColors[index % placeholderColors.size].copy(alpha = 0.85f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.22f), shape)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Apps,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(previewIconDp * 0.5f)
                            )
                        }
                    }

                    if (preferences.showAppLabels) {
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = app?.label ?: "应用 ${index + 1}",
                            color = Color.White,
                            fontSize = labelFontSize,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "${columns}列 × 6行 · ${preferences.desktopIconSizeDp.roundToInt()}dp · 常用应用栏${if (preferences.showFavoritesBar) "开启" else "隐藏"}",
            color = Color.White.copy(alpha = 0.62f),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
