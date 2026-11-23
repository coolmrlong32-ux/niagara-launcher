package com.niagara.launcher

import com.niagara.launcher.core.model.AppItem
import com.niagara.launcher.core.pinyin.PinyinEngine
import com.niagara.launcher.core.search.AppSearchEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppFilterTest {

    private fun buildApp(
        pkg: String,
        label: String,
        isFavorite: Boolean = false
    ): AppItem {
        val index = PinyinEngine.buildIndex(label)
        return AppItem(
            packageName = pkg,
            activityName = ".MainActivity",
            label = label,
            firstLetter = index.firstLetter,
            pinyinInitials = index.primaryInitials,
            fullPinyin = index.primaryFullPinyin,
            pinyinIndex = index,
            isFavorite = isFavorite
        )
    }

    @Test
    fun testRealPinyinAndEnglishInitialMatching() {
        val weChat = buildApp("com.tencent.mm", "微信")
        val alipay = buildApp("com.eg.android.AlipayGphone", "支付宝")
        val chrome = buildApp("com.android.chrome", "Google Chrome")
        val youtube = buildApp("com.google.android.youtube", "YouTube")

        assertTrue(weChat.matchesQuery("微"))
        assertTrue(weChat.matchesQuery("wx"))
        assertTrue(weChat.matchesQuery("WX"))
        assertTrue(weChat.matchesQuery("weix"))
        assertTrue(weChat.matchesQuery("weixin"))
        assertFalse(weChat.matchesQuery("zfb"))

        assertTrue(alipay.matchesQuery("支"))
        assertTrue(alipay.matchesQuery("zfb"))
        assertTrue(alipay.matchesQuery("zhifu"))

        assertTrue(chrome.matchesQuery("gc"))
        assertTrue(chrome.matchesQuery("chr"))
        assertTrue(chrome.matchesQuery("CHROME"))
        assertTrue(chrome.matchesQuery("google"))

        assertTrue(youtube.matchesQuery("yt"))
        assertTrue(youtube.matchesQuery("tube"))
    }

    @Test
    fun testPolyphoneAndMixedQueryMatching() {
        val neteaseMusic = buildApp("com.netease.cloudmusic", "网易云音乐")
        val cmbBank = buildApp("cmb.pb", "招商银行")
        val qqMusic = buildApp("com.tencent.qqmusic", "QQ音乐")
        val weRead = buildApp("com.tencent.weread", "微信读书")

        assertTrue(neteaseMusic.matchesQuery("wyyyy"))
        assertTrue(neteaseMusic.matchesQuery("wyyyl"))
        assertTrue(neteaseMusic.matchesQuery("yinyue"))
        assertTrue(neteaseMusic.matchesQuery("yinle"))

        assertTrue(cmbBank.matchesQuery("zsyh"))
        assertTrue(cmbBank.matchesQuery("yinhang"))

        assertTrue(qqMusic.matchesQuery("qqyy"))
        assertTrue(qqMusic.matchesQuery("qqyin"))
        assertTrue(qqMusic.matchesQuery("qq音乐"))

        assertTrue(weRead.matchesQuery("wx读书"))
    }

    @Test
    fun testRelevanceRankingOrder() {
        val weChat = buildApp("com.tencent.mm", "微信")
        val weRead = buildApp("com.tencent.weread", "微信读书")
        val weWork = buildApp("com.tencent.wework", "企业微信")
        val favoriteWeRead = buildApp("com.tencent.weread", "微信读书", isFavorite = true)

        val rankedForWx = AppSearchEngine.search(listOf(weWork, weRead, weChat), "wx")
        assertEquals(listOf("微信", "微信读书", "企业微信"), rankedForWx.map { it.label })

        val rankedWithFavoriteTieBreaker = AppSearchEngine.search(
            listOf(weWork, weRead, favoriteWeRead),
            "wxds"
        )
        assertEquals("微信读书", rankedWithFavoriteTieBreaker.first().label)
        assertTrue(rankedWithFavoriteTieBreaker.first().isFavorite)
    }

    @Test
    fun testPackageNameFallbackMatching() {
        val qq = buildApp("com.tencent.mobileqq", "QQ")
        assertTrue(qq.matchesQuery("tencent"))
    }
}
