package com.niagara.launcher.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagara.launcher.core.data.LocalIconAppearance
import com.niagara.launcher.core.theme.LocalLauncherVisuals
import com.niagara.launcher.ui.home.components.AlphabetWaveBar
import com.niagara.launcher.ui.home.components.AppActionBottomSheet
import com.niagara.launcher.ui.home.components.AppListItem
import com.niagara.launcher.ui.home.components.AppPickerBottomSheet
import com.niagara.launcher.ui.home.components.DesktopMenuBottomSheet
import com.niagara.launcher.ui.home.components.DesktopSettingsScreen
import com.niagara.launcher.ui.home.components.GlanceWidget
import com.niagara.launcher.ui.home.components.IconPickerScreen
import com.niagara.launcher.ui.home.components.InvisibleLeftAlphabetHotspot
import com.niagara.launcher.ui.home.components.IosHomePage
import com.niagara.launcher.ui.home.components.IosPageIndicator
import com.niagara.launcher.ui.home.components.LauncherSettingsBottomSheet
import com.niagara.launcher.ui.search.SearchOverlay
import kotlinx.coroutines.launch

/**
 * Main Home screen with horizontal paging between the iOS-style Home page (page 0)
 * and the Niagara A-Z App Drawer (page 1), accompanied by an iOS Page Indicator.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val visuals = LocalLauncherVisuals.current
    val listState = rememberLazyListState()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var leftEdgeFocusedLetter by remember { mutableStateOf<Char?>(null) }
    var isHomeIconDragging by remember { mutableStateOf(false) }

    // Exit desktop edit mode automatically when swiping away from page 0
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != 0 && uiState.isHomeEditMode) {
            viewModel.setHomeEditMode(false)
        }
    }

    // Observe transient UI feedback events (e.g. Undo hide app, Desktop full)
    LaunchedEffect(viewModel) {
        viewModel.snackbarEvents.collect { event ->
            when (event) {
                is HomeSnackbarEvent.AppHidden -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "已隐藏「${event.app.label}」",
                        actionLabel = "撤销",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.unhideApp(event.app)
                    }
                }

                is HomeSnackbarEvent.DesktopFull -> {
                    snackbarHostState.showSnackbar(
                        message = "桌面已满（最多 ${event.maxCount} 个应用）",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    // Track current visible A-Z letter section while scrolling
    val activeSectionLetter by remember(uiState.listEntries) {
        derivedStateOf {
            val firstIdx = listState.firstVisibleItemIndex
            uiState.listEntries.getOrNull(firstIdx)?.sectionLetter
        }
    }

    // Handle system back button in priority order: Icon Picker -> Desktop Settings -> Search -> Edit Mode -> Drawer Scroll -> Home Page
    BackHandler(
        enabled = uiState.isIconPickerVisible ||
            uiState.isDesktopSettingsVisible ||
            uiState.isSearching ||
            uiState.isHomeEditMode ||
            pagerState.currentPage != 0 ||
            listState.firstVisibleItemIndex > 0
    ) {
        when {
            uiState.isIconPickerVisible -> viewModel.closeIconPicker()
            uiState.isDesktopSettingsVisible -> viewModel.closeDesktopSettings()
            uiState.isSearching -> viewModel.setSearching(false)
            uiState.isHomeEditMode -> viewModel.setHomeEditMode(false)
            pagerState.currentPage == 1 && listState.firstVisibleItemIndex > 0 -> {
                coroutineScope.launch {
                    listState.animateScrollToItem(0)
                }
            }
            pagerState.currentPage != 0 -> {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(0)
                }
            }
        }
    }

    val onLetterSelected: (Char) -> Unit = { letter ->
        val targetIndex = HomeListModel.resolveTargetLetterIndex(letter, uiState.letterIndexMap)
        if (targetIndex != null) {
            coroutineScope.launch {
                listState.scrollToItem(targetIndex)
            }
        }
    }

    val previewApps = remember(uiState.favorites, uiState.desktopApps, uiState.allApps) {
        (uiState.favorites + uiState.desktopApps + uiState.allApps)
            .distinctBy { it.key }
            .take(5)
    }

    CompositionLocalProvider(LocalIconAppearance provides uiState.preferences.iconAppearance) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            visuals.scrimTop,
                            visuals.scrimMiddle,
                            visuals.scrimBottom
                        )
                    )
                )
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = !uiState.isSearching &&
                        !uiState.isDesktopSettingsVisible &&
                        !uiState.isIconPickerVisible &&
                        !isHomeIconDragging,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> {
                            IosHomePage(
                                favorites = uiState.favorites,
                                desktopApps = uiState.desktopApps,
                                isEditMode = uiState.isHomeEditMode,
                                preferences = uiState.preferences,
                                onLaunchApp = { viewModel.launchApp(it) },
                                onEnterEditMode = { viewModel.setHomeEditMode(true) },
                                onExitEditMode = { viewModel.setHomeEditMode(false) },
                                onOpenDesktopMenu = { viewModel.openDesktopMenu() },
                                onOpenDesktopSettings = { viewModel.openDesktopSettings() },
                                onRemoveFavorite = { viewModel.removeFavorite(it) },
                                onRemoveDesktopApp = { viewModel.removeDesktopApp(it) },
                                onReorderFavorites = { viewModel.reorderFavorites(it) },
                                onReorderDesktopApps = { viewModel.reorderDesktopApps(it) },
                                onDraggingChanged = { isHomeIconDragging = it },
                                onOpenFavoritesPicker = { viewModel.openAppPicker(AppPickerMode.FAVORITES) },
                                onOpenDesktopPicker = { viewModel.openAppPicker(AppPickerMode.DESKTOP) },
                                onSearchClick = { viewModel.setSearching(true) }
                            )
                        }

                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding()
                                    .navigationBarsPadding()
                            ) {
                                // Single-source-of-truth LazyColumn with end clearance for the visible right bar
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(end = 30.dp)
                                ) {
                                    items(
                                        items = uiState.listEntries,
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
                                                    showBattery = uiState.preferences.showBatteryInGlance,
                                                    onSearchClick = { viewModel.setSearching(true) },
                                                    onSettingsClick = { viewModel.openSettings() }
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
                                                    onClick = { viewModel.launchApp(entry.app) },
                                                    onLongClick = { viewModel.openAppMenu(entry.app) }
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
                                                    onClick = { viewModel.launchApp(entry.app) },
                                                    onLongClick = { viewModel.openAppMenu(entry.app) }
                                                )
                                            }

                                            HomeListEntry.BottomSpacer -> {
                                                Spacer(modifier = Modifier.height(88.dp))
                                            }
                                        }
                                    }
                                }

                                // Left edge: completely invisible vertical alphabet scrubbing hotspot
                                if (uiState.preferences.isRightHanded) {
                                    InvisibleLeftAlphabetHotspot(
                                        hapticFeedbackEnabled = uiState.preferences.hapticFeedbackEnabled,
                                        onLetterSelected = onLetterSelected,
                                        onFocusedLetterChanged = { leftEdgeFocusedLetter = it },
                                        modifier = Modifier.align(Alignment.CenterStart)
                                    )
                                }

                                // Right edge: sole visible compact one-handed alphabet wave bar
                                AlphabetWaveBar(
                                    isRightHanded = true,
                                    hapticFeedbackEnabled = uiState.preferences.hapticFeedbackEnabled,
                                    availableLetters = uiState.availableLetters,
                                    activeLetter = activeSectionLetter,
                                    externalFocusedLetter = leftEdgeFocusedLetter,
                                    onLetterSelected = onLetterSelected,
                                    modifier = Modifier.align(Alignment.CenterEnd)
                                )
                            }
                        }
                    }
                }

                // Bottom iOS-style Page Indicator (hidden while full-screen Search is open)
                if (!uiState.isSearching) {
                    IosPageIndicator(
                        pageCount = 2,
                        currentPage = pagerState.currentPage,
                        onPageSelected = { targetPage ->
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(targetPage)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 10.dp)
                    )
                }
            }

            // Undo / status Snackbar host
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 56.dp)
            )

            // Search Overlay
            SearchOverlay(
                visible = uiState.isSearching,
                query = uiState.searchQuery,
                results = uiState.searchResults,
                hiddenResults = uiState.hiddenSearchResults,
                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                onDismiss = { viewModel.setSearching(false) },
                onAppClick = { app ->
                    viewModel.setSearching(false)
                    viewModel.launchApp(app)
                },
                onAppLongClick = { app ->
                    viewModel.openAppMenu(app)
                }
            )

            // App Action Bottom Sheet (Long-press)
            uiState.selectedAppForMenu?.let { app ->
                val favIndex = uiState.favorites.indexOfFirst { it.key == app.key }
                val canMoveUp = favIndex > 0
                val canMoveDown = favIndex >= 0 && favIndex < uiState.favorites.lastIndex

                AppActionBottomSheet(
                    app = app,
                    canMoveFavoriteUp = canMoveUp,
                    canMoveFavoriteDown = canMoveDown,
                    onDismissRequest = { viewModel.closeAppMenu() },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onMoveFavoriteUp = { viewModel.moveFavoriteUp(it) },
                    onMoveFavoriteDown = { viewModel.moveFavoriteDown(it) },
                    onToggleDesktop = { viewModel.toggleDesktopApp(it) },
                    onToggleHidden = { viewModel.toggleHidden(it) },
                    onOpenDetails = { viewModel.openAppDetails(it) },
                    onUninstall = { viewModel.uninstallApp(it) }
                )
            }

            // Multi-select Picker for Favorites or 4x6 Desktop Grid
            uiState.appPickerMode?.let { pickerMode ->
                when (pickerMode) {
                    AppPickerMode.FAVORITES -> {
                        AppPickerBottomSheet(
                            mode = pickerMode,
                            allApps = uiState.allApps,
                            selectedKeys = uiState.favorites.map { it.key }.toSet(),
                            maxSelection = null,
                            onToggleApp = { viewModel.toggleFavorite(it) },
                            onDismissRequest = { viewModel.closeAppPicker() }
                        )
                    }

                    AppPickerMode.DESKTOP -> {
                        AppPickerBottomSheet(
                            mode = pickerMode,
                            allApps = uiState.allApps,
                            selectedKeys = uiState.desktopApps.map { it.key }.toSet(),
                            maxSelection = uiState.preferences.maxDesktopApps,
                            onToggleApp = { viewModel.toggleDesktopApp(it) },
                            onDismissRequest = { viewModel.closeAppPicker() }
                        )
                    }
                }
            }

            // iOS-style Bottom Sheet Menu on Home Screen empty-space long press
            if (uiState.isDesktopMenuVisible) {
                DesktopMenuBottomSheet(
                    preferences = uiState.preferences,
                    installedIconPacks = uiState.installedIconPacks,
                    onDismissRequest = { viewModel.closeDesktopMenu() },
                    onOpenIconPicker = { viewModel.openIconPicker() },
                    onOpenDesktopSettings = { viewModel.openDesktopSettings() },
                    onEnterEditMode = { viewModel.setHomeEditMode(true) },
                    onOpenDesktopPicker = { viewModel.openAppPicker(AppPickerMode.DESKTOP) }
                )
            }

            // Launcher Settings & Hidden Apps Management Sheet
            if (uiState.isSettingsSheetVisible) {
                LauncherSettingsBottomSheet(
                    preferences = uiState.preferences,
                    hiddenApps = uiState.hiddenApps,
                    onDismissRequest = { viewModel.closeSettings() },
                    onRightHandedChange = { viewModel.setRightHanded(it) },
                    onHapticFeedbackChange = { viewModel.setHapticFeedbackEnabled(it) },
                    onShowBatteryChange = { viewModel.setShowBatteryInGlance(it) },
                    onUnhideApp = { viewModel.unhideApp(it) },
                    onOpenDesktopSettings = { viewModel.openDesktopSettings() }
                )
            }

            // Full-screen iOS Settings Inset Grouped List: Desktop Settings
            AnimatedVisibility(
                visible = uiState.isDesktopSettingsVisible,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                DesktopSettingsScreen(
                    preferences = uiState.preferences,
                    previewApps = previewApps,
                    installedIconPacks = uiState.installedIconPacks,
                    hiddenAppsCount = uiState.hiddenApps.size,
                    onDismiss = { viewModel.closeDesktopSettings() },
                    onIconSizePreview = { viewModel.previewDesktopIconSizeDp(it) },
                    onIconSizeCommit = { viewModel.commitDesktopIconSizeDp(it) },
                    onGridColumnsChange = { viewModel.setDesktopGridColumns(it) },
                    onShowAppLabelsChange = { viewModel.setShowAppLabels(it) },
                    onShowFavoritesBarChange = { viewModel.setShowFavoritesBar(it) },
                    onOpenIconPicker = { viewModel.openIconPicker() },
                    onEnterEditMode = {
                        viewModel.closeDesktopSettings()
                        viewModel.setHomeEditMode(true)
                    },
                    onOpenLauncherSettings = { viewModel.openSettings() }
                )
            }

            // Full-screen iOS Settings Inset Grouped List: Icon Style & Third-Party Icon Pack Picker
            AnimatedVisibility(
                visible = uiState.isIconPickerVisible,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                IconPickerScreen(
                    preferences = uiState.preferences,
                    previewApps = previewApps,
                    installedIconPacks = uiState.installedIconPacks,
                    isRefreshingIconPacks = uiState.isRefreshingIconPacks,
                    showBackToSettings = uiState.isDesktopSettingsVisible,
                    onSelectIconStyle = { viewModel.setIconStyle(it) },
                    onSelectIconPack = { viewModel.setIconPackPackage(it) },
                    onRefreshIconPacks = { viewModel.refreshIconPacks() },
                    onBack = { viewModel.closeIconPicker() },
                    onDone = { viewModel.closeAllDesktopOverlays() }
                )
            }
        }
    }
}

@Composable
private fun DrawerSectionHeader(
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
