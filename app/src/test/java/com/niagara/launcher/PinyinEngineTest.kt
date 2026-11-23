package com.niagara.launcher

import com.niagara.launcher.core.pinyin.PinyinEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinyinEngineTest {

    @Test
    fun testFirstLetterExtraction() {
        assertEquals('W', PinyinEngine.getFirstLetter("微信"))
        assertEquals('Z', PinyinEngine.getFirstLetter("支付宝"))
        assertEquals('Y', PinyinEngine.getFirstLetter("银行"))
        assertEquals('Y', PinyinEngine.getFirstLetter("音乐"))
        assertEquals('C', PinyinEngine.getFirstLetter("重庆"))
        assertEquals('M', PinyinEngine.getFirstLetter("魅族"))
        assertEquals('Q', PinyinEngine.getFirstLetter("麒麟"))
        assertEquals('X', PinyinEngine.getFirstLetter("鑫"))
        assertEquals('C', PinyinEngine.getFirstLetter("Chrome"))
        assertEquals('Y', PinyinEngine.getFirstLetter("YouTube"))
        assertEquals('#', PinyinEngine.getFirstLetter("12306"))
        assertEquals('#', PinyinEngine.getFirstLetter("@App"))
        assertEquals('#', PinyinEngine.getFirstLetter("   "))
    }

    @Test
    fun testPinyinInitialsExtraction() {
        assertEquals("WX", PinyinEngine.getPinyinInitials("微信"))
        assertEquals("ZFB", PinyinEngine.getPinyinInitials("支付宝"))
        assertEquals("MT", PinyinEngine.getPinyinInitials("美团"))
        assertEquals("S", PinyinEngine.getPinyinInitials("Setting"))
        assertEquals("GC", PinyinEngine.getPinyinInitials("Google Chrome"))
        assertEquals("YT", PinyinEngine.getPinyinInitials("YouTube"))
        assertEquals("WC", PinyinEngine.getPinyinInitials("WeChat"))
        assertEquals("QQYY", PinyinEngine.getPinyinInitials("QQ音乐"))
    }

    @Test
    fun testFullPinyinExtractionAndPolyphones() {
        assertEquals("weixin", PinyinEngine.getFullPinyin("微信"))
        assertEquals("zhifubao", PinyinEngine.getFullPinyin("支付宝"))
        assertEquals("chrome", PinyinEngine.getFullPinyin("Chrome"))
        assertEquals("wangyiyunyinyue", PinyinEngine.getFullPinyin("网易云音乐"))
        assertEquals("zhaoshangyinhang", PinyinEngine.getFullPinyin("招商银行"))

        val musicIndex = PinyinEngine.buildIndex("网易云音乐")
        assertTrue(musicIndex.initialVariants.contains("wyyyy"))
        assertTrue(musicIndex.initialVariants.contains("wyyyl"))
        assertTrue(musicIndex.fullPinyinVariants.contains("wangyiyunyinyue"))
        assertTrue(musicIndex.fullPinyinVariants.contains("wangyiyunyinle"))
    }

    @Test
    fun testIsChineseRange() {
        assertTrue(PinyinEngine.isChinese('微'))
        assertTrue(PinyinEngine.isChinese('信'))
        assertTrue(PinyinEngine.isChinese('魅'))
        assertTrue(PinyinEngine.isChinese('麒'))
        assertFalse(PinyinEngine.isChinese('A'))
        assertFalse(PinyinEngine.isChinese('1'))
        assertFalse(PinyinEngine.isChinese('@'))
    }
}
