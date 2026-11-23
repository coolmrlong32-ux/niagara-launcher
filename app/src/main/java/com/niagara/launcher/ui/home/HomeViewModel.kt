package com.niagara.launcher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.niagara.launcher.core.data.AppRepository
import com.niagara.launcher.core.data.PreferencesRepository
import com.niagara.launcher.core.model.AppItem
import com.niagara.launcher.core.model.LauncherPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val appRepository: AppRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        appRepository.startObservingPackages()
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                appRepository.installedAppsFlow,
                appRepository.isLoaded,
                preferencesRepository.launcherPreferencesFlow,
                _searchQuery
            ) { installedApps, isLoaded, prefs, query ->
                val processed = processApps(installedApps, prefs)
                val searchResults = if (query.isBlank()) {
                    emptyList()
                } else {
                    processed.visibleApps.filter { it.matchesQuery(query) }
                }
                ObservedState(
                    isLoading = !isLoaded,
                    processed = processed,
                    searchQuery = query,
                    searchResults = searchResults
                )
            }
                .flowOn(Dispatchers.Default)
                .collect { observed ->
                    _uiState.update { current ->
                        current.copy(
                            isLoading = observed.isLoading,
                            favorites = observed.processed.favorites,
                            allApps = observed.processed.visibleApps,
                            appsByLetter = observed.processed.grouped,
                            letterIndexMap = observed.processed.indexMap,
                            searchQuery = observed.searchQuery,
                            searchResults = observed.searchResults,
                            preferences = observed.processed.prefs
                        )
                    }
                }
        }
    }

    private fun processApps(
        allApps: List<AppItem>,
        prefs: LauncherPreferences
    ): ProcessedAppsResult {
        val appMap = allApps.associateBy { it.key }
        val favoriteKeySet = prefs.favoriteKeys.toSet()

        // Process favorites
        val favorites = prefs.favoriteKeys.mapNotNull { key ->
            appMap[key]?.copy(isFavorite = true)
        }

        // Visible apps excluding hidden ones
        val visibleApps = allApps
            .filterNot { prefs.hiddenKeys.contains(it.key) }
            .map { it.copy(isFavorite = favoriteKeySet.contains(it.key)) }

        val grouped = visibleApps.groupBy { it.firstLetter }

        // Calculate index offsets for LazyColumn:
        // Index 0 is Glance Widget
        // Index 1..favorites.size are favorite items
        // Followed by letter headers and app items
        val indexMap = mutableMapOf<Char, Int>()
        var currentIndex = 1 + favorites.size // After Glance + Favorites

        val letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ#".toCharArray()
        for (letter in letters) {
            val list = grouped[letter]
            if (!list.isNullOrEmpty()) {
                indexMap[letter] = currentIndex
                currentIndex += 1 + list.size // Header + items
            }
        }

        return ProcessedAppsResult(favorites, visibleApps, grouped, indexMap, prefs)
    }

    private data class ProcessedAppsResult(
        val favorites: List<AppItem>,
        val visibleApps: List<AppItem>,
        val grouped: Map<Char, List<AppItem>>,
        val indexMap: Map<Char, Int>,
        val prefs: LauncherPreferences
    )

    private data class ObservedState(
        val isLoading: Boolean,
        val processed: ProcessedAppsResult,
        val searchQuery: String,
        val searchResults: List<AppItem>
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setSearching(isSearching: Boolean) {
        if (!isSearching) {
            _searchQuery.value = ""
        }
        _uiState.update {
            it.copy(
                isSearching = isSearching,
                searchQuery = if (isSearching) it.searchQuery else "",
                searchResults = if (isSearching) it.searchResults else emptyList()
            )
        }
    }

    fun openAppMenu(app: AppItem) {
        _uiState.update { it.copy(selectedAppForMenu = app) }
    }

    fun closeAppMenu() {
        _uiState.update { it.copy(selectedAppForMenu = null) }
    }

    fun toggleFavorite(app: AppItem) {
        viewModelScope.launch {
            preferencesRepository.toggleFavorite(app.key)
        }
    }

    fun toggleHidden(app: AppItem) {
        viewModelScope.launch {
            preferencesRepository.toggleHidden(app.key)
        }
    }

    fun launchApp(app: AppItem) {
        appRepository.launchApp(app)
    }

    fun openAppDetails(app: AppItem) {
        appRepository.openAppDetails(app.packageName)
    }

    fun uninstallApp(app: AppItem) {
        appRepository.requestUninstall(app.packageName)
    }

    class Factory(
        private val appRepository: AppRepository,
        private val preferencesRepository: PreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(appRepository, preferencesRepository) as T
        }
    }
}
