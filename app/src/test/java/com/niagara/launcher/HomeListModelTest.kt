package com.niagara.launcher

import com.niagara.launcher.core.model.AppItem
import com.niagara.launcher.ui.home.HomeListEntry
import com.niagara.launcher.ui.home.HomeListModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeListModelTest {

    private fun app(pkg: String, label: String, letter: Char) = AppItem(
        packageName = pkg,
        activityName = ".MainActivity",
        label = label,
        firstLetter = letter
    )

    @Test
    fun testLetterIndicesWithoutFavorites() {
        val a1 = app("com.a1", "Alpha", 'A')
        val m1 = app("com.m1", "Maps", 'M')
        val grouped = mapOf('A' to listOf(a1), 'M' to listOf(m1))

        val result = HomeListModel.build(favorites = emptyList(), appsByLetter = grouped)

        // Entry 0: Glance, Entry 1: Header A, Entry 2: Alpha, Entry 3: Header M, Entry 4: Maps, Entry 5: BottomSpacer
        assertEquals(1, result.letterIndexMap['A'])
        assertEquals(3, result.letterIndexMap['M'])
        assertEquals(HomeListEntry.LetterHeader('A'), result.entries[1])
        assertEquals(HomeListEntry.LetterHeader('M'), result.entries[3])
        assertEquals(setOf('A', 'M'), result.availableLetters)
    }

    @Test
    fun testLetterIndicesWithFavoritesIncludeHeaderAndDivider() {
        val fav1 = app("com.fav1", "WeChat", 'W')
        val fav2 = app("com.fav2", "Alipay", 'A')
        val a1 = app("com.a1", "Alpha", 'A')
        val m1 = app("com.m1", "Maps", 'M')
        val grouped = mapOf('A' to listOf(a1), 'M' to listOf(m1))

        val result = HomeListModel.build(favorites = listOf(fav1, fav2), appsByLetter = grouped)

        // 0: Glance, 1: FavHeader, 2: fav1, 3: fav2, 4: FavDivider, 5: Header A, 6: a1, 7: Header M
        assertEquals(5, result.letterIndexMap['A'])
        assertEquals(7, result.letterIndexMap['M'])
        assertEquals(HomeListEntry.LetterHeader('A'), result.entries[5])
        assertEquals(HomeListEntry.LetterHeader('M'), result.entries[7])
    }

    @Test
    fun testEmptyLetterResolvesToNearestPopulatedLetter() {
        val grouped = mapOf(
            'A' to listOf(app("com.a", "Alpha", 'A')),
            'F' to listOf(app("com.f", "Files", 'F'))
        )
        val result = HomeListModel.build(favorites = emptyList(), appsByLetter = grouped)

        // 'B' is closer to 'A' (distance 1) than 'F' (distance 4)
        assertEquals(result.letterIndexMap['A'], HomeListModel.resolveTargetLetterIndex('B', result.letterIndexMap))
        // 'E' is closer to 'F' (distance 1) than 'A' (distance 4)
        assertEquals(result.letterIndexMap['F'], HomeListModel.resolveTargetLetterIndex('E', result.letterIndexMap))
        // Empty map returns null
        assertNull(HomeListModel.resolveTargetLetterIndex('A', emptyMap()))
    }
}
