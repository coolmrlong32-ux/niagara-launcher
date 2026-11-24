package com.niagara.launcher.ui.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagara.launcher.core.model.AppItem
import com.niagara.launcher.core.model.LauncherPreferences
import com.niagara.launcher.core.theme.IosDotActive
import com.niagara.launcher.core.theme.IosDotInactive
import com.niagara.launcher.core.theme.IosGlassBorderBottom
import com.niagara.launcher.core.theme.IosGlassBorderTop
import com.niagara.launcher.core.theme.IosGlassFillBottom
import com.niagara.launcher.core.theme.IosGlassFillTop
import com.niagara.launcher.core.theme.IosGlassTintDark
import com.niagara.launcher.core.theme.IosLabelShadow
import com.niagara.launcher.core.theme.IosPageIndicatorBg
import com.niagara.launcher.core.theme.IosPageIndicatorBorder
import com.niagara.launcher.core.theme.IosRemoveBadgeBg
import com.niagara.launcher.core.theme.IosRemoveBadgeIcon
import com.niagara.launcher.core.theme.IosRoundedCardSquircleShape
import com.niagara.launcher.core.theme.IosSquircleShape

/**
 * iOS-style frosted glass container card with layered translucent fill,
 * blurred background sheen, and hairline specular rim border.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IosFrostedGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardShape = remember(cornerRadius) { IosRoundedCardSquircleShape(cornerRadius) }
    val interactiveModifier = if (onClick != null || onLongClick != null) {
        Modifier.combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = { onClick?.invoke() },
            onLongClick = onLongClick
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(cardShape)
            .then(interactiveModifier)
            .background(IosGlassTintDark)
            .background(
                Brush.verticalGradient(
                    colors = listOf(IosGlassFillTop, IosGlassFillBottom)
                )
            )
            .border(
                width = 0.8.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(IosGlassBorderTop, IosGlassBorderBottom)
                ),
                shape = cardShape
            )
    ) {
        // Blurred internal frosted sheen kept strictly behind foreground content
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(24.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.14f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.06f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}

/**
 * Single iOS home screen app icon tile with continuous-curvature squircle shape,
 * shadowed white label, and jiggle edit animation with remove badge.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IosAppIconTile(
    app: AppItem,
    index: Int,
    isEditMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val squircleShape = remember { IosSquircleShape() }

    val jiggleAngle: Float
    val jiggleOffsetY: Float
    if (isEditMode) {
        val infiniteTransition = rememberInfiniteTransition(label = "jiggle_$index")
        val direction = if (index % 2 == 0) 1f else -1f
        val durationMs = 240 + (index % 4) * 14
        val angle by infiniteTransition.animateFloat(
            initialValue = -2.1f * direction,
            targetValue = 2.1f * direction,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = durationMs, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "jiggleAngle"
        )
        val offsetY by infiniteTransition.animateFloat(
            initialValue = -0.8f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = durationMs + 30, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "jiggleOffsetY"
        )
        jiggleAngle = angle
        jiggleOffsetY = offsetY
    } else {
        jiggleAngle = 0f
        jiggleOffsetY = 0f
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .graphicsLayer {
                rotationZ = jiggleAngle
                translationY = jiggleOffsetY
            }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (!isEditMode) {
                        onClick()
                    }
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .padding(vertical = 2.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(62.dp)
        ) {
            AppIcon(
                app = app,
                shape = squircleShape,
                modifier = Modifier
                    .size(56.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = squircleShape,
                        ambientColor = Color.Black.copy(alpha = 0.45f),
                        spotColor = Color.Black.copy(alpha = 0.55f)
                    )
                    .border(
                        width = 0.5.dp,
                        color = Color.White.copy(alpha = 0.16f),
                        shape = squircleShape
                    )
            )

            if (isEditMode) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-1).dp, y = (-1).dp)
                        .size(21.dp)
                        .shadow(2.dp, CircleShape)
                        .clip(CircleShape)
                        .background(IosRemoveBadgeBg)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onRemoveClick()
                        }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Remove,
                        contentDescription = "移除 ${app.label}",
                        tint = IosRemoveBadgeIcon,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = app.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.sp,
                shadow = Shadow(
                    color = IosLabelShadow,
                    offset = Offset(0f, 1.5f),
                    blurRadius = 4f
                )
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
        )
    }
}

/**
 * Dashed/frosted "+" add slot matching iOS squircle icon geometry.
 */
@Composable
fun IosAddIconSlot(
    label: String = "添加",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val squircleShape = remember { IosSquircleShape() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 2.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(62.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(squircleShape)
                    .background(Color.White.copy(alpha = 0.14f))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.30f),
                        shape = squircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = label,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.sp,
                shadow = Shadow(
                    color = IosLabelShadow,
                    offset = Offset(0f, 1.5f),
                    blurRadius = 4f
                )
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
        )
    }
}

/**
 * Top Frosted Glass Favorites Card on the iOS Home Page with 4-column squircle grid.
 */
@Composable
fun IosFavoritesCard(
    favorites: List<AppItem>,
    isEditMode: Boolean,
    onLaunchApp: (AppItem) -> Unit,
    onEnterEditMode: () -> Unit,
    onRemoveFavorite: (AppItem) -> Unit,
    onOpenFavoritesPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    IosFrostedGlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 28.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
        onLongClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onOpenFavoritesPicker()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "常用应用",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.2.sp
                )
            )

            Surface(
                onClick = onOpenFavoritesPicker,
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.16f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (favorites.isEmpty()) "添加" else "管理",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (favorites.isEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenFavoritesPicker
                    )
            ) {
                IosAddIconSlot(
                    label = "添加常用",
                    onClick = onOpenFavoritesPicker,
                    modifier = Modifier.width(72.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "固定常用应用到这里",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "长按此卡片或点击右上角即可添加/移除常用应用",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.62f),
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 0.sp
                        )
                    )
                }
            }
        } else {
            val slotCount = if (isEditMode) favorites.size + 1 else favorites.size
            val rows = (slotCount + LauncherPreferences.FAVORITES_GRID_COLUMNS - 1) /
                LauncherPreferences.FAVORITES_GRID_COLUMNS

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (rowIndex in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (colIndex in 0 until LauncherPreferences.FAVORITES_GRID_COLUMNS) {
                            val index = rowIndex * LauncherPreferences.FAVORITES_GRID_COLUMNS + colIndex
                            if (index < favorites.size) {
                                val app = favorites[index]
                                IosAppIconTile(
                                    app = app,
                                    index = index,
                                    isEditMode = isEditMode,
                                    onClick = { onLaunchApp(app) },
                                    onLongClick = { onEnterEditMode() },
                                    onRemoveClick = { onRemoveFavorite(app) },
                                    modifier = Modifier.weight(1f)
                                )
                            } else if (isEditMode && index == favorites.size) {
                                IosAddIconSlot(
                                    label = "添加",
                                    onClick = onOpenFavoritesPicker,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 4x6 iOS Desktop App Grid with squircle icons, shadowed labels, and edit mode jiggle.
 */
@Composable
fun IosDesktopGrid(
    desktopApps: List<AppItem>,
    isEditMode: Boolean,
    onLaunchApp: (AppItem) -> Unit,
    onEnterEditMode: () -> Unit,
    onRemoveDesktopApp: (AppItem) -> Unit,
    onOpenDesktopPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleDesktopApps = desktopApps.take(LauncherPreferences.MAX_DESKTOP_APPS)

    if (visibleDesktopApps.isEmpty() && !isEditMode) {
        IosFrostedGlassCard(
            modifier = modifier.fillMaxWidth(),
            cornerRadius = 24.dp,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 28.dp),
            onClick = onOpenDesktopPicker,
            onLongClick = onEnterEditMode
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.AddCircleOutline,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(34.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "桌面图标区 (4×6)",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "点击添加桌面应用，或长按空白处进入编辑模式",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.68f)
                    )
                )
            }
        }
        return
    }

    val showAddSlot = isEditMode && visibleDesktopApps.size < LauncherPreferences.MAX_DESKTOP_APPS
    val totalSlots = visibleDesktopApps.size + if (showAddSlot) 1 else 0
    val rows = ((totalSlots + LauncherPreferences.DESKTOP_GRID_COLUMNS - 1) /
        LauncherPreferences.DESKTOP_GRID_COLUMNS)
        .coerceAtMost(LauncherPreferences.DESKTOP_GRID_ROWS)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        for (rowIndex in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (colIndex in 0 until LauncherPreferences.DESKTOP_GRID_COLUMNS) {
                    val index = rowIndex * LauncherPreferences.DESKTOP_GRID_COLUMNS + colIndex
                    if (index < visibleDesktopApps.size) {
                        val app = visibleDesktopApps[index]
                        IosAppIconTile(
                            app = app,
                            index = index,
                            isEditMode = isEditMode,
                            onClick = { onLaunchApp(app) },
                            onLongClick = onEnterEditMode,
                            onRemoveClick = { onRemoveDesktopApp(app) },
                            modifier = Modifier.weight(1f)
                        )
                    } else if (showAddSlot && index == visibleDesktopApps.size) {
                        IosAddIconSlot(
                            label = "添加",
                            onClick = onOpenDesktopPicker,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * iOS-style frosted capsule page indicator at the bottom of the launcher.
 */
@Composable
fun IosPageIndicator(
    pageCount: Int,
    currentPage: Int,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .clip(CircleShape)
            .background(IosPageIndicatorBg)
            .border(0.6.dp, IosPageIndicatorBorder, CircleShape)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        for (page in 0 until pageCount) {
            val isSelected = page == currentPage
            val dotWidth by animateDpAsState(
                targetValue = if (isSelected) 16.dp else 6.dp,
                label = "dotWidth_$page"
            )
            val dotColor by animateColorAsState(
                targetValue = if (isSelected) IosDotActive else IosDotInactive,
                label = "dotColor_$page"
            )

            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(dotWidth)
                    .clip(CircleShape)
                    .background(dotColor)
                    .clickable { onPageSelected(page) }
            )
        }
    }
}
