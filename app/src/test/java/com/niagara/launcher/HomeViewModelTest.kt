package com.niagara.launcher

import android.content.Context
import android.content.pm.PackageManager
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.niagara.launcher.core.data.AppRepository
import com.niagara.launcher.core.data.PreferencesRepository
import com.niagara.launcher.core.model.AppItem
import com.niagara.launcher.core.pinyin.PinyinEngine
import com.niagara.launcher.ui.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createSampleApp(pkg: String, label: String): AppItem {
        val index = PinyinEngine.buildIndex(label)
        return AppItem(
            packageName = pkg,
            activityName = ".MainActivity",
            label = label,
            firstLetter = index.firstLetter,
            pinyinInitials = index.primaryInitials,
            fullPinyin = index.primaryFullPinyin,
            pinyinIndex = index
        )
    }

    @Test
    fun testSearchRelevanceAndHiddenAppSeparation() {
        val wechat = createSampleApp("com.tencent.mm", "微信")
        val weread = createSampleApp("com.tencent.weread", "微信读书")
        val hiddenVault = createSampleApp("com.vault.secret", "私密保险箱")

        val visibleResults = com.niagara.launcher.core.search.AppSearchEngine.search(
            listOf(weread, wechat),
            "wx"
        )
        val hiddenResults = com.niagara.launcher.core.search.AppSearchEngine.search(
            listOf(hiddenVault),
            "smbx"
        )

        assertEquals(listOf("微信", "微信读书"), visibleResults.map { it.label })
        assertEquals(listOf("私密保险箱"), hiddenResults.map { it.label })
        assertTrue(hiddenResults.none { it.packageName == "com.tencent.mm" })
    }

    @Test
    fun testPreferencesKeyEncodingAndReorderingRules() = runTest {
        val dataFile = tempFolder.newFile("test_prefs.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { dataFile }
        )

        val favKey = stringPreferencesKey("favorite_app_keys")
        val hiddenKey = stringSetPreferencesKey("hidden_app_keys")

        dataStore.edit { prefs ->
            prefs[favKey] = "com.a/.Main;com.b/.Main;com.c/.Main"
            prefs[hiddenKey] = setOf("com.b/.Main")
        }
        advanceUntilIdle()

        var savedFavs: List<String> = emptyList()
        var savedHidden: Set<String> = emptySet()
        dataStore.edit { prefs ->
            savedFavs = (prefs[favKey] ?: "").split(";").filter { it.isNotBlank() }
            savedHidden = prefs[hiddenKey] ?: emptySet()
        }

        val visibleFavs = savedFavs.filterNot { savedHidden.contains(it) }
        assertEquals(listOf("com.a/.Main", "com.c/.Main"), visibleFavs)
        assertFalse(visibleFavs.contains("com.b/.Main"))
    }
}
