package com.niagara.launcher.core.data

import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import javax.xml.parsers.SAXParserFactory

/**
 * Parsed rules from a third-party Android Icon Pack's `appfilter.xml`.
 */
data class IconPackFilterData(
    val componentDrawables: Map<String, String> = emptyMap(),
    val packageFallbackDrawables: Map<String, String> = emptyMap(),
    val calendarPrefixes: Map<String, String> = emptyMap(),
    val iconBacks: List<String> = emptyList(),
    val iconMask: String? = null,
    val iconUpon: String? = null,
    val scale: Float = 1.0f
) {
    fun resolveDrawableName(
        packageName: String,
        activityName: String,
        dayOfMonth: Int? = null
    ): String? {
        val componentKey = IconPackAppFilterParser.normalizeComponentKey(packageName, activityName)
        if (dayOfMonth != null && dayOfMonth in 1..31) {
            calendarPrefixes[componentKey]?.let { prefix ->
                return "$prefix$dayOfMonth"
            }
        }
        componentDrawables[componentKey]?.let { return it }
        return packageFallbackDrawables[packageName.trim()]
    }

    val hasFallbackTemplate: Boolean
        get() = iconBacks.isNotEmpty() || iconMask != null || iconUpon != null || scale != 1.0f
}

/**
 * Pure Kotlin + SAX/XmlPullParser engine for parsing third-party Icon Pack `appfilter.xml` definitions.
 * Both `parseXml` and `parseStream` execute cleanly in pure JVM unit tests.
 */
object IconPackAppFilterParser {
    private const val MIN_SCALE = 0.5f
    private const val MAX_SCALE = 1.5f

    fun normalizeActivityName(packageName: String, activityName: String): String {
        val trimmedPkg = packageName.trim()
        val trimmedAct = activityName.trim()
        return if (trimmedAct.startsWith(".")) {
            "$trimmedPkg$trimmedAct"
        } else {
            trimmedAct
        }
    }

    fun normalizeComponent(raw: String?): Pair<String, String>? {
        if (raw.isNullOrBlank()) return null
        var inner = raw.trim()
        if (inner.startsWith("ComponentInfo{") && inner.endsWith("}")) {
            inner = inner.substring("ComponentInfo{".length, inner.length - 1).trim()
        }
        val slashIndex = inner.indexOf('/')
        if (slashIndex <= 0 || slashIndex >= inner.lastIndex) return null
        val pkg = inner.substring(0, slashIndex).trim()
        val act = inner.substring(slashIndex + 1).trim()
        if (pkg.isBlank() || act.isBlank()) return null
        return pkg to normalizeActivityName(pkg, act)
    }

    fun normalizeComponentKey(packageName: String, activityName: String): String {
        val pkg = packageName.trim()
        return "$pkg/${normalizeActivityName(pkg, activityName)}"
    }

    fun parseXml(xml: String): IconPackFilterData {
        if (xml.isBlank()) return IconPackFilterData()
        return parseStream(xml.byteInputStream(Charsets.UTF_8))
    }

    fun parseStream(inputStream: InputStream): IconPackFilterData {
        val builder = IconPackFilterBuilder()
        try {
            val factory = SAXParserFactory.newInstance().apply {
                runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
                runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
                runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            }
            val saxParser = factory.newSAXParser()
            saxParser.parse(inputStream, object : DefaultHandler() {
                override fun startElement(
                    uri: String?,
                    localName: String?,
                    qName: String?,
                    attributes: Attributes?
                ) {
                    val tagName = (qName ?: localName).orEmpty().lowercase()
                    if (attributes == null) return
                    val attrList = ArrayList<Pair<String, String>>(attributes.length)
                    for (i in 0 until attributes.length) {
                        val name = (attributes.getQName(i) ?: attributes.getLocalName(i)).orEmpty()
                        val value = attributes.getValue(i).orEmpty()
                        attrList.add(name to value)
                    }
                    builder.onStartTag(tagName, attrList)
                }
            })
        } catch (_: Exception) {
            // Return partially parsed rules safely on malformed XML
        }
        return builder.build()
    }

    fun parsePullParser(parser: XmlPullParser): IconPackFilterData {
        val builder = IconPackFilterBuilder()
        try {
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    val tagName = parser.name.orEmpty().lowercase()
                    val count = parser.attributeCount
                    val attrList = ArrayList<Pair<String, String>>(count)
                    for (i in 0 until count) {
                        attrList.add(parser.getAttributeName(i).orEmpty() to parser.getAttributeValue(i).orEmpty())
                    }
                    builder.onStartTag(tagName, attrList)
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {
            // Return partially parsed rules safely on malformed XML
        }
        return builder.build()
    }

    private class IconPackFilterBuilder {
        private val componentDrawables = LinkedHashMap<String, String>()
        private val packageFallbackDrawables = LinkedHashMap<String, String>()
        private val calendarPrefixes = LinkedHashMap<String, String>()
        private val iconBacks = ArrayList<String>()
        private var iconMask: String? = null
        private var iconUpon: String? = null
        private var scale: Float = 1.0f

        fun onStartTag(tagName: String, attributes: List<Pair<String, String>>) {
            when (tagName) {
                "item" -> {
                    val componentRaw = attributes.firstValue("component")
                    val drawableRaw = cleanDrawableName(attributes.firstValue("drawable"))
                    val parsed = normalizeComponent(componentRaw)
                    if (parsed != null && drawableRaw != null) {
                        val (pkg, act) = parsed
                        componentDrawables["$pkg/$act"] = drawableRaw
                        packageFallbackDrawables.putIfAbsent(pkg, drawableRaw)
                    }
                }

                "calendar" -> {
                    val componentRaw = attributes.firstValue("component")
                    val prefixRaw = cleanDrawableName(attributes.firstValue("prefix"))
                    val parsed = normalizeComponent(componentRaw)
                    if (parsed != null && prefixRaw != null) {
                        val (pkg, act) = parsed
                        calendarPrefixes["$pkg/$act"] = prefixRaw
                    }
                }

                "iconback" -> {
                    for ((attrName, attrValue) in attributes) {
                        if (attrName.startsWith("img", ignoreCase = true)) {
                            cleanDrawableName(attrValue)?.let { iconBacks.add(it) }
                        }
                    }
                }

                "iconmask" -> {
                    val maskName = cleanDrawableName(
                        attributes.firstValue("img1")
                            ?: attributes.firstOrNull { it.first.startsWith("img", ignoreCase = true) }?.second
                    )
                    if (maskName != null) {
                        iconMask = maskName
                    }
                }

                "iconupon" -> {
                    val uponName = cleanDrawableName(
                        attributes.firstValue("img1")
                            ?: attributes.firstOrNull { it.first.startsWith("img", ignoreCase = true) }?.second
                    )
                    if (uponName != null) {
                        iconUpon = uponName
                    }
                }

                "scale" -> {
                    val parsedScale = attributes.firstValue("factor")?.trim()?.toFloatOrNull()
                    if (parsedScale != null && parsedScale.isFinite()) {
                        scale = parsedScale.coerceIn(MIN_SCALE, MAX_SCALE)
                    }
                }
            }
        }

        fun build(): IconPackFilterData {
            return IconPackFilterData(
                componentDrawables = componentDrawables,
                packageFallbackDrawables = packageFallbackDrawables,
                calendarPrefixes = calendarPrefixes,
                iconBacks = iconBacks,
                iconMask = iconMask,
                iconUpon = iconUpon,
                scale = scale
            )
        }

        private fun List<Pair<String, String>>.firstValue(name: String): String? {
            return firstOrNull { it.first.equals(name, ignoreCase = true) }?.second
        }

        private fun cleanDrawableName(raw: String?): String? {
            val trimmed = raw?.trim()?.removePrefix("@drawable/")?.trim()
            return trimmed?.takeIf { it.isNotEmpty() }
        }
    }
}
