package com.niagara.launcher.core.model

import androidx.compose.runtime.Immutable

/**
 * Represents an installed Android application accessible from the launcher.
 * Icons are loaded asynchronously via [com.niagara.launcher.core.data.AppIconCache]
 * to keep this model immutable and Compose-skippable.
 */
@Immutable
data class AppItem(
    val packageName: String,
    val activityName: String,
    val label: String,
    val firstLetter: Char = '#',
    val pinyinInitials: String = "",
    val fullPinyin: String = "",
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false
) {
    /**
     * Unique identifier for the application component.
     */
    val key: String get() = "$packageName/$activityName"

    /**
     * Matches user search query against app title, pinyin initials, or full pinyin.
     */
    fun matchesQuery(query: String): Boolean {
        if (query.isBlank()) return true
        val cleanQuery = query.trim().lowercase()
        return label.lowercase().contains(cleanQuery) ||
                pinyinInitials.lowercase().contains(cleanQuery) ||
                fullPinyin.lowercase().contains(cleanQuery)
    }
}
