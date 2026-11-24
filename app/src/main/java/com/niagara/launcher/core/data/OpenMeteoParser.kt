package com.niagara.launcher.core.data

/**
 * Pure Kotlin lightweight JSON parser for Open-Meteo geocoding & current-weather responses.
 * Runs identically on Android and pure JVM unit tests without external dependencies.
 */
object OpenMeteoParser {

    data class GeocodingMatch(
        val name: String,
        val admin1: String?,
        val country: String?,
        val latitude: Double,
        val longitude: Double
    )

    data class ForecastResult(
        val temperatureCelsius: Double,
        val weatherCode: Int,
        val isDay: Boolean
    )

    fun parseGeocodingJson(json: String): GeocodingMatch? {
        val root = SimpleJsonReader.parse(json) as? Map<*, *> ?: return null
        val results = root["results"] as? List<*> ?: return null
        val first = results.firstOrNull() as? Map<*, *> ?: return null

        val name = (first["name"] as? String)?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val lat = (first["latitude"] as? Number)?.toDouble() ?: return null
        val lon = (first["longitude"] as? Number)?.toDouble() ?: return null
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null

        return GeocodingMatch(
            name = name,
            admin1 = (first["admin1"] as? String)?.trim()?.takeIf { it.isNotEmpty() },
            country = (first["country"] as? String)?.trim()?.takeIf { it.isNotEmpty() },
            latitude = lat,
            longitude = lon
        )
    }

    fun parseForecastJson(json: String): ForecastResult? {
        val root = SimpleJsonReader.parse(json) as? Map<*, *> ?: return null
        val current = root["current"] as? Map<*, *> ?: return null

        val temperature = (current["temperature_2m"] as? Number)?.toDouble() ?: return null
        val weatherCode = (current["weather_code"] as? Number)?.toInt() ?: return null
        val isDayRaw = (current["is_day"] as? Number)?.toInt()
        val isDay = isDayRaw != 0

        return ForecastResult(
            temperatureCelsius = temperature,
            weatherCode = weatherCode,
            isDay = isDay
        )
    }
}

/**
 * Minimal RFC-8259 compliant pure-Kotlin JSON reader supporting objects, arrays, strings, numbers, booleans, null.
 */
internal class SimpleJsonReader private constructor(private val input: String) {
    private var pos = 0

    private fun parseValue(): Any? {
        skipWhitespace()
        if (pos >= input.length) return null
        return when (input[pos]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> parseString()
            't' -> parseLiteral("true", true)
            'f' -> parseLiteral("false", false)
            'n' -> parseLiteral("null", null)
            '-', in '0'..'9' -> parseNumber()
            else -> throw IllegalArgumentException("Unexpected char '${input[pos]}' at $pos")
        }
    }

    private fun parseObject(): Map<String, Any?> {
        pos++ // '{'
        val map = LinkedHashMap<String, Any?>()
        skipWhitespace()
        if (pos < input.length && input[pos] == '}') {
            pos++
            return map
        }
        while (pos < input.length) {
            skipWhitespace()
            if (pos >= input.length || input[pos] != '"') {
                throw IllegalArgumentException("Expected string key at $pos")
            }
            val key = parseString()
            skipWhitespace()
            if (pos >= input.length || input[pos] != ':') {
                throw IllegalArgumentException("Expected ':' at $pos")
            }
            pos++ // ':'
            val value = parseValue()
            map[key] = value
            skipWhitespace()
            if (pos < input.length && input[pos] == ',') {
                pos++
                continue
            }
            if (pos < input.length && input[pos] == '}') {
                pos++
                return map
            }
            throw IllegalArgumentException("Expected ',' or '}' at $pos")
        }
        throw IllegalArgumentException("Unterminated object")
    }

    private fun parseArray(): List<Any?> {
        pos++ // '['
        val list = ArrayList<Any?>()
        skipWhitespace()
        if (pos < input.length && input[pos] == ']') {
            pos++
            return list
        }
        while (pos < input.length) {
            list.add(parseValue())
            skipWhitespace()
            if (pos < input.length && input[pos] == ',') {
                pos++
                continue
            }
            if (pos < input.length && input[pos] == ']') {
                pos++
                return list
            }
            throw IllegalArgumentException("Expected ',' or ']' at $pos")
        }
        throw IllegalArgumentException("Unterminated array")
    }

    private fun parseString(): String {
        pos++ // opening '"'
        val sb = StringBuilder()
        while (pos < input.length) {
            val ch = input[pos++]
            when (ch) {
                '"' -> return sb.toString()
                '\\' -> {
                    if (pos >= input.length) throw IllegalArgumentException("Unterminated escape")
                    when (val esc = input[pos++]) {
                        '"', '\\', '/' -> sb.append(esc)
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            if (pos + 4 > input.length) throw IllegalArgumentException("Invalid unicode escape")
                            val hex = input.substring(pos, pos + 4)
                            sb.append(hex.toInt(16).toChar())
                            pos += 4
                        }
                        else -> throw IllegalArgumentException("Invalid escape '\\$esc'")
                    }
                }
                else -> sb.append(ch)
            }
        }
        throw IllegalArgumentException("Unterminated string")
    }

    private fun parseNumber(): Number {
        val start = pos
        if (input[pos] == '-') pos++
        while (pos < input.length && input[pos] in '0'..'9') pos++
        var isFloat = false
        if (pos < input.length && input[pos] == '.') {
            isFloat = true
            pos++
            while (pos < input.length && input[pos] in '0'..'9') pos++
        }
        if (pos < input.length && (input[pos] == 'e' || input[pos] == 'E')) {
            isFloat = true
            pos++
            if (pos < input.length && (input[pos] == '+' || input[pos] == '-')) pos++
            while (pos < input.length && input[pos] in '0'..'9') pos++
        }
        val token = input.substring(start, pos)
        return if (isFloat) {
            token.toDouble()
        } else {
            token.toLongOrNull() ?: token.toDouble()
        }
    }

    private fun parseLiteral(expected: String, result: Any?): Any? {
        if (!input.regionMatches(pos, expected, 0, expected.length)) {
            throw IllegalArgumentException("Expected '$expected' at $pos")
        }
        pos += expected.length
        return result
    }

    private fun skipWhitespace() {
        while (pos < input.length && input[pos] <= ' ') {
            pos++
        }
    }

    companion object {
        fun parse(json: String): Any? {
            return try {
                SimpleJsonReader(json).parseValue()
            } catch (_: Exception) {
                null
            }
        }
    }
}
