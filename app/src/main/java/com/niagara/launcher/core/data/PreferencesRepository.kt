package com.niagara.launcher.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
        val mutable = current.toMutableList()
        val item = mutable.removeAt(index)
        mutable.add(targetIndex, item)
        return mutable
    }
}

class PreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val FAVORITE_KEYS = stringPreferencesKey("favorite_app_keys")
        val DESKTOP_APP_KEYS = stringPreferencesKey("desktop_app_keys")
        val HIDDEN_KEYS = stringSetPreferencesKey("hidden_app_keys")
        val IS_RIGHT_HANDED = booleanPreferencesKey("is_right_handed")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback_enabled")
        val SHOW_BATTERY = booleanPreferencesKey("show_battery_in_glance")
    }

    val launcherPreferencesFlow: Flow<LauncherPreferences> = context.dataStore.data.map { preferences ->
        val favoriteList = PreferencesKeyCodec.decode(preferences[PreferencesKeys.FAVORITE_KEYS])
        val desktopList = PreferencesKeyCodec.decode(preferences[PreferencesKeys.DESKTOP_APP_KEYS])
            .take(LauncherPreferences.MAX_DESKTOP_APPS)
        val hiddenSet = preferences[PreferencesKeys.HIDDEN_KEYS] ?: emptySet()
        val isRightHanded = preferences[PreferencesKeys.IS_RIGHT_HANDED] ?: true
        val hapticEnabled = preferences[PreferencesKeys.HAPTIC_FEEDBACK] ?: true
        val showBattery = preferences[PreferencesKeys.SHOW_BATTERY] ?: true

        LauncherPreferences(
            favoriteKeys = favoriteList,
            desktopAppKeys = desktopList,
            hiddenKeys = hiddenSet,
            isRightHanded = isRightHanded,
            hapticFeedbackEnabled = hapticEnabled,
            showBatteryInGlance = showBattery
        )
    }

    suspend fun toggleFavorite(appKey: String) {
        context.dataStore.edit { preferences ->
            val current = PreferencesKeyCodec.decode(preferences[PreferencesKeys.FAVORITE_KEYS])
            val (updated, _) = PreferencesKeyCodec.toggle(current, appKey)
            preferences[PreferencesKeys.FAVORITE_KEYS] = PreferencesKeyCodec.encode(updated)
        }
    }

    suspend fun addFavorite(appKey: String) {
        context.dataStore.edit { preferences ->
            val current = PreferencesKeyCodec.decode(preferences[PreferencesKeys.FAVORITE_KEYS])
            val (updated, _) = PreferencesKeyCodec.add(current, appKey)
            preferences[PreferencesKeys.FAVORITE_KEYS] = PreferencesKeyCodec.encode(updated)
        }
    }

    suspend fun removeFavorite(appKey: String) {
        context.dataStore.edit { preferences ->
            val current = PreferencesKeyCodec.decode(preferences[PreferencesKeys.FAVORITE_KEYS])
            val updated = PreferencesKeyCodec.remove(current, appKey)
            preferences[PreferencesKeys.FAVORITE_KEYS] = PreferencesKeyCodec.encode(updated)
        }
    }

    suspend fun setFavorites(favoriteKeys: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FAVORITE_KEYS] = PreferencesKeyCodec.encode(favoriteKeys)
        }
    }

    suspend fun moveFavorite(appKey: String, delta: Int) {
        if (delta == 0) return
        context.dataStore.edit { preferences ->
            val current = PreferencesKeyCodec.decode(preferences[PreferencesKeys.FAVORITE_KEYS])
            val updated = PreferencesKeyCodec.move(current, appKey, delta)
            if (updated != current) {
                preferences[PreferencesKeys.FAVORITE_KEYS] = PreferencesKeyCodec.encode(updated)
            }
        }
    }

    suspend fun toggleDesktopApp(appKey: String): Boolean {
        var succeeded = false
        context.dataStore.edit { preferences ->
            val current = PreferencesKeyCodec.decode(preferences[PreferencesKeys.DESKTOP_APP_KEYS])
                .take(LauncherPreferences.MAX_DESKTOP_APPS)
            val (updated, applied) = PreferencesKeyCodec.toggle(
                current = current,
                key = appKey,
                maxSize = LauncherPreferences.MAX_DESKTOP_APPS
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
        context.dataStore.edit { preferences ->
            val current = PreferencesKeyCodec.decode(preferences[PreferencesKeys.DESKTOP_APP_KEYS])
                .take(LauncherPreferences.MAX_DESKTOP_APPS)
            val (updated, applied) = PreferencesKeyCodec.add(
                current = current,
                key = appKey,
                maxSize = LauncherPreferences.MAX_DESKTOP_APPS
            )
            succeeded = applied
            if (applied) {
                preferences[PreferencesKeys.DESKTOP_APP_KEYS] = PreferencesKeyCodec.encode(updated)
            }
        }
        return succeeded
    }

    suspend fun removeDesktopApp(appKey: String) {
        context.dataStore.edit { preferences ->
            val current = PreferencesKeyCodec.decode(preferences[PreferencesKeys.DESKTOP_APP_KEYS])
            val updated = PreferencesKeyCodec.remove(current, appKey)
            preferences[PreferencesKeys.DESKTOP_APP_KEYS] = PreferencesKeyCodec.encode(updated)
        }
    }

    suspend fun setDesktopApps(keys: List<String>) {
        context.dataStore.edit { preferences ->
            val capped = keys.distinct().take(LauncherPreferences.MAX_DESKTOP_APPS)
            preferences[PreferencesKeys.DESKTOP_APP_KEYS] = PreferencesKeyCodec.encode(capped)
        }
    }

    suspend fun moveDesktopApp(appKey: String, delta: Int) {
        if (delta == 0) return
        context.dataStore.edit { preferences ->
            val current = PreferencesKeyCodec.decode(preferences[PreferencesKeys.DESKTOP_APP_KEYS])
            val updated = PreferencesKeyCodec.move(current, appKey, delta)
            if (updated != current) {
                preferences[PreferencesKeys.DESKTOP_APP_KEYS] = PreferencesKeyCodec.encode(updated)
            }
        }
    }

    suspend fun toggleHidden(appKey: String) {
        context.dataStore.edit { preferences ->
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
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.HIDDEN_KEYS] ?: emptySet()
            preferences[PreferencesKeys.HIDDEN_KEYS] = if (hidden) {
                current + appKey
            } else {
                current - appKey
            }
        }
    }

    suspend fun setRightHanded(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_RIGHT_HANDED] = enabled
        }
    }

    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAPTIC_FEEDBACK] = enabled
        }
    }

    suspend fun setShowBatteryInGlance(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_BATTERY] = enabled
        }
    }
}
