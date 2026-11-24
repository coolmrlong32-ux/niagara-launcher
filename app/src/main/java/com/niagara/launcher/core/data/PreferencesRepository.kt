package com.niagara.launcher.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.niagara.launcher.core.model.IconStyle
import com.niagara.launcher.core.model.LauncherPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_preferences")

/**
 * Pure Kotlin codec and ordered list manipulation helpers for semicolon-delimited app keys.
 */
object PreferencesKeyCodec {
    private const val DELIMITER = ";"

    fun decode(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(DELIMITER)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    fun encode(keys: List<String>): String {
        return keys.asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(DELIMITER)
    }

    fun toggle(current: List<String>, key: String, maxSize: Int? = null): Pair<List<String>, Boolean> {
        if (current.contains(key)) {
            return current.filterNot { it == key } to true
        }
        if (maxSize != null && current.size >= maxSize) {
            return current to false
        }
        return (current + key) to true
    }

    fun add(current: List<String>, key: String, maxSize: Int? = null): Pair<List<String>, Boolean> {
        if (current.contains(key)) return current to true
        if (maxSize != null && current.size >= maxSize) {
            return current to false
        }
        return (current + key) to true
    }

    fun remove(current: List<String>, key: String): List<String> {
        return current.filterNot { it == key }
    }

    fun move(current: List<String>, key: String, delta: Int): List<String> {
        if (delta == 0) return current
        val index = current.indexOf(key)
        if (index < 0) return current
        val targetIndex = (index + delta).coerceIn(0, current.lastIndex)
        if (targetIndex == index) return current
        return reorder(current, index, targetIndex)
    }

    fun reorder(current: List<String>, fromIndex: Int, toIndex: Int): List<String> {
        if (current.size <= 1) return current
        if (fromIndex !in current.indices || toIndex !in current.indices || fromIndex == toIndex) {
            return current
        }
        val mutable = current.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        return mutable
    }

    /**
     * Applies a reordered list of visible app keys onto [storedKeys] while preserving
     * any hidden/non-visible keys stored in preferences.
     */
    fun applyVisibleOrder(storedKeys: List<String>, reorderedVisibleKeys: List<String>): List<String> {
        val normalizedVisible = reorderedVisibleKeys.asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
        if (normalizedVisible.isEmpty()) return storedKeys

        val visibleSet = normalizedVisible.toSet()
        val result = ArrayList<String>(maxOf(storedKeys.size, normalizedVisible.size))
        var visibleCursor = 0

        for (stored in storedKeys) {
            if (stored in visibleSet) {
                if (visibleCursor < normalizedVisible.size) {
                    result.add(normalizedVisible[visibleCursor++])
                }
            } else {
                result.add(stored)
            }
        }
        while (visibleCursor < normalizedVisible.size) {
            val next = normalizedVisible[visibleCursor++]
            if (!result.contains(next)) {
                result.add(next)
            }
        }
        return result.distinct()
    }
}

class PreferencesRepository internal constructor(
    private val dataStore: DataStore<Preferences>
) {

    constructor(context: Context) : this(context.dataStore)

    private object PreferencesKeys {
        val FAVORITE_KEYS = stringPreferencesKey("favorite_app_keys")
        val DESKTOP_APP_KEYS = stringPreferencesKey("desktop_app_keys")
        val HIDDEN_KEYS = stringSetPreferencesKey("hidden_app_keys")
        val IS_RIGHT_HANDED = booleanPreferencesKey("is_right_handed")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback_enabled")
        val SHOW_BATTERY = booleanPreferencesKey("show_battery_in_glance")
        val DESKTOP_ICON_SIZE_DP = floatPreferencesKey("desktop_icon_size_dp")
        val DESKTOP_GRID_COLUMNS = intPreferencesKey("desktop_grid_columns")
        val SHOW_APP_LABELS = booleanPreferencesKey("show_app_labels")
        val SHOW_FAVORITES_BAR = booleanPreferencesKey("show_favorites_bar")
        val ICON_STYLE = stringPreferencesKey("icon_style")
        val ICON_PACK_PACKAGE = stringPreferencesKey("icon_pack_package")
    }

    val launcherPreferencesFlow: Flow<LauncherPreferences> = dataStore.data.map { preferences ->
        val favoriteList = PreferencesKeyCodec.decode(preferences[PreferencesKeys.FAVORITE_KEYS])
        val desktopList = PreferencesKeyCodec.decode(preferences[PreferencesKeys.DESKTOP_APP_KEYS])
            .take(LauncherPreferences.MAX_DESKTOP_APPS_SUPPORTED)
        val hiddenSet = preferences[PreferencesKeys.HIDDEN_KEYS] ?: emptySet()
        val isRightHanded = preferences[PreferencesKeys.IS_RIGHT_HANDED] ?: true
        val hapticEnabled = preferences[PreferencesKeys.HAPTIC_FEEDBACK] ?: true
        val showBattery = preferences[PreferencesKeys.SHOW_BATTERY] ?: true
        val desktopIconSizeDp = LauncherPreferences.clampIconSizeDp(
            preferences[PreferencesKeys.DESKTOP_ICON_SIZE_DP] ?: LauncherPreferences.DEFAULT_ICON_SIZE_DP
        )
        val desktopGridColumns = LauncherPreferences.normalizeGridColumns(
            preferences[PreferencesKeys.DESKTOP_GRID_COLUMNS] ?: LauncherPreferences.DESKTOP_GRID_COLUMNS
        )
        val showAppLabels = preferences[PreferencesKeys.SHOW_APP_LABELS] ?: true
        val showFavoritesBar = preferences[PreferencesKeys.SHOW_FAVORITES_BAR] ?: true
        val iconStyle = IconStyle.fromStorageKey(preferences[PreferencesKeys.ICON_STYLE])
        val iconPackPackage = preferences[PreferencesKeys.ICON_PACK_PACKAGE]
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        LauncherPreferences(
            favoriteKeys = favoriteList,
            desktopAppKeys = desktopList,
            hiddenKeys = hiddenSet,
            isRightHanded = isRightHanded,
            hapticFeedbackEnabled = hapticEnabled,
            showBatteryInGlance = showBattery,
            desktopIconSizeDp = desktopIconSizeDp,
            desktopGridColumns = desktopGridColumns,
            showAppLabels = showAppLabels,
            showFavoritesBar = showFavoritesBar,
            iconStyle = iconStyle,
            iconPackPackage = iconPackPackage
        )
    }

    suspend fun toggleFavorite(appKey: String) {
        dataStore.edit { preferences ->
            val current = PreferencesKeyCodec.decode(preferences[PreferencesKeys.FAVORITE_KEYS])
            val (updated, _) = PreferencesKeyCodec.toggle(current, appKey)
            preferences[PreferencesKeys.FAVORITE_KEYS] = PreferencesKeyCodec.encode(updated)
        }
    }

    suspend fun addFavorite(appKey: String) {
        dataStore.edit { preferences ->
            val current = PreferencesKeyCodec.decode(preferences[PreferencesKeys.FAVORITE_KEYS])
            val (updated, _) = PreferencesKeyCodec.add(current, appKey)
            preferences[PreferencesKeys.FAVORITE_KEYS] = PreferencesKeyCodec.encode(updated)
        }
    }

    suspend fun removeFavorite(appKey: String) {
        dataStore.edit { preferences ->
            val current = PreferencesKeyCodec.decode(preferences[PreferencesKeys.FAVORITE_KEYS])
            val updated = PreferencesKeyCodec.remove(current, appKey)
            preferences[PreferencesKeys.FAVORITE_KEYS] = PreferencesKeyCodec.encode(updated)
        }
    }

    suspend fun setFavorites(favoriteKeys: List<String>) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FAVORITE_KEYS] = PreferencesKeyCodec.encode(favoriteKeys)
        }
    }

    suspend fun moveFavorite(appKey: String, delta: Int) {
        if (delta == 0) return
        dataStore.edit { preferences ->
            val current = PreferencesKeyCodec.decode(preferences[PreferencesKeys.FAVORITE_KEYS])
            val updated = PreferencesKeyCodec.move(current, appKey, delta)
            if (updated != current) {
                preferences[PreferencesKeys.FAVORITE_KEYS] = PreferencesKeyCodec.encode(updated)
            }
        }
    }

    suspend fun reorderFavorites(orderedVisibleKeys: List<String>) {
        if (orderedVisibleKeys.size <= 1) return
        dataStore.edit { preferences ->
            val current = PreferencesKeyCodec.decode(preferences[PreferencesKeys.FAVORITE_KEYS])
            val updated = PreferencesKeyCodec.applyVisibleOrder(current, orderedVisibleKeys)
            if (updated != current) {
                preferences[PreferencesKeys.FAVORITE_KEYS] = PreferencesKeyCodec.encode(updated)
            }
        }
    }

    suspend fun toggleDesktopApp(appKey: String): Boolean {
        var succeeded = false
        dataStore.edit { preferences ->
            val columns = LauncherPreferences.normalizeGridColumns(
                preferences[PreferencesKeys.DESKTOP_GRID_COLUMNS] ?: LauncherPreferences.DESKTOP_GRID_COLUMNS
            )
            val maxCapacity = columns * LauncherPreferences.DESKTOP_GRID_ROWS
            val current = PreferencesKeyCodec.decode(preferences[PreferencesKeys.DESKTOP_APP_KEYS])
                .take(LauncherPreferences.MAX_DESKTOP_APPS_SUPPORTED)
            val (updated, applied) = PreferencesKeyCodec.toggle(
                current = current,
                key = appKey,
                maxSize = maxCapacity
            )
            succeeded = applied
            if (applied) {
                preferences[PreferencesKeys.DESKTOP_APP_KEYS] = PreferencesKeyCodec.encode(updated)
            }
        }
        return succeeded
    }

    suspend fun addDesktopApp(appKey: String): Boolean {
        var succeeded = false
        dataStore.edit { preferences ->
            val columns = LauncherPreferences.normalizeGridColumns(
                preferences[PreferencesKeys.DESKTOP_GRID_COLUMNS] ?: LauncherPreferences.DESKTOP_GRID_COLUMNS
            )
            val maxCapacity = columns * LauncherPreferences.DESKTOP_GRID_ROWS
            val current = PreferencesKeyCodec.decode(preferences[PreferencesKeys.DESKTOP_APP_KEYS])
                .take(LauncherPreferences.MAX_DESKTOP_APPS_SUPPORTED)
            val (updated, applied) = PreferencesKeyCodec.add(
                current = current,
                key = appKey,
                maxSize = maxCapacity
            )
            succeeded = applied
            if (applied) {
                preferences[PreferencesKeys.DESKTOP_APP_KEYS] = PreferencesKeyCodec.encode(updated)
            }
        }
        return succeeded
    }

    suspend fun removeDesktopApp(appKey: String) {
        dataStore.edit { preferences ->
            val current = PreferencesKeyCodec.decode(preferences[PreferencesKeys.DESKTOP_APP_KEYS])
            val updated = PreferencesKeyCodec.remove(current, appKey)
            preferences[PreferencesKeys.DESKTOP_APP_KEYS] = PreferencesKeyCodec.encode(updated)
        }
    }

    suspend fun setDesktopApps(keys: List<String>) {
        dataStore.edit { preferences ->
            val columns = LauncherPreferences.normalizeGridColumns(
                preferences[PreferencesKeys.DESKTOP_GRID_COLUMNS] ?: LauncherPreferences.DESKTOP_GRID_COLUMNS
            )
            val maxCapacity = columns * LauncherPreferences.DESKTOP_GRID_ROWS
            val capped = keys.distinct().take(maxCapacity)
            preferences[PreferencesKeys.DESKTOP_APP_KEYS] = PreferencesKeyCodec.encode(capped)
        }
    }

    suspend fun moveDesktopApp(appKey: String, delta: Int) {
        if (delta == 0) return
        dataStore.edit { preferences ->
            val current = PreferencesKeyCodec.decode(preferences[PreferencesKeys.DESKTOP_APP_KEYS])
            val updated = PreferencesKeyCodec.move(current, appKey, delta)
            if (updated != current) {
                preferences[PreferencesKeys.DESKTOP_APP_KEYS] = PreferencesKeyCodec.encode(updated)
            }
        }
    }

    suspend fun reorderDesktopApps(orderedVisibleKeys: List<String>) {
        if (orderedVisibleKeys.size <= 1) return
        dataStore.edit { preferences ->
            val current = PreferencesKeyCodec.decode(preferences[PreferencesKeys.DESKTOP_APP_KEYS])
                .take(LauncherPreferences.MAX_DESKTOP_APPS_SUPPORTED)
            val updated = PreferencesKeyCodec.applyVisibleOrder(current, orderedVisibleKeys)
                .take(LauncherPreferences.MAX_DESKTOP_APPS_SUPPORTED)
            if (updated != current) {
                preferences[PreferencesKeys.DESKTOP_APP_KEYS] = PreferencesKeyCodec.encode(updated)
            }
        }
    }

    suspend fun toggleHidden(appKey: String) {
        dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.HIDDEN_KEYS] ?: emptySet()
            val newSet = if (current.contains(appKey)) {
                current - appKey
            } else {
                current + appKey
            }
            preferences[PreferencesKeys.HIDDEN_KEYS] = newSet
        }
    }

    suspend fun setHidden(appKey: String, hidden: Boolean) {
        dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.HIDDEN_KEYS] ?: emptySet()
            preferences[PreferencesKeys.HIDDEN_KEYS] = if (hidden) {
                current + appKey
            } else {
                current - appKey
            }
        }
    }

    suspend fun setRightHanded(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_RIGHT_HANDED] = enabled
        }
    }

    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAPTIC_FEEDBACK] = enabled
        }
    }

    suspend fun setShowBatteryInGlance(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_BATTERY] = enabled
        }
    }

    suspend fun setDesktopIconSizeDp(sizeDp: Float) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DESKTOP_ICON_SIZE_DP] = LauncherPreferences.clampIconSizeDp(sizeDp)
        }
    }

    suspend fun setDesktopGridColumns(columns: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DESKTOP_GRID_COLUMNS] = LauncherPreferences.normalizeGridColumns(columns)
        }
    }

    suspend fun setShowAppLabels(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_APP_LABELS] = enabled
        }
    }

    suspend fun setShowFavoritesBar(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_FAVORITES_BAR] = enabled
        }
    }

    suspend fun setIconStyle(style: IconStyle) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ICON_STYLE] = style.storageKey
        }
    }

    suspend fun setIconPackPackage(packageName: String?) {
        dataStore.edit { preferences ->
            val normalized = packageName?.trim()?.takeIf { it.isNotEmpty() }
            if (normalized == null) {
                preferences.remove(PreferencesKeys.ICON_PACK_PACKAGE)
            } else {
                preferences[PreferencesKeys.ICON_PACK_PACKAGE] = normalized
            }
        }
    }
}
