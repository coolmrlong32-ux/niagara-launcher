package com.niagara.launcher.ui.home.components

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagara.launcher.core.gesture.DoubleTapLockGuard
import com.niagara.launcher.core.gesture.TapOutcome
import com.niagara.launcher.core.model.AppItem
import com.niagara.launcher.core.theme.IosPillShape
import com.niagara.launcher.core.theme.LocalLauncherVisuals
import com.niagara.launcher.core.transition.LaunchSourceBounds
import com.niagara.launcher.ui.home.HomeListEntry
import kotlinx.coroutines.launch

/**
 * Redesigned one-handed "All Apps" Page 2 featuring:
 * - Pure minimalist clock header (no weather or device telemetry tags)
 * - Right-side retractable wave bar (plus optional invisible left hotspot)
 * - Bottom-anchored locked-letter view so filtered apps sit within comfortable thumb reach
 * - Bottom one-handed floating dock for Search, Back-to-Top, Unlock, and Settings
 */
@Composable
fun AllAppsDrawerPage(
    listEntries: List<HomeListEntry>,
    allAppsCount: Int,
    availableLetters: Set<Char>,
    letterCounts: Map<Char, Int>,
    lockedLetter: Char?,
    lockedApps: List<AppItem>,
    listState: LazyListState,
    isRightHanded: Boolean,
    hapticFeedbackEnabled: Boolean,
    doubleTapToLockEnabled: Boolean,
    isOverlayBlocking: Boolean,
    onLaunchApp: (AppItem, LaunchSourceBounds?) -> Unit,
    onOpenAppMenu: (AppItem) -> Unit,
    onLetterSelected: (Char) -> Unit,
    onLetterLocked: (Char?) -> Unit,
    onClearLockedLetter: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onScrubStateChanged: (Boolean) -> Unit,
    onDoubleTapLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visuals = LocalLauncherVisuals.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var leftEdgeFocusedLetter by remember { mutableStateOf<Char?>(null) }
    var isRightScrubbing by remember { mutableStateOf(false) }
    var isLeftScrubbing by remember { mutableStateOf(false) }
    val isAlphabetScrubbing = isRightScrubbing || isLeftScrubbing

    LaunchedEffect(isAlphabetScrubbing) {
        onScrubStateChanged(isAlphabetScrubbing)
    }

    val doubleTapGuard = remember { DoubleTapLockGuard() }
    val maxTapDistancePx = with(density) { 28.dp.toPx() }

    LaunchedEffect(isOverlayBlocking, isAlphabetScrubbing, lockedLetter) {
        if (isOverlayBlocking || isAlphabetScrubbing || lockedLetter != null) {
            doubleTapGuard.reset()
        }
    }

    val latestLockedLetter by rememberUpdatedState(lockedLetter)
    val latestIsOverlayBlocking by rememberUpdatedState(isOverlayBlocking)
    val latestIsScrubbing by rememberUpdatedState(isAlphabetScrubbing)
    val latestDoubleTapLockEnabled by rememberUpdatedState(doubleTapToLockEnabled)
    val latestOnClearLockedLetter by rememberUpdatedState(onClearLockedLetter)
    val latestOnDoubleTapLock by rememberUpdatedState(onDoubleTapLock)

    val activeSectionLetter by remember(listEntries) {
        derivedStateOf {
            listEntries.getOrNull(listState.firstVisibleItemIndex)?.sectionLetter
        }
    }
    val canScrollToTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 2
        }
    }

    val guardedLaunchApp: (AppItem, LaunchSourceBounds?) -> Unit = { app, bounds ->
        doubleTapGuard.reset()
        onLaunchApp(app, bounds)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(maxTapDistancePx) {
                detectTapGestures(
                    onTap = { offset ->
                        if (latestLockedLetter != null) {
                            doubleTapGuard.reset()
                            latestOnClearLockedLetter()
                        } else {
                            val outcome = doubleTapGuard.onEmptySpaceTap(
                                x = offset.x,
                                y = offset.y,
                                maxDistancePx = maxTapDistancePx,
                                nowMs = SystemClock.uptimeMillis(),
                                enabled = latestDoubleTapLockEnabled,
                                isBlockedByMode = latestIsOverlayBlocking || latestIsScrubbing
                            )
                            if (outcome == TapOutcome.DOUBLE_TAP_LOCK) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                latestOnDoubleTapLock()
                            }
                        }
                    }
                )
            }
    ) {
        AnimatedContent(
            targetState = lockedLetter,
            transitionSpec = {
                (fadeIn() + slideInVertically { it / 16 }) togetherWith
                    (fadeOut() + slideOutVertically { -it / 16 })
            },
            label = "drawerLockedTransition",
            modifier = Modifier.fillMaxSize()
        ) { targetLockedLetter ->
            if (targetLockedLetter == null) {
                // Full A-Z Drawer
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 34.dp)
                ) {
                    items(
                        items = listEntries,
                        key = { it.listKey },
                        contentType = { entry ->
                            when (entry) {
                                HomeListEntry.Glance -> "glance"
                                HomeListEntry.FavoritesHeader -> "fav_header"
                                is HomeListEntry.FavoriteApp -> "app_item"
                                HomeListEntry.FavoritesDivider -> "divider"
                                is HomeListEntry.LetterHeader -> "letter_header"
                                is HomeListEntry.DrawerApp -> "app_item"
                                HomeListEntry.BottomSpacer -> "spacer"
                            }
                        }
                    ) { entry ->
                        when (entry) {
                            HomeListEntry.Glance -> {
                                GlanceWidget(
                                    subtitle = "共 $allAppsCount 个应用"
                                )
                            }

                            HomeListEntry.FavoritesHeader -> {
                                DrawerSectionHeader(
                                    title = "常用",
                                    accentColor = visuals.textSecondary
                                )
                            }

                            is HomeListEntry.FavoriteApp -> {
                                AppListItem(
                                    app = entry.app,
                                    onClickWithBounds = { bounds ->
                                        guardedLaunchApp(entry.app, bounds)
                                    },
                                    onLongClick = {
                                        doubleTapGuard.reset()
                                        onOpenAppMenu(entry.app)
                                    }
                                )
                            }

                            HomeListEntry.FavoritesDivider -> {
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            is HomeListEntry.LetterHeader -> {
                                DrawerSectionHeader(
                                    title = entry.letter.toString(),
                                    accentColor = visuals.accent
                                )
                            }

                            is HomeListEntry.DrawerApp -> {
                                AppListItem(
                                    app = entry.app,
                                    onClickWithBounds = { bounds ->
                                        guardedLaunchApp(entry.app, bounds)
                                    },
                                    onLongClick = {
                                        doubleTapGuard.reset()
                                        onOpenAppMenu(entry.app)
                                    }
                                )
                            }

                            HomeListEntry.BottomSpacer -> {
                                Spacer(modifier = Modifier.height(114.dp))
                            }
                        }
                    }
                }
            } else {
                // One-handed Bottom-Anchored Locked Letter View
                Column(modifier = Modifier.fillMaxSize()) {
                    GlanceWidget(
                        subtitle = "已锁定 $targetLockedLetter · ${lockedApps.size} 个应用"
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Bottom),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 114.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(end = 34.dp)
                    ) {
                        item(key = "locked_header_$targetLockedLetter") {
                            DrawerSectionHeader(
                                title = "$targetLockedLetter · ${lockedApps.size} 个应用",
                                accentColor = visuals.accent
                            )
                        }

                        items(
                            items = lockedApps,
                            key = { "locked_${it.key}" }
                        ) { app ->
                            AppListItem(
                                app = app,
                                onClickWithBounds = { bounds ->
                                    guardedLaunchApp(app, bounds)
                                },
                                onLongClick = {
                                    doubleTapGuard.reset()
                                    onOpenAppMenu(app)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Bottom one-handed control dock positioned directly above the page indicator
        DrawerBottomOneHandDock(
            lockedLetter = lockedLetter,
            lockedCount = lockedApps.size,
            canScrollToTop = canScrollToTop,
            onSearchClick = {
                doubleTapGuard.reset()
                onSearchClick()
            },
            onSettingsClick = {
                doubleTapGuard.reset()
                onSettingsClick()
            },
            onScrollToTopClick = {
                doubleTapGuard.reset()
                coroutineScope.launch {
                    listState.animateScrollToItem(0)
                }
            },
            onClearLockedLetter = {
                doubleTapGuard.reset()
                onClearLockedLetter()
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 48.dp, bottom = 44.dp)
        )

        // Left edge: invisible vertical alphabet scrubbing hotspot
        if (isRightHanded) {
            InvisibleLeftAlphabetHotspot(
                hapticFeedbackEnabled = hapticFeedbackEnabled,
                availableLetters = availableLetters,
                letterCounts = letterCounts,
                onLetterSelected = onLetterSelected,
                onLetterLocked = onLetterLocked,
                onFocusedLetterChanged = { leftEdgeFocusedLetter = it },
                onScrubStateChanged = { isLeftScrubbing = it },
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }

        // Right edge: retractable one-handed alphabet wave bar (preserved vertical span)
        AlphabetWaveBar(
            isRightHanded = true,
            hapticFeedbackEnabled = hapticFeedbackEnabled,
            availableLetters = availableLetters,
            activeLetter = activeSectionLetter,
            externalFocusedLetter = leftEdgeFocusedLetter,
            lockedLetter = lockedLetter,
            letterCounts = letterCounts,
            onLetterSelected = onLetterSelected,
            onLetterLocked = onLetterLocked,
            onScrubStateChanged = { isRightScrubbing = it },
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
private fun DrawerBottomOneHandDock(
    lockedLetter: Char?,
    lockedCount: Int,
    canScrollToTop: Boolean,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onScrollToTopClick: () -> Unit,
    onClearLockedLetter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visuals = LocalLauncherVisuals.current

    AnimatedContent(
        targetState = lockedLetter,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "drawerBottomDock",
        modifier = modifier.fillMaxWidth()
    ) { activeLockedLetter ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    ambientColor = visuals.cardShadowAmbient,
                    spotColor = visuals.cardShadowSpot
                )
                .clip(CircleShape)
                .background(visuals.dockSurface)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.12f),
                            Color.White.copy(alpha = 0.03f)
                        )
                    )
                )
                .border(0.7.dp, visuals.dockBorder, CircleShape)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            if (activeLockedLetter == null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(IosPillShape)
                        .clickable(onClick = onSearchClick)
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "搜索应用",
                        tint = visuals.accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "搜索应用或拼音…",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = visuals.textSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                AnimatedVisibility(
                    visible = canScrollToTop,
                    enter = fadeIn() + scaleIn(initialScale = 0.85f),
                    exit = fadeOut() + scaleOut(targetScale = 0.85f)
                ) {
                    IosGlassPill(
                        shape = CircleShape,
                        onClick = onScrollToTopClick,
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowUp,
                            contentDescription = "回到顶部",
                            tint = visuals.textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                IosGlassPill(
                    shape = CircleShape,
                    onClick = onSettingsClick,
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = "启动器设置",
                        tint = visuals.textPrimary.copy(alpha = 0.92f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(visuals.accent.copy(alpha = 0.24f))
                            .border(0.7.dp, visuals.accent.copy(alpha = 0.72f), CircleShape)
                    ) {
                        Text(
                            text = activeLockedLetter.toString(),
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = visuals.accent,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(9.dp))
                    Text(
                        text = "${lockedCount} 个应用",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = visuals.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IosGlassPill(
                    onClick = onClearLockedLetter,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "返回全部应用",
                        tint = visuals.textPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "返回全部",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = visuals.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                IosGlassPill(
                    shape = CircleShape,
                    onClick = onSearchClick,
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "搜索",
                        tint = visuals.textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun DrawerSectionHeader(
    title: String,
    accentColor: Color
) {
    val visuals = LocalLauncherVisuals.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 5.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                color = accentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp,
                letterSpacing = 0.6.sp,
                shadow = Shadow(
                    color = visuals.labelShadow,
                    offset = Offset(0f, 1f),
                    blurRadius = 3f
                )
            )
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(0.5.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.16f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}
