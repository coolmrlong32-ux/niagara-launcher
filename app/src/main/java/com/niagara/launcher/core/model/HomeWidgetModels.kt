package com.niagara.launcher.core.model

import androidx.compose.runtime.Immutable

/**
 * Built-in flagship widgets shipped with the launcher.
 */
enum class BuiltInWidgetKind(
    val storageKey: String,
    val title: String,
    val subtitle: String,
    val defaultHeightDp: Int
) {
    WEATHER(
        storageKey = "weather",
        title = "实时天气",
        subtitle = "Open-Meteo 本地缓存实况天气",
        defaultHeightDp = 96
    ),
    DEVICE_STATUS(
        storageKey = "device_status",
        title = "设备状态",
        subtitle = "内存、电池温度与电量监控",
        defaultHeightDp = 92
    ),
    QUICK_SEARCH(
        storageKey = "quick_search",
        title = "快捷搜索",
        subtitle = "单手一键搜索本地应用与网页",
        defaultHeightDp = 68
    ),
    DIGITAL_CLOCK(
        storageKey = "digital_clock",
        title = "极简时钟卡片",
        subtitle = "大字号毛玻璃时间与秒数显示",
        defaultHeightDp = 132
    );

    companion object {
        fun fromStorageKey(raw: String?): BuiltInWidgetKind? {
            return entries.firstOrNull { it.storageKey == raw }
        }
    }
}

/**
 * Quick height presets available when resizing home widgets in edit mode.
 */
enum class WidgetSizePreset(
    val label: String,
    val heightDp: Int
) {
    COMPACT("紧凑", 104),
    MEDIUM("标准", 164),
    LARGE("大尺寸", 232)
}

/**
 * Represents a widget card placed on Page 0 of the launcher (either built-in or an Android AppWidget).
 */
@Immutable
sealed interface HomeWidgetItem {
    val id: String
    val heightDp: Int

    @Immutable
    data class BuiltIn(
        override val id: String,
        val kind: BuiltInWidgetKind,
        override val heightDp: Int = kind.defaultHeightDp
    ) : HomeWidgetItem

    @Immutable
    data class SystemAppWidget(
        override val id: String,
        val appWidgetId: Int,
        val providerPackage: String,
        val providerClass: String,
        val label: String,
        override val heightDp: Int = 164
    ) : HomeWidgetItem
}

/**
 * Reorderable sections on the Home screen (Page 0).
 */
enum class HomeSectionType(
    val storageKey: String,
    val displayName: String
) {
    WIDGETS("widgets", "桌面插件"),
    FAVORITES("favorites", "常用应用"),
    DESKTOP_APPS("desktop_apps", "桌面应用");

    companion object {
        fun fromStorageKey(raw: String?): HomeSectionType? {
            return entries.firstOrNull { it.storageKey == raw }
        }
    }
}

/**
 * Pure Kotlin codec and ordered manipulation helpers for home widgets.
 */
object HomeWidgetCodec {
    const val MIN_WIDGET_HEIGHT_DP = 64
    const val MAX_WIDGET_HEIGHT_DP = 360
    const val RESIZE_STEP_DP = 28
    const val MAX_HOME_WIDGETS = 8

    private const val ENTRY_DELIMITER = ";"
    private const val FIELD_DELIMITER = "|"
    private const val TYPE_BUILTIN = "builtin"
    private const val TYPE_SYSTEM = "system"

    fun clampHeightDp(heightDp: Int): Int {
        return heightDp.coerceIn(MIN_WIDGET_HEIGHT_DP, MAX_WIDGET_HEIGHT_DP)
    }

    fun encode(items: List<HomeWidgetItem>): String {
        return items.asSequence()
            .distinctBy { it.id }
            .take(MAX_HOME_WIDGETS)
            .map { item ->
                val clampedHeight = clampHeightDp(item.heightDp)
                when (item) {
                    is HomeWidgetItem.BuiltIn -> listOf(
                        TYPE_BUILTIN,
                        escapeField(item.id),
                        item.kind.storageKey,
                        clampedHeight.toString()
                    ).joinToString(FIELD_DELIMITER)

                    is HomeWidgetItem.SystemAppWidget -> listOf(
                        TYPE_SYSTEM,
                        escapeField(item.id),
                        item.appWidgetId.toString(),
                        escapeField(item.providerPackage),
                        escapeField(item.providerClass),
                        escapeField(item.label),
                        clampedHeight.toString()
                    ).joinToString(FIELD_DELIMITER)
                }
            }
            .joinToString(ENTRY_DELIMITER)
    }

    fun decode(raw: String?): List<HomeWidgetItem> {
        if (raw.isNullOrBlank()) return emptyList()
        val result = ArrayList<HomeWidgetItem>()
        val seenIds = HashSet<String>()
        for (token in raw.split(ENTRY_DELIMITER)) {
            val trimmed = token.trim()
            if (trimmed.isEmpty()) continue
            val parts = trimmed.split(FIELD_DELIMITER)
            val parsed: HomeWidgetItem? = when (parts.firstOrNull()) {
                TYPE_BUILTIN -> {
                    if (parts.size < 4) null
                    else {
                        val id = unescapeField(parts[1]).takeIf { it.isNotBlank() }
                        val kind = BuiltInWidgetKind.fromStorageKey(parts[2])
                        val height = parts[3].toIntOrNull()
                        if (id != null && kind != null) {
                            HomeWidgetItem.BuiltIn(
                                id = id,
                                kind = kind,
                                heightDp = clampHeightDp(height ?: kind.defaultHeightDp)
                            )
                        } else null
                    }
                }

                TYPE_SYSTEM -> {
                    if (parts.size < 7) null
                    else {
                        val id = unescapeField(parts[1]).takeIf { it.isNotBlank() }
                        val appWidgetId = parts[2].toIntOrNull()
                        val pkg = unescapeField(parts[3]).takeIf { it.isNotBlank() }
                        val cls = unescapeField(parts[4]).takeIf { it.isNotBlank() }
                        val label = unescapeField(parts[5])
                        val height = parts[6].toIntOrNull()
                        if (id != null && appWidgetId != null && appWidgetId >= 0 && pkg != null && cls != null) {
                            HomeWidgetItem.SystemAppWidget(
                                id = id,
                                appWidgetId = appWidgetId,
                                providerPackage = pkg,
                                providerClass = cls,
                                label = label.ifBlank { pkg },
                                heightDp = clampHeightDp(height ?: 164)
                            )
                        } else null
                    }
                }

                else -> null
            }
            if (parsed != null && seenIds.add(parsed.id)) {
                result.add(parsed)
                if (result.size >= MAX_HOME_WIDGETS) break
            }
        }
        return result
    }

    fun add(current: List<HomeWidgetItem>, item: HomeWidgetItem): Pair<List<HomeWidgetItem>, Boolean> {
        if (current.any { it.id == item.id }) return current to true
        if (current.size >= MAX_HOME_WIDGETS) return current to false
        val normalized = when (item) {
            is HomeWidgetItem.BuiltIn -> item.copy(heightDp = clampHeightDp(item.heightDp))
            is HomeWidgetItem.SystemAppWidget -> item.copy(heightDp = clampHeightDp(item.heightDp))
        }
        return (current + normalized) to true
    }

    fun remove(current: List<HomeWidgetItem>, id: String): List<HomeWidgetItem> {
        return current.filterNot { it.id == id }
    }

    fun move(current: List<HomeWidgetItem>, id: String, delta: Int): List<HomeWidgetItem> {
        if (delta == 0 || current.size <= 1) return current
        val index = current.indexOfFirst { it.id == id }
        if (index < 0) return current
        val targetIndex = (index + delta).coerceIn(0, current.lastIndex)
        if (targetIndex == index) return current
        val mutable = current.toMutableList()
        val moved = mutable.removeAt(index)
        mutable.add(targetIndex, moved)
        return mutable
    }

    fun reorder(current: List<HomeWidgetItem>, orderedIds: List<String>): List<HomeWidgetItem> {
        if (current.size <= 1 || orderedIds.isEmpty()) return current
        val map = current.associateBy { it.id }
        val result = ArrayList<HomeWidgetItem>(current.size)
        val used = HashSet<String>()
        for (id in orderedIds) {
            val item = map[id]
            if (item != null && used.add(id)) {
                result.add(item)
            }
        }
        for (item in current) {
            if (used.add(item.id)) {
                result.add(item)
            }
        }
        return result
    }

    fun resize(current: List<HomeWidgetItem>, id: String, heightDp: Int): List<HomeWidgetItem> {
        val clamped = clampHeightDp(heightDp)
        return current.map { item ->
            if (item.id != id) item
            else when (item) {
                is HomeWidgetItem.BuiltIn -> item.copy(heightDp = clamped)
                is HomeWidgetItem.SystemAppWidget -> item.copy(heightDp = clamped)
            }
        }
    }

    private fun escapeField(value: String): String {
        val sb = StringBuilder(value.length)
        for (ch in value) {
            when (ch) {
                '%' -> sb.append("%25")
                ';' -> sb.append("%3B")
                '|' -> sb.append("%7C")
                '\n' -> sb.append("%0A")
                '\r' -> sb.append("%0D")
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun unescapeField(value: String): String {
        if (!value.contains('%')) return value
        val sb = StringBuilder(value.length)
        var i = 0
        while (i < value.length) {
            val ch = value[i]
            if (ch == '%' && i + 2 < value.length) {
                when (value.substring(i, i + 3).uppercase()) {
                    "%25" -> { sb.append('%'); i += 3; continue }
                    "%3B" -> { sb.append(';'); i += 3; continue }
                    "%7C" -> { sb.append('|'); i += 3; continue }
                    "%0A" -> { sb.append('\n'); i += 3; continue }
                    "%0D" -> { sb.append('\r'); i += 3; continue }
                }
            }
            sb.append(ch)
            i++
        }
        return sb.toString()
    }
}

/**
 * Pure Kotlin codec and manipulation helpers for ordering sections on Page 0.
 */
object HomeSectionOrderCodec {
    val DEFAULT_ORDER: List<HomeSectionType> = listOf(
        HomeSectionType.WIDGETS,
        HomeSectionType.FAVORITES,
        HomeSectionType.DESKTOP_APPS
    )

    private const val DELIMITER = ";"

    fun encode(order: List<HomeSectionType>): String {
        return normalize(order).joinToString(DELIMITER) { it.storageKey }
    }

    fun decode(raw: String?): List<HomeSectionType> {
        if (raw.isNullOrBlank()) return DEFAULT_ORDER
        val parsed = raw.split(DELIMITER)
            .mapNotNull { HomeSectionType.fromStorageKey(it.trim()) }
        return normalize(parsed)
    }

    fun move(order: List<HomeSectionType>, section: HomeSectionType, delta: Int): List<HomeSectionType> {
        val normalized = normalize(order)
        if (delta == 0) return normalized
        val index = normalized.indexOf(section)
        if (index < 0) return normalized
        val targetIndex = (index + delta).coerceIn(0, normalized.lastIndex)
        if (targetIndex == index) return normalized
        val mutable = normalized.toMutableList()
        val moved = mutable.removeAt(index)
        mutable.add(targetIndex, moved)
        return mutable
    }

    private fun normalize(order: List<HomeSectionType>): List<HomeSectionType> {
        val result = ArrayList<HomeSectionType>(DEFAULT_ORDER.size)
        for (item in order) {
            if (!result.contains(item)) {
                result.add(item)
            }
        }
        for (fallback in DEFAULT_ORDER) {
            if (!result.contains(fallback)) {
                result.add(fallback)
            }
        }
        return result
    }
}
