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
    fun testAppQueryMatchingWithRealPinyinEngine() {
        val weChat = buildApp("com.tencent.mm", "微信")
        val weChatRead = buildApp("com.tencent.weread", "微信读书")
        val alipay = buildApp("com.eg.android.AlipayGphone", "支付宝")
        val chrome = buildApp("com.android.chrome", "Google Chrome")
        val youtube = buildApp("com.google.android.youtube", "YouTube")
        val neteaseMusic = buildApp("com.netease.cloudmusic", "网易云音乐")
        val qqMusic = buildApp("com.tencent.qqmusic", "QQ音乐")
        val cmbBank = buildApp("cmb.pb", "招商银行")

        // WeChat & WeChat Read
        assertTrue(weChat.matchesQuery("微"))
        assertTrue(weChat.matchesQuery("wx"))
        assertTrue(weChat.matchesQuery("WX"))
        assertTrue(weChat.matchesQuery("weix"))
        assertTrue(weChat.matchesQuery("weixin"))
        assertTrue(weChatRead.matchesQuery("wx读书"))
        assertFalse(weChat.matchesQuery("zfb"))

        // Alipay
        assertTrue(alipay.matchesQuery("支"))
        assertTrue(alipay.matchesQuery("zfb"))
        assertTrue(alipay.matchesQuery("zhifu"))

        // English word & CamelCase initials
        assertTrue(chrome.matchesQuery("gc"))
        assertTrue(chrome.matchesQuery("chr"))
        assertTrue(chrome.matchesQuery("CHROME"))
        assertTrue(youtube.matchesQuery("yt"))

        // Polyphone & mixed queries
        assertTrue(neteaseMusic.matchesQuery("wyyyy"))
        assertTrue(neteaseMusic.matchesQuery("wyyyl"))
        assertTrue(neteaseMusic.matchesQuery("yinyue"))
        assertTrue(qqMusic.matchesQuery("qqyy"))
        assertTrue(cmbBank.matchesQuery("zsyh"))
        assertTrue(cmbBank.matchesQuery("yinhang"))

        // Package name fallback
        assertTrue(weChat.matchesQuery("tencent"))
    }

    @Test
    fun testRelevanceRankingOrder() {
        val weChatRead = buildApp("com.tencent.weread", "微信读书")
        val weChat = buildApp("com.tencent.mm", "微信")
        val weibo = buildApp("com.sina.weibo", "微博")
        val enterpriseWeChat = buildApp("com.tencent.wework", "企业微信")

        val rankedByWx = AppSearchEngine.search(
            listOf(weChatRead, enterpriseWeChat, weibo, weChat),
            "wx"
        )
        // Exact initials "wx" -> "微信" ranks first before prefix "wxds" ("微信读书") and substring ("企业微信")
        assertEquals("微信", rankedByWx.first().label)
        assertEquals("微信读书", rankedByWx[1].label)

        // Favorite boost breaks ties or promotes pinned app within same tier
        val favBank = buildApp("cmb.pb", "招商银行", isFavorite = true)
        val normalBank = buildApp("com.icbc", "工商银行", isFavorite = false)
        val rankedBanks = AppSearchEngine.search(listOf(normalBank, favBank), "yh")
        assertEquals("招商银行", rankedBanks.first().label)
    }
}
