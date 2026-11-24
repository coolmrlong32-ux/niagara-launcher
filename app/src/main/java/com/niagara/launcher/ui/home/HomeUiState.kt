package com.niagara.launcher.ui.home

import com.niagara.launcher.core.model.AppItem
import com.niagara.launcher.core.model.LauncherPreferences

enum class AppPickerMode {
    FAVORITES,
    DESKTOP
}

/**
 * State representing the Home screen and app drawer.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val listEntries: List<HomeListEntry> = emptyList(),
    val favorites: List<AppItem> = emptyList(),
    val desktopApps: List<AppItem> = emptyList(),
    val allApps: List<AppItem> = emptyList(),
    val hiddenApps: List<AppItem> = emptyList(),
    val appsByLetter: Map<Char, List<AppItem>> = emptyMap(),
    val letterIndexMap: Map<Char, Int> = emptyMap(),
    val availableLetters: Set<Char> = emptySet(),
    val searchQuery: String = "",
    val searchResults: List<AppItem> = emptyList(),
    val hiddenSearchResults: List<AppItem> = emptyList(),
    val isSearching: Boolean = false,
    val isHomeEditMode: Boolean = false,
    val appPickerMode: AppPickerMode? = null,
    val isSettingsSheetVisible: Boolean = false,
    val selectedAppForMenu: AppItem? = null,
    val preferences: LauncherPreferences = LauncherPreferences()
)
