package com.niagara.launcher

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.niagara.launcher.core.data.PreferencesRepository
import com.niagara.launcher.core.model.BuiltInWidgetKind
import com.niagara.launcher.core.model.HomeSectionOrderCodec
import com.niagara.launcher.core.model.HomeSectionType
import com.niagara.launcher.core.model.HomeWidgetCodec
import com.niagara.launcher.core.model.HomeWidgetItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class HomeWidgetAndLayoutTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testHomeWidgetCodecRoundTripEscapingAndManipulations() {
        val weather = HomeWidgetItem.BuiltIn(
            id = "builtin_weather",
            kind = BuiltInWidgetKind.WEATHER,
            heightDp = 96
        )
        val system = HomeWidgetItem.SystemAppWidget(
            id = "sys_42",
            appWidgetId = 42,
            providerPackage = "com.example.clock",
            providerClass = "com.example.clock.ClockWidget",
            label = "Clock | 100% ; Pro",
            heightDp = 180
        )

        val encoded = HomeWidgetCodec.encode(listOf(weather, system))
        val decoded = HomeWidgetCodec.decode(encoded)
        assertEquals(listOf(weather, system), decoded)

        // Resize clamps within MIN..MAX
        val resizedSmall = HomeWidgetCodec.resize(decoded, "sys_42", 10)
        assertEquals(HomeWidgetCodec.MIN_WIDGET_HEIGHT_DP, resizedSmall[1].heightDp)
        val resizedLarge = HomeWidgetCodec.resize(decoded, "sys_42", 999)
        assertEquals(HomeWidgetCodec.MAX_WIDGET_HEIGHT_DP, resizedLarge[1].heightDp)

        // Move and reorder
        val moved = HomeWidgetCodec.move(decoded, "sys_42", -1)
        assertEquals(listOf(system, weather), moved)
        assertEquals(listOf(weather, system), HomeWidgetCodec.reorder(moved, listOf("builtin_weather", "sys_42")))

        // Remove
        assertEquals(listOf(weather), HomeWidgetCodec.remove(decoded, "sys_42"))

        // Capacity cap at MAX_HOME_WIDGETS
        var list = emptyList<HomeWidgetItem>()
        for (i in 1..HomeWidgetCodec.MAX_HOME_WIDGETS) {
            val (next, added) = HomeWidgetCodec.add(
                list,
                HomeWidgetItem.SystemAppWidget("sys_$i", i, "pkg$i", "cls$i", "W$i")
            )
            assertTrue(added)
            list = next
        }
        val (overflowList, overflowAdded) = HomeWidgetCodec.add(
            list,
            HomeWidgetItem.BuiltIn("builtin_clock", BuiltInWidgetKind.DIGITAL_CLOCK)
        )
        assertFalse(overflowAdded)
        assertEquals(HomeWidgetCodec.MAX_HOME_WIDGETS, overflowList.size)
    }

    @Test
    fun testHomeSectionOrderCodecNormalizationAndMoves() {
        assertEquals(HomeSectionOrderCodec.DEFAULT_ORDER, HomeSectionOrderCodec.decode(null))
        assertEquals(HomeSectionOrderCodec.DEFAULT_ORDER, HomeSectionOrderCodec.decode("bad_token;;widgets"))

        val custom = listOf(HomeSectionType.DESKTOP_APPS, HomeSectionType.WIDGETS, HomeSectionType.FAVORITES)
        val encoded = HomeSectionOrderCodec.encode(custom)
        assertEquals(custom, HomeSectionOrderCodec.decode(encoded))

        val moved = HomeSectionOrderCodec.move(HomeSectionOrderCodec.DEFAULT_ORDER, HomeSectionType.DESKTOP_APPS, -1)
        assertEquals(
            listOf(HomeSectionType.WIDGETS, HomeSectionType.DESKTOP_APPS, HomeSectionType.FAVORITES),
            moved
        )
    }

    @Test
    fun testPreferencesRepositoryPersistsWidgetsSectionOrderAndDoubleTapLock() = runTest {
        val dataFile = tempFolder.newFile("home_widgets_v2.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { dataFile }
        )
        val repo = PreferencesRepository(dataStore)

        val initial = repo.launcherPreferencesFlow.first()
        assertTrue(initial.doubleTapToLockEnabled)
        assertTrue(initial.homeWidgets.isEmpty())
        assertEquals(HomeSectionOrderCodec.DEFAULT_ORDER, initial.homeSectionOrder)

        val w1 = HomeWidgetItem.BuiltIn("builtin_weather", BuiltInWidgetKind.WEATHER, 96)
        val w2 = HomeWidgetItem.SystemAppWidget("sys_7", 7, "com.cal", ".CalWidget", "Calendar", 164)
        assertTrue(repo.addHomeWidget(w1))
        assertTrue(repo.addHomeWidget(w2))
        repo.resizeHomeWidget("sys_7", 220)
        repo.moveHomeWidget("sys_7", -1)
        repo.moveHomeSection(HomeSectionType.DESKTOP_APPS, -2)
        repo.setDoubleTapToLockEnabled(false)
        advanceUntilIdle()

        val updated = repo.launcherPreferencesFlow.first()
        assertFalse(updated.doubleTapToLockEnabled)
        assertEquals(listOf(w2.copy(heightDp = 220), w1), updated.homeWidgets)
        assertEquals(
            listOf(HomeSectionType.DESKTOP_APPS, HomeSectionType.WIDGETS, HomeSectionType.FAVORITES),
            updated.homeSectionOrder
        )

        repo.removeHomeWidget("sys_7")
        advanceUntilIdle()
        assertEquals(listOf(w1), repo.launcherPreferencesFlow.first().homeWidgets)
    }
}
