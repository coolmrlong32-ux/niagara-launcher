package com.niagara.launcher.core.pinyin

import androidx.compose.runtime.Immutable
import net.sourceforge.pinyin4j.PinyinHelper
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType
import java.util.concurrent.ConcurrentHashMap

/**
 * Structured Pinyin and word-token index for fast, polyphone-aware matching.
 */
@Immutable
data class PinyinIndex(
    val firstLetter: Char = '#',
    val primaryInitials: String = "",
    val primaryFullPinyin: String = "",
    val initialVariants: List<String> = emptyList(),
    val fullPinyinVariants: List<String> = emptyList(),
    val tokenSyllables: List<List<String>> = emptyList()
)

/**
 * Full-coverage Pinyin engine backed by pinyin4j with app-domain polyphone priority overrides,
 * English word/CamelCase tokenization, and per-character concurrent caching.
 */
object PinyinEngine {

    private val outputFormat = HanyuPinyinOutputFormat().apply {
        caseType = HanyuPinyinCaseType.LOWERCASE
        toneType = HanyuPinyinToneType.WITHOUT_TONE
        vCharType = HanyuPinyinVCharType.WITH_V
    }

    private val charCache = ConcurrentHashMap<Char, List<String>>()

    // Prioritized readings for common polyphones in Chinese app names
    private val POLYPHONE_PRIORITY: Map<Char, List<String>> = mapOf(
        '乐' to listOf("yue", "le"),
        '行' to listOf("hang", "xing"),
        '重' to listOf("chong", "zhong"),
        '长' to listOf("chang", "zhang"),
        '调' to listOf("tiao", "diao"),
        '单' to listOf("dan", "shan"),
        '藏' to listOf("cang", "zang"),
        '厦' to listOf("xia", "sha"),
        '校' to listOf("xiao", "jiao"),
        '传' to listOf("chuan", "zhuan"),
        '率' to listOf("lv", "shuai"),
        '省' to listOf("sheng", "xing"),
        '秘' to listOf("mi", "bi"),
        '宿' to listOf("su", "xiu"),
        '都' to listOf("du", "dou"),
        '奇' to listOf("qi", "ji"),
        '朝' to listOf("chao", "zhao"),
        '数' to listOf("shu", "shuo"),
        '角' to listOf("jiao", "jue"),
        '绿' to listOf("lv", "lu")
    )

    /**
     * Determines whether the character is a CJK Unified Ideograph.
     */
    fun isChinese(ch: Char): Boolean {
        return ch in '\u4E00'..'\u9FFF'
    }

    /**
     * Returns all distinct, tone-free lowercase Pinyin readings for a single Chinese character,
     * ordered with common app-name pronunciations first.
     */
    fun getCharPinyins(ch: Char): List<String> {
        if (!isChinese(ch)) return emptyList()
        return charCache.computeIfAbsent(ch) { target ->
            val rawReadings = try {
                PinyinHelper.toHanyuPinyinStringArray(target, outputFormat)
                    ?.map { it.trim().lowercase() }
                    ?.filter { it.isNotEmpty() }
                    ?.distinct()
                    .orEmpty()
            } catch (_: Exception) {
                emptyList()
            }

            val priority = POLYPHONE_PRIORITY[target]
            when {
                rawReadings.isEmpty() && !priority.isNullOrEmpty() -> priority
                rawReadings.isEmpty() -> emptyList()
                priority.isNullOrEmpty() -> rawReadings
                else -> {
                    val ordered = LinkedHashSet<String>()
                    for (preferred in priority) {
                        if (rawReadings.contains(preferred) || preferred == "lv") {
                            ordered.add(preferred)
                        }
                    }
                    ordered.addAll(rawReadings)
                    ordered.toList()
                }
            }
        }
    }

    /**
     * Builds a comprehensive [PinyinIndex] for [text], handling Chinese polyphones,
     * multi-word English labels ("Google Chrome" -> "gc"), CamelCase ("YouTube" -> "yt"),
     * and mixed labels ("QQ音乐" -> "qqyy" / "qqyl").
     */
    fun buildIndex(text: String): PinyinIndex {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return PinyinIndex()
        }

        val firstLetter = computeFirstLetter(trimmed)
        val tokenSyllables = tokenize(trimmed)
        if (tokenSyllables.isEmpty()) {
            return PinyinIndex(firstLetter = firstLetter)
        }

        val primaryInitials = buildString {
            for (variants in tokenSyllables) {
                val primary = variants.firstOrNull() ?: continue
                if (primary.isNotEmpty()) {
                    append(primary[0].uppercaseChar())
                }
            }
        }

        val primaryFullPinyin = buildString {
            for (variants in tokenSyllables) {
                val primary = variants.firstOrNull() ?: continue
                append(primary.lowercase())
            }
        }

        // Expand bounded Cartesian combinations for polyphones (capped at 16 variants)
        val initialVariants = expandVariants(
            tokenSyllables = tokenSyllables,
            transform = { syllable -> syllable.firstOrNull()?.lowercaseChar()?.toString().orEmpty() },
            maxVariants = 16
        )

        val fullPinyinVariants = expandVariants(
            tokenSyllables = tokenSyllables,
            transform = { syllable -> syllable.lowercase() },
            maxVariants = 16
        )

        return PinyinIndex(
            firstLetter = firstLetter,
            primaryInitials = primaryInitials,
            primaryFullPinyin = primaryFullPinyin,
            initialVariants = initialVariants,
            fullPinyinVariants = fullPinyinVariants,
            tokenSyllables = tokenSyllables
        )
    }

    /**
     * Returns the capitalized initial letter for A-Z drawer grouping.
     */
    fun getFirstLetter(text: String): Char = computeFirstLetter(text.trim())

    /**
     * Extracts uppercase initials from a label:
     * - "微信" -> "WX"
     * - "Google Chrome" -> "GC"
     * - "YouTube" -> "YT"
     * - "QQ音乐" -> "QQYY"
     */
    fun getPinyinInitials(text: String): String = buildIndex(text).primaryInitials

    /**
     * Converts a label into concatenated lowercase Pinyin / normalized tokens without spaces.
     */
    fun getFullPinyin(text: String): String = buildIndex(text).primaryFullPinyin

    private fun computeFirstLetter(trimmed: String): Char {
        if (trimmed.isEmpty()) return '#'
        val firstChar = trimmed[0]
        if (firstChar in 'a'..'z') return firstChar.uppercaseChar()
        if (firstChar in 'A'..'Z') return firstChar
        if (isChinese(firstChar)) {
            val primary = getCharPinyins(firstChar).firstOrNull()
            val initial = primary?.firstOrNull()?.uppercaseChar()
            if (initial != null && initial in 'A'..'Z') {
                return initial
            }
        }
        return '#'
    }

    /**
     * Splits [text] into search tokens:
     * - Each Chinese character becomes a syllable variant list (e.g. '乐' -> ["yue", "le"]).
     * - Contiguous ASCII letters split on whitespace/separators and CamelCase boundaries
     *   (e.g. "Google Chrome" -> ["google"], ["chrome"]; "YouTube" -> ["you"], ["tube"];
     *    all-caps acronyms like "QQ" stay together as ["qq"]).
     * - Contiguous digits stay as a single numeric token (e.g. "12306").
     */
    private fun tokenize(text: String): List<List<String>> {
        val tokens = mutableListOf<List<String>>()
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when {
                isChinese(ch) -> {
                    val readings = getCharPinyins(ch)
                    if (readings.isNotEmpty()) {
                        tokens.add(readings)
                    }
                    i++
                }
                ch.isAsciiLetter() -> {
                    val start = i
                    while (i < text.length && text[i].isAsciiLetter()) {
                        i++
                    }
                    val word = text.substring(start, i)
                    splitCamelCaseWord(word).forEach { subWord ->
                        tokens.add(listOf(subWord.lowercase()))
                    }
                }
                ch in '0'..'9' -> {
                    val start = i
                    while (i < text.length && text[i] in '0'..'9') {
                        i++
                    }
                    tokens.add(listOf(text.substring(start, i)))
                }
                else -> {
                    i++
                }
            }
        }
        return tokens
    }

    private fun splitCamelCaseWord(word: String): List<String> {
        if (word.length <= 1) return listOf(word)
        // Keep pure uppercase acronyms like "QQ", "WPS", "PDF" intact
        if (word.all { it in 'A'..'Z' }) {
            return listOf(word)
        }
        val parts = mutableListOf<String>()
        var partStart = 0
        for (idx in 1 until word.length) {
            val prev = word[idx - 1]
            val curr = word[idx]
            val nextIsLower = idx + 1 < word.length && word[idx + 1] in 'a'..'z'
            val isBoundary = (prev in 'a'..'z' && curr in 'A'..'Z') ||
                (prev in 'A'..'Z' && curr in 'A'..'Z' && nextIsLower)
            if (isBoundary) {
                parts.add(word.substring(partStart, idx))
                partStart = idx
            }
        }
        parts.add(word.substring(partStart))
        return parts
    }

    private fun expandVariants(
        tokenSyllables: List<List<String>>,
        transform: (String) -> String,
        maxVariants: Int
    ): List<String> {
        var current = listOf("")
        for (syllableOptions in tokenSyllables) {
            val pieces = syllableOptions.map(transform).filter { it.isNotEmpty() }.distinct()
            if (pieces.isEmpty()) continue
            val next = LinkedHashSet<String>()
            for (prefix in current) {
                for (piece in pieces) {
                    next.add(prefix + piece)
                    if (next.size >= maxVariants) break
                }
                if (next.size >= maxVariants) break
            }
            current = next.toList()
        }
        return current.filter { it.isNotEmpty() }.distinct()
    }

    private fun Char.isAsciiLetter(): Boolean = (this in 'a'..'z') || (this in 'A'..'Z')
}
