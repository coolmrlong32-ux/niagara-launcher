package com.niagara.launcher.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagara.launcher.core.model.LauncherPreferences
import com.niagara.launcher.ui.home.components.AlphabetWaveBar
import com.niagara.launcher.ui.home.components.AppActionBottomSheet
import com.niagara.launcher.ui.home.components.AppListItem
import com.niagara.launcher.ui.home.components.AppPickerBottomSheet
import com.niagara.launcher.ui.home.components.GlanceWidget
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
    val listState = rememberLazyListState()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

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

    // Handle system back button in priority order: Search -> Edit Mode -> Drawer Scroll -> Home Page
    BackHandler(
        enabled = uiState.isSearching ||
            uiState.isHomeEditMode ||
            pagerState.currentPage != 0 ||
            listState.firstVisibleItemIndex > 0
    ) {
        when {
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0x55000000),
                        Color(0x33000000),
                        Color(0x66000000)
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
                userScrollEnabled = !uiState.isSearching,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> {
                        IosHomePage(
                            favorites = uiState.favorites,
                            desktopApps = uiState.desktopApps,
                            isEditMode = uiState.isHomeEditMode,
                            showBattery = uiState.preferences.showBatteryInGlance,
                            onLaunchApp = { viewModel.launchApp(it) },
                            onEnterEditMode = { viewModel.setHomeEditMode(true) },
                            onExitEditMode = { viewModel.setHomeEditMode(false) },
                            onRemoveFavorite = { viewModel.removeFavorite(it) },
                            onRemoveDesktopApp = { viewModel.removeDesktopApp(it) },
                            onOpenFavoritesPicker = { viewModel.openAppPicker(AppPickerMode.FAVORITES) },
                            onOpenDesktopPicker = { viewModel.openAppPicker(AppPickerMode.DESKTOP) },
                            onSearchClick = { viewModel.setSearching(true) },
                            onSettingsClick = { viewModel.openSettings() }
                        )
                    }

                    else -> {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .navigationBarsPadding()
                        ) {
                            // If left-handed mode is preferred, place alphabet bar on left
                            if (!uiState.preferences.isRightHanded) {
                                AlphabetWaveBar(
                                    isRightHanded = false,
                                    hapticFeedbackEnabled = uiState.preferences.hapticFeedbackEnabled,
                                    availableLetters = uiState.availableLetters,
                                    activeLetter = activeSectionLetter,
                                    onLetterSelected = onLetterSelected
                                )
                            }

                            // Single-source-of-truth LazyColumn driven by uiState.listEntries
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
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
                                            Text(
                                                text = "常用",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color.White.copy(alpha = 0.5f),
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.sp
                                                ),
                                                modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 4.dp)
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
                                            Spacer(modifier = Modifier.height(16.dp))
                                        }

                                        is HomeListEntry.LetterHeader -> {
                                            Text(
                                                text = entry.letter.toString(),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                ),
                                                modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp)
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
                                            Spacer(modifier = Modifier.height(84.dp))
                                        }
                                    }
                                }
                            }

                            // If right-handed mode (default), place alphabet bar on right
                            if (uiState.preferences.isRightHanded) {
                                AlphabetWaveBar(
                                    isRightHanded = true,
                                    hapticFeedbackEnabled = uiState.preferences.hapticFeedbackEnabled,
                                    availableLetters = uiState.availableLetters,
                                    activeLetter = activeSectionLetter,
                                    onLetterSelected = onLetterSelected
                                )
                            }
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
                        maxSelection = LauncherPreferences.MAX_DESKTOP_APPS,
                        onToggleApp = { viewModel.toggleDesktopApp(it) },
                        onDismissRequest = { viewModel.closeAppPicker() }
                    )
                }
            }
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
                onUnhideApp = { viewModel.unhideApp(it) }
            )
        }
    }
}
