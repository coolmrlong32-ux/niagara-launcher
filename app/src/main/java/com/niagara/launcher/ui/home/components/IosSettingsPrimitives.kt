package com.niagara.launcher.ui.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagara.launcher.core.theme.IosAccentBlue
import com.niagara.launcher.core.theme.IosSettingsCardElevated
import com.niagara.launcher.core.theme.IosSettingsCardSurface
import com.niagara.launcher.core.theme.IosSettingsChevron
import com.niagara.launcher.core.theme.IosSettingsDivider
import com.niagara.launcher.core.theme.IosSettingsLabelPrimary
import com.niagara.launcher.core.theme.IosSettingsLabelSecondary
import com.niagara.launcher.core.theme.IosSwitchGreen
import com.niagara.launcher.core.theme.IosSwitchTrackOff

/**
 * iOS-style top navigation bar with centered title, optional back chevron + label, and trailing action.
 */
@Composable
fun IosNavigationBar(
    title: String,
    leadingText: String? = null,
    onLeadingClick: (() -> Unit)? = null,
    trailingText: String? = "完成",
    onTrailingClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0A0C))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(48.dp)
                .padding(horizontal = 8.dp)
        ) {
            if (leadingText != null && onLeadingClick != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onLeadingClick)
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = leadingText,
                        tint = IosAccentBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = leadingText,
                        color = IosAccentBlue,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }

            Text(
                text = title,
                color = IosSettingsLabelPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.Center)
            )

            if (trailingText != null && onTrailingClick != null) {
                TextButton(
                    onClick = onTrailingClick,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Text(
                        text = trailingText,
                        color = IosAccentBlue,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = IosSettingsDivider)
    }
}

@Composable
fun IosSettingsSectionHeader(
    title: String,
    trailingAction: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 32.dp, end = 20.dp, top = 22.dp, bottom = 6.dp)
    ) {
        Text(
            text = title,
            color = IosSettingsLabelSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal
        )
        trailingAction?.invoke()
    }
}

@Composable
fun IosSettingsSectionFooter(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = IosSettingsLabelSecondary,
        fontSize = 12.5.sp,
        lineHeight = 17.sp,
        modifier = modifier.padding(start = 32.dp, end = 32.dp, top = 8.dp)
    )
}

@Composable
fun IosInsetGroupedCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = IosSettingsCardSurface,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

@Composable
fun IosInsetDivider(
    startIndent: Dp = 56.dp,
    modifier: Modifier = Modifier
) {
    HorizontalDivider(
        modifier = modifier.padding(start = startIndent),
        thickness = 0.5.dp,
        color = IosSettingsDivider
    )
}

@Composable
fun IosSettingsIconBadge(
    icon: ImageVector,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(29.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(backgroundColor)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
fun IosSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) IosSwitchGreen else IosSwitchTrackOff,
        label = "iosSwitchTrack"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 2.dp,
        label = "iosSwitchThumb"
    )

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .width(51.dp)
            .height(31.dp)
            .clip(CircleShape)
            .background(trackColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCheckedChange(!checked) }
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(27.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
fun IosSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(IosSettingsCardElevated)
            .padding(2.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (selected) Color(0xFF636366) else Color.Transparent)
                    .clickable { onOptionSelected(index) }
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun IosSettingsSwitchRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        IosSettingsIconBadge(icon = icon, backgroundColor = iconBg)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = IosSettingsLabelPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        IosSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun IosSettingsNavigationRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    valueText: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        IosSettingsIconBadge(icon = icon, backgroundColor = iconBg)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = IosSettingsLabelPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (!valueText.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = valueText,
                color = IosSettingsLabelSecondary,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = IosSettingsChevron,
            modifier = Modifier.size(18.dp)
        )
    }
}
