package com.niagara.launcher.ui.home

import androidx.activity.compose.BackHandler
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
import com.niagara.launcher.ui.home.components.AlphabetWaveBar
import com.niagara.launcher.ui.home.components.AppActionBottomSheet
import com.niagara.launcher.ui.home.components.AppListItem
import com.niagara.launcher.ui.home.components.GlanceWidget
import com.niagara.launcher.ui.home.components.LauncherSettingsBottomSheet
import com.niagara.launcher.ui.search.SearchOverlay
import kotlinx.coroutines.launch

/**
 * Main Home screen combining Glance widget, favorites, A-Z drawer, and Alphabet Wave bar.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Observe transient UI feedback events (e.g. Undo hide app)
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

    // Handle system back button
    BackHandler(enabled = uiState.isSearching || listState.firstVisibleItemIndex > 0) {
        if (uiState.isSearching) {
            viewModel.setSearching(false)
        } else {
            coroutineScope.launch {
                listState.animateScrollToItem(0)
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
                                Spacer(modifier = Modifier.height(64.dp))
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

        // Undo / status Snackbar host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
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
                onToggleHidden = { viewModel.toggleHidden(it) },
                onOpenDetails = { viewModel.openAppDetails(it) },
                onUninstall = { viewModel.uninstallApp(it) }
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
                onUnhideApp = { viewModel.unhideApp(it) }
            )
        }
    }
}
