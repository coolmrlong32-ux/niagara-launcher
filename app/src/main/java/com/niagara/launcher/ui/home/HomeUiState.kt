package com.niagara.launcher.ui.home

import com.niagara.launcher.core.model.AppItem
import com.niagara.launcher.core.model.LauncherPreferences

/**
 * State representing the Home screen and app drawer.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val favorites: List<AppItem> = emptyList(),
    val allApps: List<AppItem> = emptyList(),
    val appsByLetter: Map<Char, List<AppItem>> = emptyMap(),
    val letterIndexMap: Map<Char, Int> = emptyMap(),
    val searchQuery: String = "",
    val searchResults: List<AppItem> = emptyList(),
    val isSearching: Boolean = false,
    val selectedAppForMenu: AppItem? = null,
    val preferences: LauncherPreferences = LauncherPreferences()
)
