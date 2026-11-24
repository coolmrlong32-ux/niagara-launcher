package com.niagara.launcher.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.niagara.launcher.core.data.LocalIconAppearance
import com.niagara.launcher.core.model.AppItem
import com.niagara.launcher.core.system.ScreenLockController
import com.niagara.launcher.core.system.ScreenLockResult
import com.niagara.launcher.core.theme.LocalLauncherVisuals
import com.niagara.launcher.core.transition.LaunchSourceBounds
import com.niagara.launcher.ui.home.components.AllAppsDrawerPage
import com.niagara.launcher.ui.home.components.AppActionBottomSheet
import com.niagara.launcher.ui.home.components.AppPickerBottomSheet
import com.niagara.launcher.ui.home.components.DesktopMenuBottomSheet
import com.niagara.launcher.ui.home.components.DesktopSettingsScreen
import com.niagara.launcher.ui.home.components.IconPickerScreen
import com.niagara.launcher.ui.home.components.IosHomePage
import com.niagara.launcher.ui.home.components.IosPageIndicator
import com.niagara.launcher.ui.home.components.LauncherSettingsBottomSheet
import com.niagara.launcher.ui.home.components.ScreenLockGuideDialog
import com.niagara.launcher.ui.home.components.WeatherConfigDialog
import com.niagara.launcher.ui.home.components.rememberForegroundDeviceStatus
import com.niagara.launcher.ui.search.SearchOverlay
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Main Home screen with horizontal paging between the iOS-style Home page (page 0)
 * and the Niagara A-Z App Drawer (page 1), accompanied by an iOS Page Indicator.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    returnToHomePulse: Int = 0,
    openSettingsPulse: Int = 0,
    returnToPageZeroPulse: Int = 0,
    onExternalAppLaunched: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val visuals = LocalLauncherVisuals.current
    val context = LocalContext.current
    val hostView = LocalView.current
    val density = LocalDensity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val listState = rememberLazyListState()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isHomeIconDragging by remember { mutableStateOf(false) }
    var isAlphabetScrubbing by remember { mutableStateOf(false) }

    // Foreground-only low-frequency device telemetry (RAM usage, battery temp & level)
    val deviceStatus = rememberForegroundDeviceStatus(
        enabled = uiState.preferences.showDeviceStatusInGlance || uiState.preferences.showBatteryInGlance
    )

    // Lazy background-safe weather refresh while foregrounded (never blocks cold startup)
    LaunchedEffect(
        lifecycleOwner,
        uiState.preferences.showWeatherInGlance,
        uiState.preferences.weatherLocationQuery
    ) {
        if (!uiState.preferences.showWeatherInGlance || uiState.preferences.weatherLocationQuery.isBlank()) {
            return@LaunchedEffect
        }
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            delay(650)
            while (true) {
                viewModel.refreshWeatherIfStale(force = false)
                delay(15L * 60L * 1000L)
            }
        }
    }

    val onWeatherTap: () -> Unit = {
        if (uiState.preferences.weatherLocationQuery.isBlank()) {
            viewModel.openWeatherConfigDialog()
        } else {
            viewModel.refreshWeatherIfStale(force = true)
        }
    }

    // Restrained, non-blocking spring settle when returning from another app to the launcher
    val returnScale = remember { Animatable(1f) }
    val returnAlpha = remember { Animatable(1f) }
    val returnOffsetY = remember { Animatable(0f) }

    LaunchedEffect(returnToHomePulse) {
        if (returnToHomePulse > 0) {
            val startOffsetPx = with(density) { 10.dp.toPx() }
            returnScale.snapTo(0.955f)
            returnAlpha.snapTo(0.80f)
            returnOffsetY.snapTo(startOffsetPx)

            val settleSpring = spring<Float>(dampingRatio = 0.84f, stiffness = 420f)
            launch { returnScale.animateTo(1f, settleSpring) }
            launch { returnOffsetY.animateTo(0f, settleSpring) }
            launch {
                returnAlpha.animateTo(
                    1f,
                    tween(durationMillis = 160, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    val launchWithTransition: (AppItem, LaunchSourceBounds?) -> Unit = { app, bounds ->
        val launched = viewModel.launchApp(
            app = app,
            anchorView = hostView,
            sourceBounds = bounds
        )
        if (launched && app.packageName != context.packageName) {
            onExternalAppLaunched()
        }
    }

    LaunchedEffect(openSettingsPulse) {
        if (openSettingsPulse > 0) {
            viewModel.closeAllDesktopOverlays()
            viewModel.openDesktopSettings()
        }
    }

    LaunchedEffect(returnToPageZeroPulse) {
        if (returnToPageZeroPulse > 0) {
            viewModel.closeAllDesktopOverlays()
            viewModel.closeSettings()
            viewModel.setSearching(false)
            viewModel.setHomeEditMode(false)
            viewModel.clearLockedLetter()
            if (pagerState.currentPage != 0) {
                pagerState.animateScrollToPage(0)
            }
        }
    }

    // Exit desktop edit mode or letter lock automatically when swiping between pages
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != 0 && uiState.isHomeEditMode) {
            viewModel.setHomeEditMode(false)
        }
        if (pagerState.currentPage != 1 && uiState.lockedLetter != null) {
            viewModel.clearLockedLetter()
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

    // Handle system back button in priority order: Lock Guide -> Icon Picker -> Desktop Settings -> Search -> Locked Letter -> Edit Mode -> Drawer Scroll -> Home Page
    BackHandler(
        enabled = uiState.isScreenLockGuideVisible ||
            uiState.isIconPickerVisible ||
            uiState.isDesktopSettingsVisible ||
            uiState.isSearching ||
            uiState.lockedLetter != null ||
            uiState.isHomeEditMode ||
            pagerState.currentPage != 0 ||
            listState.firstVisibleItemIndex > 0
    ) {
        when {
            uiState.isScreenLockGuideVisible -> viewModel.closeScreenLockGuide()
            uiState.isIconPickerVisible -> viewModel.closeIconPicker()
            uiState.isDesktopSettingsVisible -> viewModel.closeDesktopSettings()
            uiState.isSearching -> viewModel.setSearching(false)
            uiState.lockedLetter != null -> viewModel.clearLockedLetter()
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

    val isOverlayBlocking = uiState.isScreenLockGuideVisible ||
        uiState.isSearching ||
        uiState.isDesktopMenuVisible ||
        uiState.isDesktopSettingsVisible ||
        uiState.isIconPickerVisible ||
        uiState.isSettingsSheetVisible ||
        uiState.appPickerMode != null ||
        uiState.selectedAppForMenu != null ||
        uiState.isWeatherConfigDialogVisible

    val onDoubleTapLock: () -> Unit = {
        when (ScreenLockController.tryLockScreen(context)) {
            ScreenLockResult.LOCKED -> Unit
            ScreenLockResult.PERMISSION_REQUIRED,
            ScreenLockResult.UNSUPPORTED -> viewModel.openScreenLockGuide()
        }
    }

    val letterCounts = remember(uiState.appsByLetter) {
        uiState.appsByLetter.mapValues { it.value.size }
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = returnScale.value
                            scaleY = returnScale.value
                            alpha = returnAlpha.value
                            translationY = returnOffsetY.value
                        }
                ) {
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = !uiState.isSearching &&
                            !uiState.isDesktopSettingsVisible &&
                            !uiState.isIconPickerVisible &&
                            !isHomeIconDragging &&
                            !isAlphabetScrubbing,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (page) {
                            0 -> {
                                IosHomePage(
                                    favorites = uiState.favorites,
                                    desktopApps = uiState.desktopApps,
                                    isEditMode = uiState.isHomeEditMode,
                                    preferences = uiState.preferences,
                                    deviceStatus = deviceStatus,
                                    isWeatherRefreshing = uiState.isWeatherRefreshing,
                                    onWeatherClick = onWeatherTap,
                                    onWeatherLongClick = { viewModel.openWeatherConfigDialog() },
                                    onLaunchApp = { app, bounds -> launchWithTransition(app, bounds) },
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
                                    onSearchClick = { viewModel.setSearching(true) },
                                    isOverlayBlocking = isOverlayBlocking,
                                    onDoubleTapLock = onDoubleTapLock
                                )
                            }

                            else -> {
                                AllAppsDrawerPage(
                                    listEntries = uiState.listEntries,
                                    allAppsCount = uiState.allApps.size,
                                    availableLetters = uiState.availableLetters,
                                    letterCounts = letterCounts,
                                    lockedLetter = uiState.lockedLetter,
                                    lockedApps = uiState.lockedLetterApps,
                                    listState = listState,
                                    isRightHanded = uiState.preferences.isRightHanded,
                                    hapticFeedbackEnabled = uiState.preferences.hapticFeedbackEnabled,
                                    doubleTapToLockEnabled = uiState.preferences.doubleTapToLockEnabled,
                                    isOverlayBlocking = isOverlayBlocking,
                                    onLaunchApp = { app, bounds -> launchWithTransition(app, bounds) },
                                    onOpenAppMenu = { viewModel.openAppMenu(it) },
                                    onLetterSelected = { letter -> viewModel.lockLetter(letter) },
                                    onLetterLocked = { letter ->
                                        if (letter == null) {
                                            viewModel.clearLockedLetter()
                                        } else {
                                            viewModel.lockLetter(letter)
                                        }
                                    },
                                    onClearLockedLetter = { viewModel.clearLockedLetter() },
                                    onSearchClick = { viewModel.setSearching(true) },
                                    onSettingsClick = { viewModel.openSettings() },
                                    onScrubStateChanged = { isAlphabetScrubbing = it },
                                    onDoubleTapLock = onDoubleTapLock
                                )
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
                onAppClick = { app, bounds ->
                    viewModel.setSearching(false)
                    launchWithTransition(app, bounds)
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
                    onShowDeviceStatusChange = { viewModel.setShowDeviceStatusInGlance(it) },
                    onShowWeatherChange = { viewModel.setShowWeatherInGlance(it) },
                    onDoubleTapToLockChange = { viewModel.setDoubleTapToLockEnabled(it) },
                    onOpenScreenLockGuide = { viewModel.openScreenLockGuide() },
                    onConfigureWeatherClick = {
                        viewModel.closeSettings()
                        viewModel.openWeatherConfigDialog()
                    },
                    onUnhideApp = { viewModel.unhideApp(it) },
                    onOpenDesktopSettings = { viewModel.openDesktopSettings() }
                )
            }

            // Open-Meteo Weather Location Configuration Dialog
            if (uiState.isWeatherConfigDialogVisible) {
                WeatherConfigDialog(
                    currentQuery = uiState.preferences.weatherLocationQuery,
                    cachedWeather = uiState.preferences.cachedWeather,
                    isRefreshing = uiState.isWeatherRefreshing,
                    errorMessage = uiState.weatherErrorMessage,
                    onDismiss = { viewModel.closeWeatherConfigDialog() },
                    onSaveLocation = { viewModel.updateWeatherLocation(it) },
                    onRefreshNow = { viewModel.refreshWeatherIfStale(force = true) }
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
                    onDoubleTapToLockChange = { viewModel.setDoubleTapToLockEnabled(it) },
                    onOpenScreenLockGuide = { viewModel.openScreenLockGuide() },
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

            // Guided Setup Dialog for Double-Tap Screen Lock
            if (uiState.isScreenLockGuideVisible) {
                ScreenLockGuideDialog(
                    onDismiss = { viewModel.closeScreenLockGuide() }
                )
            }
        }
    }
}
