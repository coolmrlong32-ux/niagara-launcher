package com.niagara.launcher

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.niagara.launcher.core.data.PreferencesKeyCodec
import com.niagara.launcher.core.data.PreferencesRepository
import com.niagara.launcher.core.model.GridReorderMath
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class GridReorderMathTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testIndexToCellOriginAndGridHeightFor4And5Columns() {
        val origin = GridReorderMath.indexToCellOrigin(
            index = 5,
            columns = 4,
            cellWidthPx = 100f,
            cellHeightPx = 120f,
            horizontalSpacingPx = 12f,
            verticalSpacingPx = 16f
        )
        // index 5 in 4 cols -> row 1, col 1
        assertEquals(112f, origin.x, 0.01f)
        assertEquals(136f, origin.y, 0.01f)

        // 6 items in 4 cols -> 2 rows: 2 * 120 + 16 = 256
        assertEquals(256f, GridReorderMath.calculateGridHeightPx(6, 4, 120f, 16f), 0.01f)
        // 6 items in 5 cols -> 2 rows as well
        assertEquals(256f, GridReorderMath.calculateGridHeightPx(6, 5, 120f, 16f), 0.01f)
    }

    @Test
    fun testPointToTargetIndexSnapsAndClampsIncompleteLastRow() {
        // 6 items in 4-column grid (row 0: 0..3, row 1: 4..5)
        // Dragging over row 1 col 3 (empty trailing slot) clamps safely to last item index 5
        val target = GridReorderMath.pointToTargetIndex(
            centerX = 390f,
            centerY = 196f,
            columns = 4,
            itemCount = 6,
            cellWidthPx = 100f,
            cellHeightPx = 120f,
            horizontalSpacingPx = 10f,
            verticalSpacingPx = 16f,
            currentIndex = 0
        )
        assertEquals(5, target)

        // Hysteresis prevents premature jitter right on boundary of slot 0 -> 1
        val steadyIndex = GridReorderMath.pointToTargetIndex(
            centerX = 106f, // midpoint is 105f; hysteresis requires crossing 116f
            centerY = 60f,
            columns = 4,
            itemCount = 6,
            cellWidthPx = 100f,
            cellHeightPx = 120f,
            horizontalSpacingPx = 10f,
            verticalSpacingPx = 16f,
            currentIndex = 0,
            hysteresisFraction = 0.10f
        )
        assertEquals(0, steadyIndex)

        // Clearly past threshold swaps to slot 1
        val movedIndex = GridReorderMath.pointToTargetIndex(
            centerX = 125f,
            centerY = 60f,
            columns = 4,
            itemCount = 6,
            cellWidthPx = 100f,
            cellHeightPx = 120f,
            horizontalSpacingPx = 10f,
            verticalSpacingPx = 16f,
            currentIndex = 0,
            hysteresisFraction = 0.10f
        )
        assertEquals(1, movedIndex)
    }

    @Test
    fun testReorderListAndApplyVisibleOrderPreservesHiddenApps() {
        val items = listOf("a", "b", "c", "d")
        assertEquals(listOf("b", "c", "a", "d"), GridReorderMath.reorder(items, 0, 2))
        assertEquals(listOf("d", "a", "b", "c"), GridReorderMath.reorder(items, 3, 0))
        assertEquals(items, GridReorderMath.reorder(items, 1, 1))
        assertEquals(items, GridReorderMath.reorder(items, -1, 2))

        // Stored list includes a hidden app ("hidden_x"); visible reorder must keep it intact
        val stored = listOf("a", "hidden_x", "b", "c")
        val reorderedVisible = listOf("c", "a", "b")
        assertEquals(
            listOf("c", "hidden_x", "a", "b"),
            PreferencesKeyCodec.applyVisibleOrder(stored, reorderedVisible)
        )
    }

    @Test
    fun testPreferencesRepositoryPersistsDraggedFavoritesAndDesktopOrder() = runTest {
        val dataFile = tempFolder.newFile("drag_order.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { dataFile }
        )
        val repo = PreferencesRepository(dataStore)

        repo.setFavorites(listOf("com.a/.Main", "com.hidden/.Main", "com.b/.Main", "com.c/.Main"))
        repo.setDesktopApps(listOf("com.1/.Main", "com.2/.Main", "com.3/.Main", "com.4/.Main"))
        advanceUntilIdle()

        // Drag reorder visible favorites (c moved to front) and desktop apps (4 moved before 2)
        repo.reorderFavorites(listOf("com.c/.Main", "com.a/.Main", "com.b/.Main"))
        repo.reorderDesktopApps(listOf("com.1/.Main", "com.4/.Main", "com.2/.Main", "com.3/.Main"))
        advanceUntilIdle()

        val prefs = repo.launcherPreferencesFlow.first()
        assertEquals(
            listOf("com.c/.Main", "com.hidden/.Main", "com.a/.Main", "com.b/.Main"),
            prefs.favoriteKeys
        )
        assertEquals(
            listOf("com.1/.Main", "com.4/.Main", "com.2/.Main", "com.3/.Main"),
            prefs.desktopAppKeys
        )
    }
}
