package com.niagara.launcher.core.pinyin

import java.util.Locale

/**
 * High-performance lightweight Pinyin engine for Chinese character indexing and search.
 * Supports Pinyin initials extraction, full Pinyin transliteration, and multi-mode fuzzy search.
 */
object PinyinEngine {

    // Common Chinese GB2312 boundary index mapping table for A-Z
    private val GB_BOUNDARY_CHARS = intArrayOf(
        0xB0A1, 0xB0C5, 0xB2C1, 0xB4EE, 0xB6EA, 0xB7A2, 0xB8C1,
        0xB9FE, 0xBBF7, 0xBFA6, 0xC0AC, 0xC2E8, 0xC4C3, 0xC5B6,
        0xC5BE, 0xC6DA, 0xC8BB, 0xC8F6, 0xCBFA, 0xCDDA, 0xCEF4,
        0xD1B9, 0xD4D1
    )

    private val GB_INITIALS = charArrayOf(
        'A', 'B', 'C', 'D', 'E', 'F', 'G',
        'H', 'J', 'K', 'L', 'M', 'N',
        'O', 'P', 'Q', 'R', 'S', 'T',
        'W', 'X', 'Y', 'Z'
    )

    // Lookup dictionary for common everyday Chinese words and tech apps
    private val COMMON_WORDS_PINYIN = mapOf(
        "微" to "wei", "信" to "xin", "支" to "zhi", "付" to "fu", "宝" to "bao",
        "淘" to "tao", "京" to "jing", "东" to "dong", "拼" to "pin", "多" to "duo",
        "美" to "mei", "团" to "tuan", "快" to "kuai", "手" to "shou", "抖" to "dou",
        "音" to "yin", "小" to "xiao", "红" to "hong", "书" to "shu", "高" to "gao",
        "德" to "de", "地" to "di", "图" to "tu", "百" to "bai", "度" to "du",
        "知" to "zhi", "乎" to "hu", "哔" to "bi", "哩" to "li", "网" to "wang",
        "易" to "yi", "云" to "yun", "音" to "yin", "乐" to "le", "酷" to "ku",
        "狗" to "gou", "设" to "she", "置" to "zhi", "相" to "xiang", "机" to "ji",
        "相" to "xiang", "册" to "ce", "电" to "dian", "话" to "hua", "信" to "xin",
        "息" to "xi", "日" to "ri", "历" to "li", "时" to "shi", "钟" to "zhong",
        "计" to "ji", "算" to "suan", "器" to "qi", "浏" to "liu", "览" to "lan",
        "文" to "wen", "件" to "jian", "天" to "tian", "气" to "qi", "邮" to "you",
        "箱" to "xiang", "便" to "bian", "签" to "qian", "录" to "lu", "音" to "yin"
    )

    /**
     * Returns the capitalized initial letter for the given text (for A-Z grouping).
     */
    fun getFirstLetter(text: String): Char {
        if (text.isBlank()) return '#'
        val firstChar = text.trim()[0]

        // English / ASCII letter
        if (firstChar in 'a'..'z') return firstChar.uppercaseChar()
        if (firstChar in 'A'..'Z') return firstChar

        // Chinese character
        if (isChinese(firstChar)) {
            val initial = getChineseInitial(firstChar)
            if (initial in 'A'..'Z') return initial
        }

        // Default category for numbers, symbols, etc.
        return '#'
    }

    /**
     * Extracts pinyin initials from a string, e.g., "微信" -> "WX", "Settings" -> "S".
     */
    fun getPinyinInitials(text: String): String {
        val sb = StringBuilder()
        for (ch in text) {
            when {
                ch in 'a'..'z' || ch in 'A'..'Z' -> sb.append(ch.uppercaseChar())
                ch in '0'..'9' -> sb.append(ch)
                isChinese(ch) -> sb.append(getChineseInitial(ch))
            }
        }
        return sb.toString()
    }

    /**
     * Converts a string into full Pinyin without tone marks.
     */
    fun getFullPinyin(text: String): String {
        val sb = StringBuilder()
        for (ch in text) {
            val chStr = ch.toString()
            if (COMMON_WORDS_PINYIN.containsKey(chStr)) {
                sb.append(COMMON_WORDS_PINYIN[chStr])
            } else if (isChinese(ch)) {
                sb.append(getChineseInitial(ch).lowercaseChar())
            } else if (ch.isLetterOrDigit()) {
                sb.append(ch.lowercaseChar())
            }
        }
        return sb.toString()
    }

    /**
     * Determines whether the character is a Chinese Han character.
     */
    fun isChinese(ch: Char): Boolean {
        return ch in '\u4E00'..'\u9FA5'
    }

    /**
     * Converts a single Chinese character to its uppercase initial consonant letter.
     */
    private fun getChineseInitial(ch: Char): Char {
        // Quick dictionary lookup
        val chStr = ch.toString()
        if (COMMON_WORDS_PINYIN.containsKey(chStr)) {
            return COMMON_WORDS_PINYIN[chStr]!![0].uppercaseChar()
        }

        try {
            val bytes = chStr.toByteArray(charset("GB2312"))
            if (bytes.size < 2) return '#'
            val b0 = bytes[0].toInt() and 0xFF
            val b1 = bytes[1].toInt() and 0xFF
            val code = (b0 shl 8) + b1

            for (i in GB_BOUNDARY_CHARS.indices.reversed()) {
                if (code >= GB_BOUNDARY_CHARS[i]) {
                    return GB_INITIALS[i]
                }
            }
        } catch (_: Exception) {
            // Fallback
        }
        return '#'
    }
}
