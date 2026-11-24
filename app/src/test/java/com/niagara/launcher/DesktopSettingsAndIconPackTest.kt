package com.niagara.launcher

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.niagara.launcher.core.data.AppIconCache
import com.niagara.launcher.core.data.IconPackAppFilterParser
import com.niagara.launcher.core.data.PreferencesRepository
import com.niagara.launcher.core.model.DesktopLayoutMath
import com.niagara.launcher.core.model.IconStyle
import com.niagara.launcher.core.model.LauncherPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DesktopSettingsAndIconPackTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testIconStyleStorageKeyMappingAndFallback() {
        assertEquals(IconStyle.IOS_SQUIRCLE, IconStyle.fromStorageKey("ios_squircle"))
        assertEquals(IconStyle.CIRCLE, IconStyle.fromStorageKey("circle"))
        assertEquals(IconStyle.ADAPTIVE, IconStyle.fromStorageKey("adaptive"))
        assertEquals(IconStyle.IOS_SQUIRCLE, IconStyle.fromStorageKey(null))
        assertEquals(IconStyle.IOS_SQUIRCLE, IconStyle.fromStorageKey("unknown_style"))
    }

    @Test
    fun testLauncherPreferencesHelpersAndBounds() {
        assertEquals(4, LauncherPreferences.normalizeGridColumns(4))
        assertEquals(5, LauncherPreferences.normalizeGridColumns(5))
        assertEquals(4, LauncherPreferences.normalizeGridColumns(3))
        assertEquals(4, LauncherPreferences.normalizeGridColumns(6))

        assertEquals(LauncherPreferences.MIN_ICON_SIZE_DP, LauncherPreferences.clampIconSizeDp(20f), 0.001f)
        assertEquals(52f, LauncherPreferences.clampIconSizeDp(52f), 0.001f)
        assertEquals(LauncherPreferences.MAX_ICON_SIZE_DP, LauncherPreferences.clampIconSizeDp(90f), 0.001f)
        assertEquals(LauncherPreferences.DEFAULT_ICON_SIZE_DP, LauncherPreferences.clampIconSizeDp(Float.NaN), 0.001f)
        assertEquals(LauncherPreferences.DEFAULT_ICON_SIZE_DP, LauncherPreferences.clampIconSizeDp(Float.POSITIVE_INFINITY), 0.001f)

        val fourColPrefs = LauncherPreferences(desktopGridColumns = 4)
        val fiveColPrefs = LauncherPreferences(desktopGridColumns = 5)
        assertEquals(24, fourColPrefs.maxDesktopApps)
        assertEquals(30, fiveColPrefs.maxDesktopApps)
    }

    @Test
    fun testDesktopLayoutMathResponsiveSizingAndSpacing() {
        assertEquals(24, DesktopLayoutMath.maxDesktopApps(4))
        assertEquals(30, DesktopLayoutMath.maxDesktopApps(5))
        assertEquals(24, DesktopLayoutMath.maxDesktopApps(99))

        // Large icon size gets bounded inside narrow 5-column cell width (e.g., 58dp - 6dp = 52dp)
        assertEquals(52f, DesktopLayoutMath.resolveIconSizeDp(64f, availableCellWidthDp = 58f), 0.001f)
        // Small icon size fits within cell without further shrinking
        assertEquals(46f, DesktopLayoutMath.resolveIconSizeDp(46f, availableCellWidthDp = 64f), 0.001f)

        assertEquals(8f, DesktopLayoutMath.horizontalSpacingDp(4), 0.001f)
        assertEquals(4f, DesktopLayoutMath.horizontalSpacingDp(5), 0.001f)
        assertEquals(11.5f, DesktopLayoutMath.labelFontSizeSp(4), 0.001f)
        assertEquals(10.5f, DesktopLayoutMath.labelFontSizeSp(5), 0.001f)
    }

    @Test
    fun testPreferencesRepositoryRoundTripAndDynamicGridCapacity() = runTest {
        val dataFile = tempFolder.newFile("desktop_settings_repo.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { dataFile }
        )
        val repository = PreferencesRepository(dataStore)

        // Default values
        val initial = repository.launcherPreferencesFlow.first()
        assertEquals(LauncherPreferences.DEFAULT_ICON_SIZE_DP, initial.desktopIconSizeDp, 0.001f)
        assertEquals(4, initial.desktopGridColumns)
        assertTrue(initial.showAppLabels)
        assertTrue(initial.showFavoritesBar)
        assertEquals(IconStyle.IOS_SQUIRCLE, initial.iconStyle)
        assertNull(initial.iconPackPackage)

        // Persist custom settings
        repository.setDesktopIconSizeDp(60f)
        repository.setDesktopGridColumns(5)
        repository.setShowAppLabels(false)
        repository.setShowFavoritesBar(false)
        repository.setIconStyle(IconStyle.CIRCLE)
        repository.setIconPackPackage("com.example.iconpack")
        advanceUntilIdle()

        val updated = repository.launcherPreferencesFlow.first()
        assertEquals(60f, updated.desktopIconSizeDp, 0.001f)
        assertEquals(5, updated.desktopGridColumns)
        assertFalse(updated.showAppLabels)
        assertFalse(updated.showFavoritesBar)
        assertEquals(IconStyle.CIRCLE, updated.iconStyle)
        assertEquals("com.example.iconpack", updated.iconPackPackage)

        // 5-column mode supports 30 desktop apps, 31st is rejected
        for (i in 1..30) {
            assertTrue("Expected slot $i to succeed in 5-col mode", repository.addDesktopApp("com.app$i/.Main"))
        }
        assertFalse(repository.addDesktopApp("com.app31/.Main"))
        advanceUntilIdle()
        assertEquals(30, repository.launcherPreferencesFlow.first().desktopAppKeys.size)

        // Switching to 4-column mode preserves all 30 persisted keys while blocking new additions
        repository.setDesktopGridColumns(4)
        advanceUntilIdle()
        assertFalse(repository.addDesktopApp("com.app32/.Main"))
        assertEquals(30, repository.launcherPreferencesFlow.first().desktopAppKeys.size)

        // Switching back to 5-column mode keeps all 30 apps intact
        repository.setDesktopGridColumns(5)
        advanceUntilIdle()
        assertEquals(30, repository.launcherPreferencesFlow.first().desktopAppKeys.size)

        // Clearing icon pack reverts to null
        repository.setIconPackPackage(null)
        advanceUntilIdle()
        assertNull(repository.launcherPreferencesFlow.first().iconPackPackage)
    }

    @Test
    fun testIconPackAppFilterParserComponentsTemplatesAndMalformedResilience() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <iconback img1="iconback_1" img2="iconback_2" />
                <iconmask img1="iconmask_alpha" />
                <iconupon img1="iconupon_gloss" />
                <scale factor="0.85" />
                <item component="ComponentInfo{com.tencent.mm/com.tencent.mm.ui.LauncherUI}" drawable="wechat" />
                <item component="ComponentInfo{com.example.notes/.MainActivity}" drawable="notes_custom" />
                <calendar component="ComponentInfo{com.google.android.calendar/.AllInOneActivity}" prefix="calendar_" />
            </resources>
        """.trimIndent()

        val parsed = IconPackAppFilterParser.parseXml(xml)
        assertEquals(listOf("iconback_1", "iconback_2"), parsed.iconBacks)
        assertEquals("iconmask_alpha", parsed.iconMask)
        assertEquals("iconupon_gloss", parsed.iconUpon)
        assertEquals(0.85f, parsed.scale, 0.001f)

        // Exact match
        assertEquals(
            "wechat",
            parsed.resolveDrawableName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI")
        )
        // Shorthand activity normalization
        assertEquals(
            "notes_custom",
            parsed.resolveDrawableName("com.example.notes", ".MainActivity")
        )
        assertEquals(
            "notes_custom",
            parsed.resolveDrawableName("com.example.notes", "com.example.notes.MainActivity")
        )
        // Package fallback when activity differs
        assertEquals(
            "wechat",
            parsed.resolveDrawableName("com.tencent.mm", ".OtherActivity")
        )
        // Dynamic calendar resolution
        assertEquals(
            "calendar_24",
            parsed.resolveDrawableName(
                packageName = "com.google.android.calendar",
                activityName = "com.google.android.calendar.AllInOneActivity",
                dayOfMonth = 24
            )
        )

        // Out-of-range scale clamping and malformed XML safety
        val clampedAndBrokenXml = """
            <resources>
                <scale factor="9.9" />
                <item component="ComponentInfo{com.ok/.Main}" drawable="ok_icon" />
                <item component="broken"
        """.trimIndent()
        val partial = IconPackAppFilterParser.parseXml(clampedAndBrokenXml)
        assertEquals(1.5f, partial.scale, 0.001f)
        assertEquals("ok_icon", partial.resolveDrawableName("com.ok", ".Main"))
    }

    @Test
    fun testAppIconCacheBuildCacheKeyIsolatesPacksAndAdaptiveIcons() {
        val squircleKey = AppIconCache.buildCacheKey("com.app", ".Main", IconStyle.IOS_SQUIRCLE, null)
        val circleKey = AppIconCache.buildCacheKey("com.app", ".Main", IconStyle.CIRCLE, null)
        val adaptiveKey = AppIconCache.buildCacheKey("com.app", ".Main", IconStyle.ADAPTIVE, null)
        val packAKey = AppIconCache.buildCacheKey("com.app", ".Main", IconStyle.IOS_SQUIRCLE, "com.pack.a")
        val packBKey = AppIconCache.buildCacheKey("com.app", ".Main", IconStyle.CIRCLE, "com.pack.b")

        assertEquals(squircleKey, circleKey)
        assertTrue(squircleKey != adaptiveKey)
        assertTrue(packAKey != squircleKey)
        assertTrue(packAKey != packBKey)
        assertEquals("pack:com.pack.a|com.app/.Main", packAKey)
    }
}
