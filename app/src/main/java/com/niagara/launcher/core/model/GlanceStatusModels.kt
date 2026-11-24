package com.niagara.launcher.core.model

import androidx.compose.runtime.Immutable
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Snapshot of RAM utilization derived from ActivityManager.MemoryInfo.
 */
@Immutable
data class MemoryUsageInfo(
    val usedBytes: Long,
    val totalBytes: Long,
    val usedPercent: Int,
    val usedGb: Float,
    val totalGb: Float
)

/**
 * Real-time lightweight phone telemetry shown in the Glance header while foregrounded.
 */
@Immutable
data class DeviceStatusSnapshot(
    val memory: MemoryUsageInfo? = null,
    val batteryPercent: Int? = null,
    val batteryTempCelsius: Float? = null,
    val isCharging: Boolean = false
)

/**
 * Pure Kotlin math and formatting helpers for device memory and battery telemetry.
 */
object DeviceStatusCalculator {
    private const val BYTES_PER_GB = 1024.0 * 1024.0 * 1024.0

    fun computeMemoryUsage(availBytes: Long, totalBytes: Long): MemoryUsageInfo? {
        if (totalBytes <= 0L) return null
        val clampedAvail = availBytes.coerceIn(0L, totalBytes)
        val usedBytes = (totalBytes - clampedAvail).coerceAtLeast(0L)
        val percent = ((usedBytes.toDouble() * 100.0) / totalBytes.toDouble())
            .roundToInt()
            .coerceIn(0, 100)
        val usedGb = (usedBytes.toDouble() / BYTES_PER_GB).toFloat()
        val totalGb = (totalBytes.toDouble() / BYTES_PER_GB).toFloat()
        return MemoryUsageInfo(
            usedBytes = usedBytes,
            totalBytes = totalBytes,
            usedPercent = percent,
            usedGb = usedGb,
            totalGb = totalGb
        )
    }

    /**
     * Converts Android BatteryManager.EXTRA_TEMPERATURE (tenths of a degree Celsius) to °C.
     */
    fun parseBatteryTempTenths(rawTenths: Int): Float? {
        if (rawTenths <= -200 || rawTenths >= 1000) return null
        return rawTenths / 10f
    }

    fun computeBatteryPercent(level: Int, scale: Int): Int? {
        if (level < 0 || scale <= 0) return null
        return ((level * 100f) / scale.toFloat()).roundToInt().coerceIn(0, 100)
    }

    fun formatMemoryCompact(info: MemoryUsageInfo): String {
        val usedStr = String.format(Locale.US, "%.1f", info.usedGb)
        val totalStr = if (info.totalGb >= 9.95f) {
            info.totalGb.roundToInt().toString()
        } else {
            String.format(Locale.US, "%.1f", info.totalGb)
        }
        return "$usedStr/${totalStr}G · ${info.usedPercent}%"
    }

    fun formatTemperatureCelsius(tempCelsius: Float): String {
        return String.format(Locale.US, "%.1f°C", tempCelsius)
    }
}

enum class WeatherIconType {
    CLEAR_DAY,
    CLEAR_NIGHT,
    PARTLY_CLOUDY,
    CLOUDY,
    FOG,
    DRIZZLE,
    RAIN,
    SNOW,
    THUNDERSTORM
}

@Immutable
data class WeatherCondition(
    val label: String,
    val iconType: WeatherIconType
)

/**
 * Maps WMO weather interpretation codes returned by Open-Meteo into concise Chinese labels and icons.
 */
object WmoWeatherMapper {
    fun map(weatherCode: Int, isDay: Boolean = true): WeatherCondition {
        return when (weatherCode) {
            0 -> WeatherCondition("晴", if (isDay) WeatherIconType.CLEAR_DAY else WeatherIconType.CLEAR_NIGHT)
            1 -> WeatherCondition("大部晴朗", if (isDay) WeatherIconType.PARTLY_CLOUDY else WeatherIconType.CLEAR_NIGHT)
            2 -> WeatherCondition("多云", WeatherIconType.PARTLY_CLOUDY)
            3 -> WeatherCondition("阴", WeatherIconType.CLOUDY)
            45, 48 -> WeatherCondition("雾", WeatherIconType.FOG)
            51, 53, 55, 56, 57 -> WeatherCondition("毛毛雨", WeatherIconType.DRIZZLE)
            61, 66 -> WeatherCondition("小雨", WeatherIconType.RAIN)
            63 -> WeatherCondition("中雨", WeatherIconType.RAIN)
            65, 67 -> WeatherCondition("大雨", WeatherIconType.RAIN)
            71, 73, 75, 77, 85, 86 -> WeatherCondition("雪", WeatherIconType.SNOW)
            80, 81, 82 -> WeatherCondition("阵雨", WeatherIconType.RAIN)
            95, 96, 99 -> WeatherCondition("雷阵雨", WeatherIconType.THUNDERSTORM)
            else -> WeatherCondition("多云", WeatherIconType.CLOUDY)
        }
    }
}

@Immutable
data class WeatherCoordinates(
    val latitude: Double,
    val longitude: Double
)

sealed interface ParsedWeatherLocation {
    data class Coordinates(
        val coords: WeatherCoordinates,
        val displayLabel: String
    ) : ParsedWeatherLocation

    data class CityName(val name: String) : ParsedWeatherLocation
}

/**
 * Parses user-provided weather locations (either "lat,lon" coordinates or a city name).
 */
object WeatherLocationParser {
    private val COORDINATE_REGEX = Regex("""^\s*([+-]?\d+(?:\.\d+)?)\s*[,，\s]+\s*([+-]?\d+(?:\.\d+)?)\s*$""")

    fun parse(rawInput: String?): ParsedWeatherLocation? {
        val trimmed = rawInput?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val match = COORDINATE_REGEX.matchEntire(trimmed)
        if (match != null) {
            val lat = match.groupValues[1].toDoubleOrNull()
            val lon = match.groupValues[2].toDoubleOrNull()
            if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) {
                val label = String.format(Locale.US, "%.2f, %.2f", lat, lon)
                return ParsedWeatherLocation.Coordinates(
                    coords = WeatherCoordinates(latitude = lat, longitude = lon),
                    displayLabel = label
                )
            }
            return null
        }
        return ParsedWeatherLocation.CityName(trimmed)
    }
}

@Immutable
data class WeatherSnapshot(
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val temperatureCelsius: Double,
    val weatherCode: Int,
    val isDay: Boolean,
    val updatedAtMillis: Long
) {
    val condition: WeatherCondition
        get() = WmoWeatherMapper.map(weatherCode, isDay)

    val formattedTemperature: String
        get() = "${temperatureCelsius.roundToInt()}°"
}

/**
 * Cache freshness policy preventing unnecessary network calls and keeping cold startup instant.
 */
object WeatherCachePolicy {
    const val REFRESH_INTERVAL_MS: Long = 30L * 60L * 1000L

    fun isStale(
        snapshot: WeatherSnapshot?,
        nowMillis: Long,
        ttlMillis: Long = REFRESH_INTERVAL_MS
    ): Boolean {
        if (snapshot == null) return true
        val elapsed = nowMillis - snapshot.updatedAtMillis
        return elapsed < 0L || elapsed >= ttlMillis
    }
}
