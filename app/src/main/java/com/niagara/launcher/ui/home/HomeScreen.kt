package com.niagara.launcher.ui.home

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    Box(
        modifier = modifier
            .fillMaxSize()
            // Subtle translucent gradient to enhance readability against custom wallpaper
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
                        onLetterSelected = { letter ->
                            val targetIndex = uiState.letterIndexMap[letter]
                            if (targetIndex != null) {
                                coroutineScope.launch {
                                    listState.scrollToItem(targetIndex)
                                }
                            }
                        }
                    )
                }

                // Main vertical scroll list
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                ) {
                    // Item 0: Niagara Glance (Clock, Date, Battery, Search)
                    item(key = "glance_widget") {
                        GlanceWidget(
                            showBattery = uiState.preferences.showBatteryInGlance,
                            onSearchClick = { viewModel.setSearching(true) }
                        )
                    }

                    // Pinned Favorites section
                    if (uiState.favorites.isNotEmpty()) {
                        item(key = "favorites_header") {
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

                        items(
                            items = uiState.favorites,
                            key = { "fav_${it.key}" }
                        ) { app ->
                            AppListItem(
                                app = app,
                                onClick = { viewModel.launchApp(app) },
                                onLongClick = { viewModel.openAppMenu(app) }
                            )
                        }

                        item(key = "favorites_divider") {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // A-Z App List grouped by first letter
                    val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ#".toCharArray()
                    alphabet.forEach { letter ->
                        val appsForLetter = uiState.appsByLetter[letter]
                        if (!appsForLetter.isNullOrEmpty()) {
                            // Letter section header
                            item(key = "header_$letter") {
                                Text(
                                    text = letter.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp)
                                )
                            }

                            // Apps under this letter
                            items(
                                items = appsForLetter,
                                key = { it.key }
                            ) { app ->
                                AppListItem(
                                    app = app,
                                    onClick = { viewModel.launchApp(app) },
                                    onLongClick = { viewModel.openAppMenu(app) }
                                )
                            }
                        }
                    }

                    // Bottom padding for navigation bar spacing
                    item(key = "bottom_spacer") {
                        Spacer(modifier = Modifier.height(64.dp))
                    }
                }

                // If right-handed mode (default), place alphabet bar on right
                if (uiState.preferences.isRightHanded) {
                    AlphabetWaveBar(
                        isRightHanded = true,
                        hapticFeedbackEnabled = uiState.preferences.hapticFeedbackEnabled,
                        onLetterSelected = { letter ->
                            val targetIndex = uiState.letterIndexMap[letter]
                            if (targetIndex != null) {
                                coroutineScope.launch {
                                    listState.scrollToItem(targetIndex)
                                }
                            }
                        }
                    )
                }
            }
        }

        // Search Overlay
        SearchOverlay(
            visible = uiState.isSearching,
            query = uiState.searchQuery,
            results = uiState.searchResults,
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
            AppActionBottomSheet(
                app = app,
                onDismissRequest = { viewModel.closeAppMenu() },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onToggleHidden = { viewModel.toggleHidden(it) },
                onOpenDetails = { viewModel.openAppDetails(it) },
                onUninstall = { viewModel.uninstallApp(it) }
            )
        }
    }
}
