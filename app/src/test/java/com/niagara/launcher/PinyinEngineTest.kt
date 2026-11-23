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
        assertEquals('C', PinyinEngine.getFirstLetter("Chrome"))
        assertEquals('Y', PinyinEngine.getFirstLetter("YouTube"))
        assertEquals('#', PinyinEngine.getFirstLetter("12306"))
        assertEquals('#', PinyinEngine.getFirstLetter("@App"))
        assertEquals('#', PinyinEngine.getFirstLetter(""))
        assertEquals('#', PinyinEngine.getFirstLetter("   "))
    }

    @Test
    fun testRareAndTraditionalCharacters() {
        assertEquals('M', PinyinEngine.getFirstLetter("魅族"))
        assertEquals("MZ", PinyinEngine.getPinyinInitials("魅族"))
        assertEquals("meizu", PinyinEngine.getFullPinyin("魅族"))

        assertEquals('Q', PinyinEngine.getFirstLetter("麒麟"))
        assertEquals("QL", PinyinEngine.getPinyinInitials("麒麟"))
        assertEquals("qilin", PinyinEngine.getFullPinyin("麒麟"))

        assertEquals('X', PinyinEngine.getFirstLetter("鑫"))
        assertEquals("xin", PinyinEngine.getFullPinyin("鑫"))

        assertEquals('S', PinyinEngine.getFirstLetter("設定"))
        assertEquals("SD", PinyinEngine.getPinyinInitials("設定"))
    }

    @Test
    fun testPolyphonePriorityAndVariants() {
        val musicIndex = PinyinEngine.buildIndex("网易云音乐")
        assertEquals('W', musicIndex.firstLetter)
        assertEquals("WYYYY", musicIndex.primaryInitials)
        assertEquals("wangyiyunyinyue", musicIndex.primaryFullPinyin)
        assertTrue(musicIndex.initialVariants.contains("wyyyy"))
        assertTrue(musicIndex.initialVariants.contains("wyyyl"))
        assertTrue(musicIndex.fullPinyinVariants.contains("wangyiyunyinyue"))
        assertTrue(musicIndex.fullPinyinVariants.contains("wangyiyunyinle"))

        val bankIndex = PinyinEngine.buildIndex("招商银行")
        assertEquals("ZSYH", bankIndex.primaryInitials)
        assertEquals("zhaoshangyinhang", bankIndex.primaryFullPinyin)
        assertTrue(bankIndex.initialVariants.contains("zsyx"))
        assertTrue(bankIndex.fullPinyinVariants.contains("zhaoshangyinxing"))

        val chongqingIndex = PinyinEngine.buildIndex("重庆")
        assertEquals('C', chongqingIndex.firstLetter)
        assertEquals("CQ", chongqingIndex.primaryInitials)
        assertEquals("chongqing", chongqingIndex.primaryFullPinyin)
        assertTrue(chongqingIndex.initialVariants.contains("zq"))
    }

    @Test
    fun testEnglishMultiWordCamelCaseAndMixedLabels() {
        assertEquals("GC", PinyinEngine.getPinyinInitials("Google Chrome"))
        assertEquals("googlechrome", PinyinEngine.getFullPinyin("Google Chrome"))

        assertEquals("YT", PinyinEngine.getPinyinInitials("YouTube"))
        assertEquals("youtube", PinyinEngine.getFullPinyin("YouTube"))

        assertEquals("WC", PinyinEngine.getPinyinInitials("WeChat"))
        assertEquals("wechat", PinyinEngine.getFullPinyin("WeChat"))

        // "QQ" is kept as a single acronym token -> primary initials "QYY" / "QYL", full variants include "qqyy" & "qqyinyue"
        val qqMusic = PinyinEngine.buildIndex("QQ音乐")
        assertEquals('Q', qqMusic.firstLetter)
        assertEquals("QYY", qqMusic.primaryInitials)
        assertTrue(qqMusic.initialVariants.contains("qyy"))
        assertTrue(qqMusic.initialVariants.contains("qyl"))
        assertTrue(qqMusic.fullPinyinVariants.contains("qqyinyue"))
        assertTrue(qqMusic.fullPinyinVariants.contains("qqyinle"))
    }

    @Test
    fun testIsChineseRange() {
        assertTrue(PinyinEngine.isChinese('微'))
        assertTrue(PinyinEngine.isChinese('信'))
        assertTrue(PinyinEngine.isChinese('魅'))
        assertFalse(PinyinEngine.isChinese('A'))
        assertFalse(PinyinEngine.isChinese('1'))
    }
}
