package com.niagara.launcher

import com.niagara.launcher.core.pinyin.PinyinEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinyinEngineTest {

    @Test
    fun testFirstLetterExtraction() {
        assertEquals('W', PinyinEngine.getFirstLetter("微信"))
        assertEquals('Z', PinyinEngine.getFirstLetter("支付宝"))
        assertEquals('C', PinyinEngine.getFirstLetter("Chrome"))
        assertEquals('Y', PinyinEngine.getFirstLetter("YouTube"))
        assertEquals('#', PinyinEngine.getFirstLetter("12306"))
        assertEquals('#', PinyinEngine.getFirstLetter("@App"))
    }

    @Test
    fun testPinyinInitialsExtraction() {
        assertEquals("WX", PinyinEngine.getPinyinInitials("微信"))
        assertEquals("ZFB", PinyinEngine.getPinyinInitials("支付宝"))
        assertEquals("MT", PinyinEngine.getPinyinInitials("美团"))
        assertEquals("SETTING", PinyinEngine.getPinyinInitials("Setting"))
    }

    @Test
    fun testFullPinyinExtraction() {
        assertEquals("weixin", PinyinEngine.getFullPinyin("微信"))
        assertEquals("zhifubao", PinyinEngine.getFullPinyin("支付宝"))
        assertEquals("chrome", PinyinEngine.getFullPinyin("Chrome"))
    }

    @Test
    fun testIsChinese() {
        assertTrue(PinyinEngine.isChinese('微'))
        assertTrue(PinyinEngine.isChinese('信'))
        assertTrue(!PinyinEngine.isChinese('A'))
        assertTrue(!PinyinEngine.isChinese('1'))
    }
}
