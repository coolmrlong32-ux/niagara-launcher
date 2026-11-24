package com.niagara.launcher.core.model

import androidx.compose.runtime.Immutable
import com.niagara.launcher.core.pinyin.PinyinIndex
import com.niagara.launcher.core.search.AppSearchEngine

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
    val pinyinIndex: PinyinIndex = PinyinIndex(),
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val isOnDesktop: Boolean = false
) {
    /**
     * Unique identifier for the application component.
     */
    val key: String get() = "$packageName/$activityName"

    /**
     * Matches user search query against app title, pinyin initials, full pinyin, or package name.
     */
    fun matchesQuery(query: String): Boolean {
        return AppSearchEngine.scoreMatch(this, query) > 0
    }
}
