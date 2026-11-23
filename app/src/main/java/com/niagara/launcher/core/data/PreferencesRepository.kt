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

class PreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val FAVORITE_KEYS = stringPreferencesKey("favorite_app_keys")
        val HIDDEN_KEYS = stringSetPreferencesKey("hidden_app_keys")
        val IS_RIGHT_HANDED = booleanPreferencesKey("is_right_handed")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback_enabled")
        val SHOW_BATTERY = booleanPreferencesKey("show_battery_in_glance")
    }

    val launcherPreferencesFlow: Flow<LauncherPreferences> = context.dataStore.data.map { preferences ->
        val favoritesRaw = preferences[PreferencesKeys.FAVORITE_KEYS] ?: ""
        val favoriteList = if (favoritesRaw.isBlank()) {
            emptyList()
        } else {
            favoritesRaw.split(";").filter { it.isNotBlank() }
        }
        val hiddenSet = preferences[PreferencesKeys.HIDDEN_KEYS] ?: emptySet()
        val isRightHanded = preferences[PreferencesKeys.IS_RIGHT_HANDED] ?: true
        val hapticEnabled = preferences[PreferencesKeys.HAPTIC_FEEDBACK] ?: true
        val showBattery = preferences[PreferencesKeys.SHOW_BATTERY] ?: true

        LauncherPreferences(
            favoriteKeys = favoriteList,
            hiddenKeys = hiddenSet,
            isRightHanded = isRightHanded,
            hapticFeedbackEnabled = hapticEnabled,
            showBatteryInGlance = showBattery
        )
    }

    suspend fun toggleFavorite(appKey: String) {
        context.dataStore.edit { preferences ->
            val favoritesRaw = preferences[PreferencesKeys.FAVORITE_KEYS] ?: ""
            val list = if (favoritesRaw.isBlank()) {
                mutableListOf()
            } else {
                favoritesRaw.split(";").filter { it.isNotBlank() }.toMutableList()
            }
            if (list.contains(appKey)) {
                list.remove(appKey)
            } else {
                list.add(appKey)
            }
            preferences[PreferencesKeys.FAVORITE_KEYS] = list.joinToString(";")
        }
    }

    suspend fun setFavorites(favoriteKeys: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FAVORITE_KEYS] = favoriteKeys.joinToString(";")
        }
    }

    suspend fun moveFavorite(appKey: String, delta: Int) {
        if (delta == 0) return
        context.dataStore.edit { preferences ->
            val favoritesRaw = preferences[PreferencesKeys.FAVORITE_KEYS] ?: ""
            val list = if (favoritesRaw.isBlank()) {
                return@edit
            } else {
                favoritesRaw.split(";").filter { it.isNotBlank() }.toMutableList()
            }
            val index = list.indexOf(appKey)
            if (index < 0) return@edit
            val targetIndex = (index + delta).coerceIn(0, list.lastIndex)
            if (targetIndex == index) return@edit
            val item = list.removeAt(index)
            list.add(targetIndex, item)
            preferences[PreferencesKeys.FAVORITE_KEYS] = list.joinToString(";")
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
