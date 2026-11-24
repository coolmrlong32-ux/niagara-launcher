package com.niagara.launcher.ui.home

import androidx.compose.runtime.Immutable
import com.niagara.launcher.core.model.AppItem

/**
 * Single source of truth for items rendered inside the Home LazyColumn.
 * Keeping list entries and letter index calculation unified prevents index drift.
 */
@Immutable
sealed interface HomeListEntry {
    val listKey: String
    val sectionLetter: Char?

    data object Glance : HomeListEntry {
        override val listKey: String = "glance_widget"
        override val sectionLetter: Char? = null
    }

    data object FavoritesHeader : HomeListEntry {
        override val listKey: String = "favorites_header"
        override val sectionLetter: Char? = null
    }

    data class FavoriteApp(val app: AppItem) : HomeListEntry {
        override val listKey: String = "fav_${app.key}"
        override val sectionLetter: Char? = null
    }

    data object FavoritesDivider : HomeListEntry {
        override val listKey: String = "favorites_divider"
        override val sectionLetter: Char? = null
    }

    data class LetterHeader(val letter: Char) : HomeListEntry {
        override val listKey: String = "header_$letter"
        override val sectionLetter: Char = letter
    }

    data class DrawerApp(val app: AppItem) : HomeListEntry {
        override val listKey: String = app.key
        override val sectionLetter: Char = app.firstLetter
    }

    data object BottomSpacer : HomeListEntry {
        override val listKey: String = "bottom_spacer"
        override val sectionLetter: Char? = null
    }
}

data class HomeListBuildResult(
    val entries: List<HomeListEntry>,
    val letterIndexMap: Map<Char, Int>,
    val availableLetters: Set<Char>
)

object HomeListModel {
    val ALPHABET: List<Char> = ("ABCDEFGHIJKLMNOPQRSTUVWXYZ#").toList()

    /**
     * Builds the exact ordered [HomeListEntry] list along with the exact index map
     * for each letter section header.
     */
    fun build(
        favorites: List<AppItem>,
        appsByLetter: Map<Char, List<AppItem>>
    ): HomeListBuildResult {
        val entries = ArrayList<HomeListEntry>(favorites.size + appsByLetter.values.sumOf { it.size } + 32)
        val letterIndexMap = LinkedHashMap<Char, Int>()
        val availableLetters = LinkedHashSet<Char>()

        entries.add(HomeListEntry.Glance)

        if (favorites.isNotEmpty()) {
            entries.add(HomeListEntry.FavoritesHeader)
            favorites.forEach { fav ->
                entries.add(HomeListEntry.FavoriteApp(fav))
            }
            entries.add(HomeListEntry.FavoritesDivider)
        }

        for (letter in ALPHABET) {
            val appsForLetter = appsByLetter[letter]
            if (!appsForLetter.isNullOrEmpty()) {
                letterIndexMap[letter] = entries.size
                availableLetters.add(letter)
                entries.add(HomeListEntry.LetterHeader(letter))
                appsForLetter.forEach { app ->
                    entries.add(HomeListEntry.DrawerApp(app))
                }
            }
        }

        entries.add(HomeListEntry.BottomSpacer)

        return HomeListBuildResult(
            entries = entries,
            letterIndexMap = letterIndexMap,
            availableLetters = availableLetters
        )
    }

    /**
     * Resolves a target scroll index for [letter]. If [letter] has no apps,
     * falls back to the nearest populated letter so dragging across empty letters stays smooth.
     */
    fun resolveTargetLetterIndex(
        letter: Char,
        letterIndexMap: Map<Char, Int>
    ): Int? {
        if (letterIndexMap.isEmpty()) return null
        letterIndexMap[letter]?.let { return it }

        val targetPos = ALPHABET.indexOf(letter)
        if (targetPos < 0) return letterIndexMap.values.firstOrNull()

        var bestIndex: Int? = null
        var bestDistance = Int.MAX_VALUE
        for ((candidateLetter, listIndex) in letterIndexMap) {
            val candidatePos = ALPHABET.indexOf(candidateLetter)
            if (candidatePos >= 0) {
                val dist = kotlin.math.abs(candidatePos - targetPos)
                if (dist < bestDistance) {
                    bestDistance = dist
                    bestIndex = listIndex
                }
            }
        }
        return bestIndex
    }

    /**
     * Resolves [letter] to itself when populated, or the nearest populated letter in [ALPHABET].
     */
    fun resolveTargetLetter(
        letter: Char,
        availableLetters: Set<Char>
    ): Char? {
        if (availableLetters.isEmpty()) return null
        if (availableLetters.contains(letter)) return letter

        val targetPos = ALPHABET.indexOf(letter)
        if (targetPos < 0) {
            return ALPHABET.firstOrNull { availableLetters.contains(it) }
                ?: availableLetters.firstOrNull()
        }

        var bestLetter: Char? = null
        var bestDistance = Int.MAX_VALUE
        for (candidate in availableLetters) {
            val candidatePos = ALPHABET.indexOf(candidate)
            if (candidatePos >= 0) {
                val dist = kotlin.math.abs(candidatePos - targetPos)
                if (dist < bestDistance) {
                    bestDistance = dist
                    bestLetter = candidate
                }
            }
        }
        return bestLetter ?: availableLetters.firstOrNull()
    }

    /**
     * Returns the apps belonging to [letter] (or nearest populated fallback letter).
     */
    fun buildLockedApps(
        letter: Char,
        appsByLetter: Map<Char, List<AppItem>>
    ): List<AppItem> {
        val resolved = resolveTargetLetter(letter, appsByLetter.keys) ?: return emptyList()
        return appsByLetter[resolved].orEmpty()
    }
}
