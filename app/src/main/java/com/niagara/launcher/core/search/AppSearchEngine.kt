package com.niagara.launcher.core.search

import com.niagara.launcher.core.model.AppItem
import com.niagara.launcher.core.pinyin.PinyinEngine
import com.niagara.launcher.core.pinyin.PinyinIndex

/**
 * Deterministic, relevance-ranked application search engine supporting:
 * - Direct label exact/prefix/substring matching
 * - Polyphone-aware Pinyin initials & English word/CamelCase initials ("wx", "gc", "yt", "qqyy")
 * - Syllable-boundary full Pinyin & mixed Chinese-English matching ("weix", "yinyue", "wx读书")
 * - Package name fallback matching
 */
object AppSearchEngine {

    private const val SCORE_LABEL_EXACT = 1000
    private const val SCORE_LABEL_PREFIX = 850
    private const val SCORE_INITIALS_EXACT = 750
    private const val SCORE_INITIALS_PREFIX = 650
    private const val SCORE_SYLLABLE_PREFIX = 550
    private const val SCORE_LABEL_CONTAINS = 400
    private const val SCORE_PINYIN_OR_PACKAGE_CONTAINS = 250
    private const val BONUS_FAVORITE = 25

    fun scoreMatch(app: AppItem, rawQuery: String): Int {
        val cleanQuery = rawQuery.trim().lowercase()
        if (cleanQuery.isEmpty()) return 1

        val labelLower = app.label.trim().lowercase()
        val compactQuery = cleanQuery.replace(" ", "")
        val index = resolveIndex(app)

        var baseScore = 0

        // 1. Exact label match
        if (labelLower == cleanQuery || labelLower.replace(" ", "") == compactQuery) {
            baseScore = maxOf(baseScore, SCORE_LABEL_EXACT)
        }

        // 2. Label prefix match (e.g. "微" -> "微信")
        if (labelLower.startsWith(cleanQuery) ||
            (compactQuery.isNotEmpty() && labelLower.replace(" ", "").startsWith(compactQuery))
        ) {
            baseScore = maxOf(baseScore, SCORE_LABEL_PREFIX)
        }

        // 3 & 4. Pinyin initials or English word/CamelCase initials exact / prefix match
        if (compactQuery.isNotEmpty()) {
            val initialsList = index.initialVariants.ifEmpty {
                listOf(app.pinyinInitials.lowercase()).filter { it.isNotEmpty() }
            }
            for (initials in initialsList) {
                if (initials == compactQuery) {
                    baseScore = maxOf(baseScore, SCORE_INITIALS_EXACT)
                } else if (initials.startsWith(compactQuery)) {
                    baseScore = maxOf(baseScore, SCORE_INITIALS_PREFIX)
                }
            }
        }

        // 5. Syllable-boundary full Pinyin / mixed Chinese-English prefix match
        if (compactQuery.isNotEmpty()) {
            if (matchesSyllableBoundary(index, compactQuery) || matchesMixedQuery(app.label, compactQuery)) {
                baseScore = maxOf(baseScore, SCORE_SYLLABLE_PREFIX)
            }
        }

        // 6. Direct label substring match (e.g. "地图" -> "高德地图")
        if (labelLower.contains(cleanQuery) ||
            (compactQuery.isNotEmpty() && labelLower.replace(" ", "").contains(compactQuery))
        ) {
            baseScore = maxOf(baseScore, SCORE_LABEL_CONTAINS)
        }

        // 7. Full Pinyin substring, initials substring, or package name contains
        if (compactQuery.isNotEmpty()) {
            val fullVariants = index.fullPinyinVariants.ifEmpty {
                listOf(app.fullPinyin.lowercase()).filter { it.isNotEmpty() }
            }
            val initialsList = index.initialVariants.ifEmpty {
                listOf(app.pinyinInitials.lowercase()).filter { it.isNotEmpty() }
            }
            if (fullVariants.any { it.contains(compactQuery) } ||
                initialsList.any { it.contains(compactQuery) } ||
                app.packageName.lowercase().contains(compactQuery)
            ) {
                baseScore = maxOf(baseScore, SCORE_PINYIN_OR_PACKAGE_CONTAINS)
            }
        }

        if (baseScore <= 0) return 0

        val favoriteBonus = if (app.isFavorite) BONUS_FAVORITE else 0
        val lengthBonus = (20 - app.label.length.coerceAtMost(20))
        return baseScore + favoriteBonus + lengthBonus
    }

    /**
     * Filters and sorts [apps] in descending order of relevance for [query].
     */
    fun search(apps: List<AppItem>, query: String): List<AppItem> {
        if (query.isBlank()) return emptyList()
        return apps.mapNotNull { app ->
            val score = scoreMatch(app, query)
            if (score > 0) app to score else null
        }
            .sortedWith(
                compareByDescending<Pair<AppItem, Int>> { it.second }
                    .thenBy { it.first.label.lowercase() }
            )
            .map { it.first }
    }

    private fun resolveIndex(app: AppItem): PinyinIndex {
        return if (app.pinyinIndex.tokenSyllables.isNotEmpty()) {
            app.pinyinIndex
        } else if (app.pinyinInitials.isNotEmpty() || app.fullPinyin.isNotEmpty()) {
            val computed = PinyinEngine.buildIndex(app.label)
            if (computed.tokenSyllables.isNotEmpty()) {
                computed
            } else {
                PinyinIndex(
                    firstLetter = app.firstLetter,
                    primaryInitials = app.pinyinInitials,
                    primaryFullPinyin = app.fullPinyin,
                    initialVariants = listOf(app.pinyinInitials.lowercase()).filter { it.isNotEmpty() },
                    fullPinyinVariants = listOf(app.fullPinyin.lowercase()).filter { it.isNotEmpty() }
                )
            }
        } else {
            PinyinEngine.buildIndex(app.label)
        }
    }

    private fun matchesSyllableBoundary(index: PinyinIndex, compactQuery: String): Boolean {
        val tokens = index.tokenSyllables
        if (tokens.isEmpty() || compactQuery.isEmpty()) return false

        for (startToken in tokens.indices) {
            if (matchFromToken(tokens, startToken, compactQuery, 0)) {
                return true
            }
        }
        return false
    }

    private fun matchFromToken(
        tokens: List<List<String>>,
        tokenIdx: Int,
        query: String,
        queryOffset: Int
    ): Boolean {
        if (queryOffset >= query.length) return true
        if (tokenIdx >= tokens.size) return false

        val remainingQuery = query.substring(queryOffset)
        val readings = tokens[tokenIdx]

        for (reading in readings) {
            if (reading.isEmpty()) continue

            // Option A: consume full syllable and advance to next token
            if (remainingQuery.startsWith(reading)) {
                if (matchFromToken(tokens, tokenIdx + 1, query, queryOffset + reading.length)) {
                    return true
                }
            }

            // Option B: remaining query is a prefix of this syllable (finishes query on this token)
            if (reading.startsWith(remainingQuery)) {
                return true
            }

            // Option C: consume initial character of this syllable and advance to next token
            if (remainingQuery[0] == reading[0]) {
                if (matchFromToken(tokens, tokenIdx + 1, query, queryOffset + 1)) {
                    return true
                }
            }
        }

        return false
    }

    private fun matchesMixedQuery(label: String, compactQuery: String): Boolean {
        val hasChinese = compactQuery.any { PinyinEngine.isChinese(it) }
        val hasLatin = compactQuery.any { it in 'a'..'z' }
        if (!hasChinese || !hasLatin) return false

        val charTokens: List<List<String>> = label.mapNotNull { ch ->
            when {
                PinyinEngine.isChinese(ch) -> {
                    (listOf(ch.toString()) + PinyinEngine.getCharPinyins(ch)).distinct()
                }
                ch.isLetterOrDigit() -> listOf(ch.lowercaseChar().toString())
                else -> null
            }
        }
        if (charTokens.isEmpty()) return false

        for (start in charTokens.indices) {
            if (matchFromToken(charTokens, start, compactQuery, 0)) {
                return true
            }
        }
        return false
    }
}
