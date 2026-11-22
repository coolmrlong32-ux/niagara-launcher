package com.niagara.launcher

import com.niagara.launcher.core.model.AppItem
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppFilterTest {

    @Test
    fun testAppQueryMatching() {
        val weChat = AppItem(
            packageName = "com.tencent.mm",
            activityName = "com.tencent.mm.ui.LauncherUI",
            label = "微信",
            firstLetter = 'W',
            pinyinInitials = "WX",
            fullPinyin = "weixin"
        )

        val alipay = AppItem(
            packageName = "com.eg.android.AlipayGphone",
            activityName = "com.alipay.mobile.quinox.LauncherActivity",
            label = "支付宝",
            firstLetter = 'Z',
            pinyinInitials = "ZFB",
            fullPinyin = "zhifubao"
        )

        val chrome = AppItem(
            packageName = "com.android.chrome",
            activityName = "com.google.android.apps.chrome.Main",
            label = "Google Chrome",
            firstLetter = 'G',
            pinyinInitials = "GC",
            fullPinyin = "google chrome"
        )

        // Matching WeChat
        assertTrue(weChat.matchesQuery("微"))
        assertTrue(weChat.matchesQuery("wx"))
        assertTrue(weChat.matchesQuery("WX"))
        assertTrue(weChat.matchesQuery("wei"))
        assertTrue(weChat.matchesQuery("weixin"))
        assertFalse(weChat.matchesQuery("zfb"))

        // Matching Alipay
        assertTrue(alipay.matchesQuery("支"))
        assertTrue(alipay.matchesQuery("zfb"))
        assertTrue(alipay.matchesQuery("zhifu"))
        assertFalse(alipay.matchesQuery("wx"))

        // Matching Chrome
        assertTrue(chrome.matchesQuery("chr"))
        assertTrue(chrome.matchesQuery("CHROME"))
        assertTrue(chrome.matchesQuery("google"))
    }
}
