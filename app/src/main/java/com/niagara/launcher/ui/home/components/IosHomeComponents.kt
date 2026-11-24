package com.niagara.launcher.ui.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.niagara.launcher.core.data.LocalIconAppearance
import com.niagara.launcher.core.model.AppItem
import com.niagara.launcher.core.model.DesktopLayoutMath
import com.niagara.launcher.core.model.GridReorderMath
import com.niagara.launcher.core.model.IconAppearance
import com.niagara.launcher.core.model.IconStyle
import com.niagara.launcher.core.model.LauncherPreferences
import com.niagara.launcher.core.theme.DragElevationHalo
import com.niagara.launcher.core.theme.IconShadowAmbient
import com.niagara.launcher.core.theme.IconShadowSpot
import com.niagara.launcher.core.theme.IconSpecularRim
import com.niagara.launcher.core.theme.IosDotActive
import com.niagara.launcher.core.theme.IosDotInactive
import com.niagara.launcher.core.theme.IosGlassSheenEnd
import com.niagara.launcher.core.theme.IosGlassSheenStart
import com.niagara.launcher.core.theme.IosPageIndicatorBg
import com.niagara.launcher.core.theme.IosPageIndicatorBorder
import com.niagara.launcher.core.theme.IosPillShape
import com.niagara.launcher.core.theme.IosRemoveBadgeBg
import com.niagara.launcher.core.theme.IosRemoveBadgeIcon
import com.niagara.launcher.core.theme.IosRoundedCardSquircleShape
import com.niagara.launcher.core.theme.LocalLauncherVisuals
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Restrained translucent glass pill surface with a subtle 0.6dp specular rim border.
 */
@Composable
fun IosGlassPill(
    modifier: Modifier = Modifier,
    shape: Shape = IosPillShape,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
    content: @Composable RowScope.() -> Unit
) {
    val visuals = LocalLauncherVisuals.current
    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(shape)
            .then(clickModifier)
            .background(visuals.pillTint)
            .background(
                Brush.verticalGradient(
                    colors = listOf(visuals.pillFill, Color.White.copy(alpha = 0.06f))
                )
            )
            .border(
                width = 0.6.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(visuals.pillBorder, Color.White.copy(alpha = 0.10f))
                ),
                shape = shape
            )
            .padding(contentPadding),
        content = content
    )
}

/**
 * iOS-style frosted glass container card with layered translucent depth,
 * subtle diagonal sheen, and a crisp 0.7dp specular hairline rim.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IosFrostedGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 15.dp, vertical = 15.dp),
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val visuals = LocalLauncherVisuals.current
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
            .background(visuals.glassTint)
            .background(
                Brush.verticalGradient(
                    colors = listOf(visuals.glassFillTop, visuals.glassFillBottom)
                )
            )
            .border(
                width = 0.7.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(visuals.glassBorderTop, visuals.glassBorderBottom)
                ),
                shape = cardShape
            )
    ) {
        // Soft internal frosted light diffusion strictly behind foreground content
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(26.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            IosGlassSheenStart,
                            Color.Transparent,
                            IosGlassSheenEnd
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
 * Single iOS home screen app icon tile with configurable contour, size, label visibility,
 * crisp micro-specular rim, drag-lift state, and edit-mode jiggle with remove badge.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IosAppIconTile(
    app: AppItem,
    index: Int,
    isEditMode: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDragged: Boolean = false,
    iconSizeDp: Float = LauncherPreferences.DEFAULT_ICON_SIZE_DP,
    gridColumns: Int = LauncherPreferences.DESKTOP_GRID_COLUMNS,
    showAppLabels: Boolean = true,
    iconAppearance: IconAppearance = LocalIconAppearance.current
) {
    val visuals = LocalLauncherVisuals.current
    val haptic = LocalHapticFeedback.current
    val iconShape = remember(iconAppearance.iconStyle) { resolveIconShape(iconAppearance.iconStyle) }
    val applyFrameChrome = iconAppearance.iconPackPackage == null && iconAppearance.iconStyle != IconStyle.ADAPTIVE

    val dragScale by animateFloatAsState(
        targetValue = if (isDragged) 1.12f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
        label = "dragScale_${app.key}"
    )
    val iconShadowDp by animateDpAsState(
        targetValue = if (isDragged) 18.dp else 7.dp,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
        label = "dragShadow_${app.key}"
    )

    val jiggleAngle: Float
    val jiggleOffsetY: Float
    if (isEditMode && !isDragged) {
        val infiniteTransition = rememberInfiniteTransition(label = "jiggle_$index")
        val direction = if (index % 2 == 0) 1f else -1f
        val durationMs = 240 + (index % 4) * 14
        val angle by infiniteTransition.animateFloat(
            initialValue = -1.9f * direction,
            targetValue = 1.9f * direction,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = durationMs, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "jiggleAngle"
        )
        val offsetY by infiniteTransition.animateFloat(
            initialValue = -0.7f,
            targetValue = 0.7f,
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

    val clickModifier = if (onLongClick != null) {
        Modifier.combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {
                if (!isEditMode && !isDragged) {
                    onClick()
                }
            },
            onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onLongClick()
            }
        )
    } else {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            enabled = !isEditMode && !isDragged,
            onClick = onClick
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .graphicsLayer {
                rotationZ = jiggleAngle
                translationY = jiggleOffsetY
                scaleX = dragScale
                scaleY = dragScale
                alpha = if (isDragged) 0.96f else 1f
            }
            .then(clickModifier)
            .padding(vertical = 2.dp)
    ) {
        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            val effectiveIconDp = DesktopLayoutMath.resolveIconSizeDp(
                requestedDp = iconSizeDp,
                availableCellWidthDp = maxWidth.value
            ).dp
            val containerDp = effectiveIconDp + 6.dp

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(containerDp)
            ) {
                val iconModifier = Modifier
                    .size(effectiveIconDp)
                    .then(
                        if (applyFrameChrome) {
                            Modifier
                                .shadow(
                                    elevation = iconShadowDp,
                                    shape = iconShape,
                                    ambientColor = IconShadowAmbient,
                                    spotColor = IconShadowSpot
                                )
                                .border(
                                    width = if (isDragged) 1.dp else 0.5.dp,
                                    color = if (isDragged) DragElevationHalo else IconSpecularRim,
                                    shape = iconShape
                                )
                        } else if (isDragged) {
                            Modifier.shadow(
                                elevation = iconShadowDp,
                                shape = iconShape,
                                ambientColor = IconShadowAmbient,
                                spotColor = IconShadowSpot
                            )
                        } else {
                            Modifier
                        }
                    )

                AppIcon(
                    app = app,
                    shape = iconShape,
                    iconStyle = iconAppearance.iconStyle,
                    iconPackPackage = iconAppearance.iconPackPackage,
                    modifier = iconModifier
                )

                if (isEditMode && !isDragged) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = (-1).dp, y = (-1).dp)
                            .size(21.dp)
                            .shadow(3.dp, CircleShape)
                            .clip(CircleShape)
                            .background(IosRemoveBadgeBg)
                            .border(0.5.dp, Color.White.copy(alpha = 0.65f), CircleShape)
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
        }

        if (showAppLabels) {
            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = app.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = visuals.textPrimary,
                    fontSize = DesktopLayoutMath.labelFontSizeSp(gridColumns).sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.sp,
                    shadow = Shadow(
                        color = visuals.labelShadow,
                        offset = Offset(0f, 1.4f),
                        blurRadius = 4f
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
            )
        }
    }
}

/**
 * Restrained frosted "+" add slot matching iOS icon geometry.
 */
@Composable
fun IosAddIconSlot(
    label: String = "添加",
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSizeDp: Float = LauncherPreferences.DEFAULT_ICON_SIZE_DP,
    gridColumns: Int = LauncherPreferences.DESKTOP_GRID_COLUMNS,
    showAppLabels: Boolean = true,
    iconStyle: IconStyle = LocalIconAppearance.current.iconStyle
) {
    val visuals = LocalLauncherVisuals.current
    val slotShape = remember(iconStyle) {
        if (iconStyle == IconStyle.ADAPTIVE) RoundedCornerShape(14.dp) else resolveIconShape(iconStyle)
    }

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
        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            val effectiveIconDp = DesktopLayoutMath.resolveIconSizeDp(
                requestedDp = iconSizeDp,
                availableCellWidthDp = maxWidth.value
            ).dp
            val containerDp = effectiveIconDp + 6.dp

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(containerDp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(effectiveIconDp)
                        .clip(slotShape)
                        .background(visuals.pillTint)
                        .background(Color.White.copy(alpha = 0.10f))
                        .border(
                            width = 0.75.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.34f),
                                    Color.White.copy(alpha = 0.14f)
                                )
                            ),
                            shape = slotShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = label,
                        tint = visuals.textPrimary.copy(alpha = 0.9f),
                        modifier = Modifier.size((effectiveIconDp * 0.44f).coerceAtLeast(18.dp))
                    )
                }
            }
        }

        if (showAppLabels) {
            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = visuals.textSecondary,
                    fontSize = DesktopLayoutMath.labelFontSizeSp(gridColumns).sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.sp,
                    shadow = Shadow(
                        color = visuals.labelShadow,
                        offset = Offset(0f, 1.4f),
                        blurRadius = 4f
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
            )
        }
    }
}

/**
 * Free-drag reorderable iOS app grid with:
 * - Long-press pickup with lift scale & elevated shadow
 * - Smooth spring sibling displacement as the dragged icon crosses slots
 * - Subtle frosted landing target indicator
 * - Spring drop settle on release & persistent order commit
 */
@Composable
fun ReorderableIosAppGrid(
    apps: List<AppItem>,
    columns: Int,
    isEditMode: Boolean,
    showAddSlot: Boolean,
    addSlotLabel: String,
    horizontalSpacing: Dp,
    verticalSpacing: Dp,
    iconSizeDp: Float,
    showAppLabels: Boolean,
    iconAppearance: IconAppearance,
    onLaunchApp: (AppItem) -> Unit,
    onEnterEditMode: () -> Unit,
    onRemoveApp: (AppItem) -> Unit,
    onAddSlotClick: () -> Unit,
    onReorderCommit: (List<String>) -> Unit,
    onDraggingChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val slotShape = remember(iconAppearance.iconStyle) {
        if (iconAppearance.iconStyle == IconStyle.ADAPTIVE) {
            RoundedCornerShape(14.dp)
        } else {
            resolveIconShape(iconAppearance.iconStyle)
        }
    }

    var workingApps by remember { mutableStateOf(apps) }
    var draggingKey by remember { mutableStateOf<String?>(null) }
    var dragDelta by remember { mutableStateOf(Offset.Zero) }
    var pendingCommittedKeys by remember { mutableStateOf<List<String>?>(null) }
    val dropStartOffsets = remember { mutableStateMapOf<String, Offset>() }

    // Keep working order synchronized with DataStore state without post-drop snapback flicker
    LaunchedEffect(apps, draggingKey, pendingCommittedKeys) {
        if (draggingKey == null) {
            val externalKeys = apps.map { it.key }
            val pending = pendingCommittedKeys
            if (pending == null || externalKeys == pending) {
                workingApps = apps
                pendingCommittedKeys = null
            }
        }
    }

    // Safety timeout in case a commit was dropped or normalized externally
    LaunchedEffect(pendingCommittedKeys) {
        if (pendingCommittedKeys != null) {
            delay(400)
            pendingCommittedKeys = null
            if (draggingKey == null) {
                workingApps = apps
            }
        }
    }

    val totalSlots = workingApps.size + if (showAddSlot) 1 else 0
    if (totalSlots == 0) return

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val hSpacePx = with(density) { horizontalSpacing.toPx() }
        val vSpacePx = with(density) { verticalSpacing.toPx() }
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val cellWidthPx = ((maxWidthPx - (columns - 1) * hSpacePx) / columns).coerceAtLeast(1f)
        val cellWidthDp = with(density) { cellWidthPx.toDp() }

        val effectiveIconDp = DesktopLayoutMath.resolveIconSizeDp(
            requestedDp = iconSizeDp,
            availableCellWidthDp = cellWidthDp.value
        ).dp
        val cellHeightDp = effectiveIconDp + 10.dp + if (showAppLabels) 22.dp else 0.dp
        val cellHeightPx = with(density) { cellHeightDp.toPx() }
        val gridHeightPx = GridReorderMath.calculateGridHeightPx(
            itemCount = totalSlots,
            columns = columns,
            cellHeightPx = cellHeightPx,
            verticalSpacingPx = vSpacePx
        )
        val gridHeightDp = with(density) { gridHeightPx.toDp() }

        val latestWorkingApps by rememberUpdatedState(workingApps)
        val latestExternalApps by rememberUpdatedState(apps)
        val latestColumns by rememberUpdatedState(columns)
        val latestIsEditMode by rememberUpdatedState(isEditMode)
        val latestCellWidthPx by rememberUpdatedState(cellWidthPx)
        val latestCellHeightPx by rememberUpdatedState(cellHeightPx)
        val latestHSpacePx by rememberUpdatedState(hSpacePx)
        val latestVSpacePx by rememberUpdatedState(vSpacePx)
        val latestOnEnterEditMode by rememberUpdatedState(onEnterEditMode)
        val latestOnReorderCommit by rememberUpdatedState(onReorderCommit)
        val latestOnDraggingChanged by rememberUpdatedState(onDraggingChanged)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeightDp)
        ) {
            // Subtle frosted placeholder indicating the target drop slot underneath the lifted icon
            val draggedIndex = draggingKey?.let { key ->
                workingApps.indexOfFirst { it.key == key }
            }?.takeIf { it >= 0 }

            if (draggedIndex != null) {
                val targetSlot = GridReorderMath.indexToCellOrigin(
                    index = draggedIndex,
                    columns = columns,
                    cellWidthPx = cellWidthPx,
                    cellHeightPx = cellHeightPx,
                    horizontalSpacingPx = hSpacePx,
                    verticalSpacingPx = vSpacePx
                )
                val ghostX by animateFloatAsState(
                    targetValue = targetSlot.x,
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                    label = "ghostSlotX"
                )
                val ghostY by animateFloatAsState(
                    targetValue = targetSlot.y,
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                    label = "ghostSlotY"
                )

                Box(
                    contentAlignment = Alignment.TopCenter,
                    modifier = Modifier
                        .offset { IntOffset(ghostX.roundToInt(), ghostY.roundToInt()) }
                        .width(cellWidthDp)
                        .height(cellHeightDp)
                        .padding(top = 5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(effectiveIconDp)
                            .clip(slotShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(0.7.dp, Color.White.copy(alpha = 0.24f), slotShape)
                    )
                }
            }

            // Render every app icon keyed by app.key so slot swaps animate smoothly with springs
            workingApps.forEachIndexed { index, app ->
                key(app.key) {
                    val targetOrigin = GridReorderMath.indexToCellOrigin(
                        index = index,
                        columns = columns,
                        cellWidthPx = cellWidthPx,
                        cellHeightPx = cellHeightPx,
                        horizontalSpacingPx = hSpacePx,
                        verticalSpacingPx = vSpacePx
                    )
                    val isDragged = draggingKey == app.key
                    val animX = remember(app.key) { Animatable(targetOrigin.x) }
                    val animY = remember(app.key) { Animatable(targetOrigin.y) }

                    LaunchedEffect(targetOrigin.x, targetOrigin.y, isDragged) {
                        if (!isDragged) {
                            val dropStart = dropStartOffsets.remove(app.key)
                            if (dropStart != null) {
                                animX.snapTo(dropStart.x)
                                animY.snapTo(dropStart.y)
                            }
                            val settleSpec = spring<Float>(
                                dampingRatio = 0.80f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                            coroutineScope {
                                launch { animX.animateTo(targetOrigin.x, settleSpec) }
                                launch { animY.animateTo(targetOrigin.y, settleSpec) }
                            }
                        }
                    }

                    val visualX = if (isDragged) targetOrigin.x + dragDelta.x else animX.value
                    val visualY = if (isDragged) targetOrigin.y + dragDelta.y else animY.value

                    Box(
                        modifier = Modifier
                            .zIndex(if (isDragged) 10f else 0f)
                            .offset { IntOffset(visualX.roundToInt(), visualY.roundToInt()) }
                            .width(cellWidthDp)
                            .height(cellHeightDp)
                            .pointerInput(app.key) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        draggingKey = app.key
                                        dragDelta = Offset.Zero
                                        latestOnDraggingChanged(true)
                                        if (!latestIsEditMode) {
                                            latestOnEnterEditMode()
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragDelta += dragAmount

                                        val currentList = latestWorkingApps
                                        val currentIdx = currentList.indexOfFirst { it.key == app.key }
                                        if (currentIdx >= 0 && currentList.size > 1) {
                                            val currentOrigin = GridReorderMath.indexToCellOrigin(
                                                index = currentIdx,
                                                columns = latestColumns,
                                                cellWidthPx = latestCellWidthPx,
                                                cellHeightPx = latestCellHeightPx,
                                                horizontalSpacingPx = latestHSpacePx,
                                                verticalSpacingPx = latestVSpacePx
                                            )
                                            val centerX = currentOrigin.x + dragDelta.x + latestCellWidthPx / 2f
                                            val centerY = currentOrigin.y + dragDelta.y + latestCellHeightPx / 2f

                                            val targetIdx = GridReorderMath.pointToTargetIndex(
                                                centerX = centerX,
                                                centerY = centerY,
                                                columns = latestColumns,
                                                itemCount = currentList.size,
                                                cellWidthPx = latestCellWidthPx,
                                                cellHeightPx = latestCellHeightPx,
                                                horizontalSpacingPx = latestHSpacePx,
                                                verticalSpacingPx = latestVSpacePx,
                                                currentIndex = currentIdx
                                            )

                                            if (targetIdx != currentIdx) {
                                                val nextOrigin = GridReorderMath.indexToCellOrigin(
                                                    index = targetIdx,
                                                    columns = latestColumns,
                                                    cellWidthPx = latestCellWidthPx,
                                                    cellHeightPx = latestCellHeightPx,
                                                    horizontalSpacingPx = latestHSpacePx,
                                                    verticalSpacingPx = latestVSpacePx
                                                )
                                                // Compensate slot origin jump so icon remains glued to user's finger
                                                dragDelta = Offset(
                                                    x = dragDelta.x + (currentOrigin.x - nextOrigin.x),
                                                    y = dragDelta.y + (currentOrigin.y - nextOrigin.y)
                                                )
                                                workingApps = GridReorderMath.reorder(
                                                    items = currentList,
                                                    fromIndex = currentIdx,
                                                    toIndex = targetIdx
                                                )
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        val finalList = latestWorkingApps
                                        val finalIdx = finalList.indexOfFirst { it.key == app.key }
                                        if (finalIdx >= 0) {
                                            val finalOrigin = GridReorderMath.indexToCellOrigin(
                                                index = finalIdx,
                                                columns = latestColumns,
                                                cellWidthPx = latestCellWidthPx,
                                                cellHeightPx = latestCellHeightPx,
                                                horizontalSpacingPx = latestHSpacePx,
                                                verticalSpacingPx = latestVSpacePx
                                            )
                                            dropStartOffsets[app.key] = Offset(
                                                x = finalOrigin.x + dragDelta.x,
                                                y = finalOrigin.y + dragDelta.y
                                            )
                                        }
                                        val committedKeys = finalList.map { it.key }
                                        val changed = committedKeys != latestExternalApps.map { it.key }
                                        draggingKey = null
                                        dragDelta = Offset.Zero
                                        latestOnDraggingChanged(false)
                                        if (changed) {
                                            pendingCommittedKeys = committedKeys
                                            latestOnReorderCommit(committedKeys)
                                        }
                                    },
                                    onDragCancel = {
                                        val finalList = latestWorkingApps
                                        val finalIdx = finalList.indexOfFirst { it.key == app.key }
                                        if (finalIdx >= 0) {
                                            val finalOrigin = GridReorderMath.indexToCellOrigin(
                                                index = finalIdx,
                                                columns = latestColumns,
                                                cellWidthPx = latestCellWidthPx,
                                                cellHeightPx = latestCellHeightPx,
                                                horizontalSpacingPx = latestHSpacePx,
                                                verticalSpacingPx = latestVSpacePx
                                            )
                                            dropStartOffsets[app.key] = Offset(
                                                x = finalOrigin.x + dragDelta.x,
                                                y = finalOrigin.y + dragDelta.y
                                            )
                                        }
                                        val committedKeys = finalList.map { it.key }
                                        val changed = committedKeys != latestExternalApps.map { it.key }
                                        draggingKey = null
                                        dragDelta = Offset.Zero
                                        latestOnDraggingChanged(false)
                                        if (changed) {
                                            pendingCommittedKeys = committedKeys
                                            latestOnReorderCommit(committedKeys)
                                        }
                                    }
                                )
                            }
                    ) {
                        IosAppIconTile(
                            app = app,
                            index = index,
                            isEditMode = isEditMode,
                            isDragged = isDragged,
                            onClick = { onLaunchApp(app) },
                            onLongClick = null,
                            onRemoveClick = { onRemoveApp(app) },
                            iconSizeDp = iconSizeDp,
                            gridColumns = columns,
                            showAppLabels = showAppLabels,
                            iconAppearance = iconAppearance,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Optional trailing "+" slot in edit mode
            if (showAddSlot) {
                val addOrigin = GridReorderMath.indexToCellOrigin(
                    index = workingApps.size,
                    columns = columns,
                    cellWidthPx = cellWidthPx,
                    cellHeightPx = cellHeightPx,
                    horizontalSpacingPx = hSpacePx,
                    verticalSpacingPx = vSpacePx
                )
                Box(
                    modifier = Modifier
                        .offset { IntOffset(addOrigin.x.roundToInt(), addOrigin.y.roundToInt()) }
                        .width(cellWidthDp)
                        .height(cellHeightDp)
                ) {
                    IosAddIconSlot(
                        label = addSlotLabel,
                        onClick = onAddSlotClick,
                        iconSizeDp = iconSizeDp,
                        gridColumns = columns,
                        showAppLabels = showAppLabels,
                        iconStyle = iconAppearance.iconStyle,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Top Frosted Glass Favorites Card on the iOS Home Page with free drag-and-drop reordering.
 */
@Composable
fun IosFavoritesCard(
    favorites: List<AppItem>,
    isEditMode: Boolean,
    onLaunchApp: (AppItem) -> Unit,
    onEnterEditMode: () -> Unit,
    onRemoveFavorite: (AppItem) -> Unit,
    onOpenFavoritesPicker: () -> Unit,
    onReorderFavorites: (List<String>) -> Unit = {},
    onDraggingChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    gridColumns: Int = LauncherPreferences.FAVORITES_GRID_COLUMNS,
    iconSizeDp: Float = LauncherPreferences.DEFAULT_ICON_SIZE_DP,
    showAppLabels: Boolean = true,
    iconAppearance: IconAppearance = LocalIconAppearance.current
) {
    val visuals = LocalLauncherVisuals.current
    val haptic = LocalHapticFeedback.current
    val columns = LauncherPreferences.normalizeGridColumns(gridColumns)
    val horizontalSpacing = DesktopLayoutMath.horizontalSpacingDp(columns).dp

    IosFrostedGlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 28.dp,
        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 14.dp),
        onLongClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onOpenFavoritesPicker()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isEditMode && favorites.size > 1) "常用应用 · 拖动排序" else "常用应用",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = visuals.textSecondary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp
                )
            )

            IosGlassPill(
                onClick = onOpenFavoritesPicker,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = visuals.textPrimary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = if (favorites.isEmpty()) "添加" else "管理",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = visuals.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.1.sp
                    )
                )
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
                    iconSizeDp = iconSizeDp,
                    gridColumns = columns,
                    showAppLabels = true,
                    iconStyle = iconAppearance.iconStyle,
                    modifier = Modifier.width(72.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "收纳常用应用",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = visuals.textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "长按卡片或点右上角管理，支持长按图标自由拖动排序",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = visuals.textTertiary,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 0.sp,
                            lineHeight = 16.sp
                        )
                    )
                }
            }
        } else {
            ReorderableIosAppGrid(
                apps = favorites,
                columns = columns,
                isEditMode = isEditMode,
                showAddSlot = isEditMode,
                addSlotLabel = "添加",
                horizontalSpacing = horizontalSpacing,
                verticalSpacing = 12.dp,
                iconSizeDp = iconSizeDp,
                showAppLabels = showAppLabels,
                iconAppearance = iconAppearance,
                onLaunchApp = onLaunchApp,
                onEnterEditMode = onEnterEditMode,
                onRemoveApp = onRemoveFavorite,
                onAddSlotClick = onOpenFavoritesPicker,
                onReorderCommit = onReorderFavorites,
                onDraggingChanged = onDraggingChanged
            )
        }
    }
}

/**
 * 4x6 or 5x6 iOS Desktop App Grid with free long-press drag-and-drop reordering.
 */
@Composable
fun IosDesktopGrid(
    desktopApps: List<AppItem>,
    isEditMode: Boolean,
    onLaunchApp: (AppItem) -> Unit,
    onEnterEditMode: () -> Unit,
    onOpenDesktopMenu: () -> Unit,
    onRemoveDesktopApp: (AppItem) -> Unit,
    onOpenDesktopPicker: () -> Unit,
    onReorderDesktopApps: (List<String>) -> Unit = {},
    onDraggingChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    gridColumns: Int = LauncherPreferences.DESKTOP_GRID_COLUMNS,
    maxDesktopApps: Int = LauncherPreferences.MAX_DESKTOP_APPS,
    iconSizeDp: Float = LauncherPreferences.DEFAULT_ICON_SIZE_DP,
    showAppLabels: Boolean = true,
    iconAppearance: IconAppearance = LocalIconAppearance.current
) {
    val visuals = LocalLauncherVisuals.current
    val columns = LauncherPreferences.normalizeGridColumns(gridColumns)
    val horizontalSpacing = DesktopLayoutMath.horizontalSpacingDp(columns).dp
    val visibleDesktopApps = desktopApps.take(maxDesktopApps)

    if (visibleDesktopApps.isEmpty() && !isEditMode) {
        IosFrostedGlassCard(
            modifier = modifier.fillMaxWidth(),
            cornerRadius = 26.dp,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 26.dp),
            onClick = onOpenDesktopPicker,
            onLongClick = onOpenDesktopMenu
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.AddCircleOutline,
                    contentDescription = null,
                    tint = visuals.textSecondary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "桌面图标区 (${columns}×${LauncherPreferences.DESKTOP_GRID_ROWS})",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = visuals.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "点击添加桌面应用，长按图标可自由拖动排序",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = visuals.textTertiary
                    )
                )
            }
        }
        return
    }

    val showAddSlot = isEditMode && visibleDesktopApps.size < maxDesktopApps
    ReorderableIosAppGrid(
        apps = visibleDesktopApps,
        columns = columns,
        isEditMode = isEditMode,
        showAddSlot = showAddSlot,
        addSlotLabel = "添加",
        horizontalSpacing = horizontalSpacing,
        verticalSpacing = 16.dp,
        iconSizeDp = iconSizeDp,
        showAppLabels = showAppLabels,
        iconAppearance = iconAppearance,
        onLaunchApp = onLaunchApp,
        onEnterEditMode = onEnterEditMode,
        onRemoveApp = onRemoveDesktopApp,
        onAddSlotClick = onOpenDesktopPicker,
        onReorderCommit = onReorderDesktopApps,
        onDraggingChanged = onDraggingChanged,
        modifier = modifier
    )
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
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        modifier = modifier
            .clip(CircleShape)
            .background(IosPageIndicatorBg)
            .border(0.6.dp, IosPageIndicatorBorder, CircleShape)
            .padding(horizontal = 13.dp, vertical = 6.dp)
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
                    .height(5.5.dp)
                    .width(dotWidth)
                    .clip(CircleShape)
                    .background(dotColor)
                    .clickable { onPageSelected(page) }
            )
        }
    }
}
