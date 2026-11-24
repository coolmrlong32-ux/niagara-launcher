package com.niagara.launcher

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.niagara.launcher.core.data.PreferencesKeyCodec
import com.niagara.launcher.core.model.LauncherPreferences
import com.niagara.launcher.core.theme.IosSquircleMath
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
class HomeDesktopTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testPreferencesKeyCodecDecodeAndEncodeDeduplicatesAndFiltersBlanks() {
        val decoded = PreferencesKeyCodec.decode(" com.a/.Main ;; com.b/.Main ; com.a/.Main ; ")
        assertEquals(listOf("com.a/.Main", "com.b/.Main"), decoded)

        val encoded = PreferencesKeyCodec.encode(listOf("com.a/.Main", "", "com.b/.Main", "com.a/.Main"))
        assertEquals("com.a/.Main;com.b/.Main", encoded)
    }

    @Test
    fun testPreferencesKeyCodecToggleAndAddRespectDesktopCapacity() {
        val initial = (1..LauncherPreferences.MAX_DESKTOP_APPS).map { "com.app$it/.Main" }

        // Adding a 25th app should fail and leave list unchanged
        val (afterAdd, addSucceeded) = PreferencesKeyCodec.add(
            current = initial,
            key = "com.extra/.Main",
            maxSize = LauncherPreferences.MAX_DESKTOP_APPS
        )
        assertFalse(addSucceeded)
        assertEquals(LauncherPreferences.MAX_DESKTOP_APPS, afterAdd.size)

        // Adding an already-present key is an idempotent success
        val (_, existingAddSucceeded) = PreferencesKeyCodec.add(
            current = initial,
            key = "com.app1/.Main",
            maxSize = LauncherPreferences.MAX_DESKTOP_APPS
        )
        assertTrue(existingAddSucceeded)

        // Toggling an existing app removes it and succeeds even when at capacity
        val (afterRemove, removeSucceeded) = PreferencesKeyCodec.toggle(
            current = initial,
            key = "com.app3/.Main",
            maxSize = LauncherPreferences.MAX_DESKTOP_APPS
        )
        assertTrue(removeSucceeded)
        assertEquals(LauncherPreferences.MAX_DESKTOP_APPS - 1, afterRemove.size)
        assertFalse(afterRemove.contains("com.app3/.Main"))
    }

    @Test
    fun testPreferencesKeyCodecMoveReorderingBounds() {
        val list = listOf("a", "b", "c")
        assertEquals(listOf("b", "a", "c"), PreferencesKeyCodec.move(list, "b", -1))
        assertEquals(listOf("a", "b", "c"), PreferencesKeyCodec.move(list, "a", -1))
        assertEquals(listOf("a", "c", "b"), PreferencesKeyCodec.move(list, "b", 1))
        assertEquals(listOf("a", "b", "c"), PreferencesKeyCodec.move(list, "c", 1))
    }

    @Test
    fun testDataStoreRoundTripForFavoritesAndDesktopKeys() = runTest {
        val dataFile = tempFolder.newFile("desktop_prefs.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { dataFile }
        )

        val favKey = stringPreferencesKey("favorite_app_keys")
        val desktopKey = stringPreferencesKey("desktop_app_keys")

        dataStore.edit { prefs ->
            prefs[favKey] = PreferencesKeyCodec.encode(listOf("com.wechat/.Main", "com.alipay/.Main"))
            prefs[desktopKey] = PreferencesKeyCodec.encode(listOf("com.camera/.Main", "com.music/.Main"))
        }
        advanceUntilIdle()

        val snapshot = dataStore.data.first()
        assertEquals(
            listOf("com.wechat/.Main", "com.alipay/.Main"),
            PreferencesKeyCodec.decode(snapshot[favKey])
        )
        assertEquals(
            listOf("com.camera/.Main", "com.music/.Main"),
            PreferencesKeyCodec.decode(snapshot[desktopKey])
        )
    }

    @Test
    fun testIosSquircleMathClampsRadiusAndTangentWithinBounds() {
        val radius = IosSquircleMath.effectiveCornerRadius(width = 100f, height = 100f, cornerFraction = 0.225f)
        assertEquals(22.5f, radius, 0.001f)

        val clampedRadius = IosSquircleMath.effectiveCornerRadius(width = 100f, height = 80f, cornerFraction = 0.99f)
        assertTrue(clampedRadius <= 40f)

        val tangent = IosSquircleMath.smoothTangentLength(radius = radius, width = 100f, height = 100f)
        assertTrue(tangent > radius)
        assertTrue(tangent <= 50f)

        assertEquals(0f, IosSquircleMath.effectiveCornerRadius(0f, 100f, 0.225f), 0.001f)
    }
}
