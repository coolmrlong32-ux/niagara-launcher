package com.niagara.launcher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.niagara.launcher.core.data.AppRepository
import com.niagara.launcher.core.data.PreferencesRepository
import com.niagara.launcher.core.model.AppItem
import com.niagara.launcher.core.model.IconStyle
import com.niagara.launcher.core.model.LauncherPreferences
import com.niagara.launcher.core.search.AppSearchEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Niagara Home screen managing app lists, favorites, hidden apps,
 * search queries, launcher preferences, and user interactions.
 */
class HomeViewModel(
    private val appRepository: AppRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _liveIconSizeOverride = MutableStateFlow<Float?>(null)
    private var iconSizePersistJob: Job? = null

    private val _snackbarEvents = MutableSharedFlow<HomeSnackbarEvent>(extraBufferCapacity = 8)
    val snackbarEvents: SharedFlow<HomeSnackbarEvent> = _snackbarEvents.asSharedFlow()

    init {
        appRepository.startObservingPackages()
        observeData()
        observeIconPacks()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                appRepository.installedAppsFlow,
                appRepository.isLoaded,
                preferencesRepository.launcherPreferencesFlow,
                _searchQuery,
                _liveIconSizeOverride
            ) { installedApps, isLoaded, prefs, query, liveIconSize ->
                val effectivePrefs = if (liveIconSize != null) {
                    prefs.copy(desktopIconSizeDp = liveIconSize)
                } else {
                    prefs
                }
                val processed = processApps(installedApps, effectivePrefs)
                val searchResults = if (query.isBlank()) {
                    emptyList()
                } else {
                    AppSearchEngine.search(processed.visibleApps, query)
                }
                val hiddenSearchResults = if (query.isBlank()) {
                    emptyList()
                } else {
                    AppSearchEngine.search(processed.hiddenApps, query)
                }
                ObservedState(
                    isLoading = !isLoaded,
                    processed = processed,
                    searchQuery = query,
                    searchResults = searchResults,
                    hiddenSearchResults = hiddenSearchResults
                )
            }
                .flowOn(Dispatchers.Default)
                .collect { observed ->
                    _uiState.update { current ->
                        current.copy(
                            isLoading = observed.isLoading,
                            listEntries = observed.processed.listBuildResult.entries,
                            favorites = observed.processed.favorites,
                            desktopApps = observed.processed.desktopApps,
                            allApps = observed.processed.visibleApps,
                            hiddenApps = observed.processed.hiddenApps,
                            appsByLetter = observed.processed.grouped,
                            letterIndexMap = observed.processed.listBuildResult.letterIndexMap,
                            availableLetters = observed.processed.listBuildResult.availableLetters,
                            searchQuery = observed.searchQuery,
                            searchResults = observed.searchResults,
                            hiddenSearchResults = observed.hiddenSearchResults,
                            preferences = observed.processed.prefs
                        )
                    }
                }
        }
    }

    private fun observeIconPacks() {
        viewModelScope.launch {
            combine(
                appRepository.installedIconPacksFlow,
                appRepository.iconPacksLoaded,
                preferencesRepository.launcherPreferencesFlow
            ) { packs, packsLoaded, prefs ->
                Triple(packs, packsLoaded, prefs.iconPackPackage)
            }.collect { (packs, packsLoaded, selectedPack) ->
                _uiState.update { it.copy(installedIconPacks = packs) }
                if (packsLoaded && selectedPack != null && packs.none { it.packageName == selectedPack }) {
                    preferencesRepository.setIconPackPackage(null)
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
        val desktopKeySet = prefs.desktopAppKeys.toSet()

        // Favorites exclude hidden apps so hiding a favorite removes it cleanly from pinned area
        val favorites = prefs.favoriteKeys.mapNotNull { key ->
            if (prefs.hiddenKeys.contains(key)) {
                null
            } else {
                appMap[key]?.copy(
                    isFavorite = true,
                    isHidden = false,
                    isOnDesktop = desktopKeySet.contains(key)
                )
            }
        }

        // Desktop apps exclude hidden apps and respect the current grid capacity (24 or 30)
        val desktopApps = prefs.desktopAppKeys
            .asSequence()
            .filterNot { prefs.hiddenKeys.contains(it) }
            .mapNotNull { key ->
                appMap[key]?.copy(
                    isFavorite = favoriteKeySet.contains(key),
                    isHidden = false,
                    isOnDesktop = true
                )
            }
            .take(prefs.maxDesktopApps)
            .toList()

        // Visible apps excluding hidden ones
        val visibleApps = allApps
            .filterNot { prefs.hiddenKeys.contains(it.key) }
            .map {
                it.copy(
                    isFavorite = favoriteKeySet.contains(it.key),
                    isHidden = false,
                    isOnDesktop = desktopKeySet.contains(it.key)
                )
            }

        // Hidden apps sorted alphabetically for management sheet & search fallback
        val hiddenApps = allApps
            .filter { prefs.hiddenKeys.contains(it.key) }
            .map {
                it.copy(
                    isFavorite = favoriteKeySet.contains(it.key),
                    isHidden = true,
                    isOnDesktop = desktopKeySet.contains(it.key)
                )
            }

        val grouped = visibleApps.groupBy { it.firstLetter }
        val listBuildResult = HomeListModel.build(
            favorites = favorites,
            appsByLetter = grouped
        )

        return ProcessedAppsResult(
            favorites = favorites,
            desktopApps = desktopApps,
            visibleApps = visibleApps,
            hiddenApps = hiddenApps,
            grouped = grouped,
            listBuildResult = listBuildResult,
            prefs = prefs
        )
    }

    private data class ProcessedAppsResult(
        val favorites: List<AppItem>,
        val desktopApps: List<AppItem>,
        val visibleApps: List<AppItem>,
        val hiddenApps: List<AppItem>,
        val grouped: Map<Char, List<AppItem>>,
        val listBuildResult: HomeListBuildResult,
        val prefs: LauncherPreferences
    )

    private data class ObservedState(
        val isLoading: Boolean,
        val processed: ProcessedAppsResult,
        val searchQuery: String,
        val searchResults: List<AppItem>,
        val hiddenSearchResults: List<AppItem>
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
                searchQuery = if (!isSearching) "" else it.searchQuery,
                searchResults = if (!isSearching) emptyList() else it.searchResults,
                hiddenSearchResults = if (!isSearching) emptyList() else it.hiddenSearchResults
            )
        }
    }

    fun openAppMenu(app: AppItem) {
        _uiState.update { it.copy(selectedAppForMenu = app) }
    }

    fun closeAppMenu() {
        _uiState.update { it.copy(selectedAppForMenu = null) }
    }

    fun setHomeEditMode(enabled: Boolean) {
        _uiState.update { it.copy(isHomeEditMode = enabled) }
    }

    fun openDesktopMenu() {
        _uiState.update { it.copy(isDesktopMenuVisible = true) }
    }

    fun closeDesktopMenu() {
        _uiState.update { it.copy(isDesktopMenuVisible = false) }
    }

    fun openDesktopSettings() {
        _uiState.update {
            it.copy(
                isDesktopMenuVisible = false,
                isSettingsSheetVisible = false,
                isDesktopSettingsVisible = true
            )
        }
    }

    fun closeDesktopSettings() {
        _uiState.update { it.copy(isDesktopSettingsVisible = false) }
    }

    fun openIconPicker() {
        _uiState.update {
            it.copy(
                isDesktopMenuVisible = false,
                isIconPickerVisible = true
            )
        }
        if (!appRepository.iconPacksLoaded.value) {
            refreshIconPacks()
        }
    }

    fun closeIconPicker() {
        _uiState.update { it.copy(isIconPickerVisible = false) }
    }

    fun closeAllDesktopOverlays() {
        _uiState.update {
            it.copy(
                isDesktopMenuVisible = false,
                isIconPickerVisible = false,
                isDesktopSettingsVisible = false
            )
        }
    }

    fun openAppPicker(mode: AppPickerMode) {
        _uiState.update { it.copy(appPickerMode = mode) }
    }

    fun closeAppPicker() {
        _uiState.update { it.copy(appPickerMode = null) }
    }

    fun openSettings() {
        _uiState.update { it.copy(isSettingsSheetVisible = true) }
    }

    fun closeSettings() {
        _uiState.update { it.copy(isSettingsSheetVisible = false) }
    }

    fun launchApp(app: AppItem) {
        appRepository.launchApp(app)
    }

    fun toggleFavorite(app: AppItem) {
        viewModelScope.launch {
            preferencesRepository.toggleFavorite(app.key)
        }
    }

    fun addFavorite(app: AppItem) {
        viewModelScope.launch {
            preferencesRepository.addFavorite(app.key)
        }
    }

    fun removeFavorite(app: AppItem) {
        viewModelScope.launch {
            preferencesRepository.removeFavorite(app.key)
        }
    }

    fun toggleDesktopApp(app: AppItem) {
        viewModelScope.launch {
            val applied = preferencesRepository.toggleDesktopApp(app.key)
            if (!applied) {
                _snackbarEvents.tryEmit(HomeSnackbarEvent.DesktopFull(_uiState.value.preferences.maxDesktopApps))
            }
        }
    }

    fun addDesktopApp(app: AppItem) {
        viewModelScope.launch {
            val applied = preferencesRepository.addDesktopApp(app.key)
            if (!applied) {
                _snackbarEvents.tryEmit(HomeSnackbarEvent.DesktopFull(_uiState.value.preferences.maxDesktopApps))
            }
        }
    }

    fun removeDesktopApp(app: AppItem) {
        viewModelScope.launch {
            preferencesRepository.removeDesktopApp(app.key)
        }
    }

    fun moveFavoriteUp(app: AppItem) {
        viewModelScope.launch {
            preferencesRepository.moveFavorite(app.key, -1)
        }
    }

    fun moveFavoriteDown(app: AppItem) {
        viewModelScope.launch {
            preferencesRepository.moveFavorite(app.key, 1)
        }
    }

    fun toggleHidden(app: AppItem) {
        viewModelScope.launch {
            val willHide = !app.isHidden
            preferencesRepository.setHidden(app.key, willHide)
            if (willHide) {
                _snackbarEvents.tryEmit(HomeSnackbarEvent.AppHidden(app))
            }
        }
    }

    fun unhideApp(app: AppItem) {
        viewModelScope.launch {
            preferencesRepository.setHidden(app.key, false)
        }
    }

    fun setRightHanded(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setRightHanded(enabled)
        }
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setHapticFeedbackEnabled(enabled)
        }
    }

    fun setShowBatteryInGlance(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowBatteryInGlance(enabled)
        }
    }

    fun previewDesktopIconSizeDp(sizeDp: Float) {
        val clamped = LauncherPreferences.clampIconSizeDp(sizeDp)
        _liveIconSizeOverride.value = clamped
        iconSizePersistJob?.cancel()
        iconSizePersistJob = viewModelScope.launch {
            delay(80)
            preferencesRepository.setDesktopIconSizeDp(clamped)
        }
    }

    fun commitDesktopIconSizeDp(sizeDp: Float) {
        val clamped = LauncherPreferences.clampIconSizeDp(sizeDp)
        _liveIconSizeOverride.value = clamped
        iconSizePersistJob?.cancel()
        iconSizePersistJob = viewModelScope.launch {
            preferencesRepository.setDesktopIconSizeDp(clamped)
            _liveIconSizeOverride.value = null
        }
    }

    fun setDesktopGridColumns(columns: Int) {
        viewModelScope.launch {
            preferencesRepository.setDesktopGridColumns(columns)
        }
    }

    fun setShowAppLabels(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowAppLabels(enabled)
        }
    }

    fun setShowFavoritesBar(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowFavoritesBar(enabled)
        }
    }

    fun setIconStyle(style: IconStyle) {
        viewModelScope.launch {
            preferencesRepository.setIconStyle(style)
        }
    }

    fun setIconPackPackage(packageName: String?) {
        val normalized = packageName?.trim()?.takeIf { it.isNotEmpty() }
        viewModelScope.launch {
            if (normalized != null) {
                appRepository.iconPackManager.preloadPack(normalized)
            }
            preferencesRepository.setIconPackPackage(normalized)
        }
    }

    fun refreshIconPacks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingIconPacks = true) }
            try {
                appRepository.refreshIconPacks()
            } finally {
                _uiState.update { it.copy(isRefreshingIconPacks = false) }
            }
        }
    }

    fun openAppDetails(app: AppItem) {
        appRepository.openAppDetails(app)
    }

    fun uninstallApp(app: AppItem) {
        appRepository.uninstallApp(app)
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

sealed interface HomeSnackbarEvent {
    data class AppHidden(val app: AppItem) : HomeSnackbarEvent
    data class DesktopFull(val maxCount: Int) : HomeSnackbarEvent
}
