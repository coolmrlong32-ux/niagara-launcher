package com.niagara.launcher.core.data

import com.niagara.launcher.core.model.LauncherPreferences
import com.niagara.launcher.core.model.ParsedWeatherLocation
import com.niagara.launcher.core.model.WeatherCachePolicy
import com.niagara.launcher.core.model.WeatherSnapshot
import com.niagara.launcher.core.model.WeatherLocationParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

sealed interface WeatherRefreshResult {
    data object NotConfigured : WeatherRefreshResult
    data object CacheFresh : WeatherRefreshResult
    data class Updated(val snapshot: WeatherSnapshot) : WeatherRefreshResult
    data class Failed(val message: String) : WeatherRefreshResult
}

/**
 * Lightweight, API-key-free Open-Meteo weather repository with local DataStore caching,
 * coordinate & city name support, and non-blocking IO refresh.
 */
class WeatherRepository(
    private val preferencesRepository: PreferencesRepository,
    private val httpFetcher: (String) -> String? = ::defaultHttpGet,
    private val clockMs: () -> Long = { System.currentTimeMillis() }
) {

    suspend fun refreshIfNeeded(
        preferences: LauncherPreferences,
        force: Boolean = false
    ): WeatherRefreshResult {
        val rawQuery = preferences.weatherLocationQuery.trim()
        if (rawQuery.isEmpty()) {
            return WeatherRefreshResult.NotConfigured
        }

        val now = clockMs()
        val cached = preferences.cachedWeather
        val sameQuery = preferences.weatherResolvedQuery.equals(rawQuery, ignoreCase = true)

        if (!force && sameQuery && !WeatherCachePolicy.isStale(cached, now)) {
            return WeatherRefreshResult.CacheFresh
        }

        return withContext(Dispatchers.IO) {
            val parsed = WeatherLocationParser.parse(rawQuery)
                ?: return@withContext WeatherRefreshResult.Failed("坐标或城市格式无效")

            val resolvedLocation = when (parsed) {
                is ParsedWeatherLocation.Coordinates -> ResolvedTarget(
                    displayName = parsed.displayLabel,
                    latitude = parsed.coords.latitude,
                    longitude = parsed.coords.longitude
                )

                is ParsedWeatherLocation.CityName -> {
                    if (sameQuery && cached != null) {
                        ResolvedTarget(
                            displayName = cached.locationName,
                            latitude = cached.latitude,
                            longitude = cached.longitude
                        )
                    } else {
                        val geoUrl = buildGeocodingUrl(parsed.name)
                        val geoJson = httpFetcher(geoUrl)
                            ?: return@withContext WeatherRefreshResult.Failed("网络连接失败，已保留本地缓存")
                        val match = OpenMeteoParser.parseGeocodingJson(geoJson)
                            ?: return@withContext WeatherRefreshResult.Failed("未找到城市「${parsed.name}」")
                        ResolvedTarget(
                            displayName = match.name,
                            latitude = match.latitude,
                            longitude = match.longitude
                        )
                    }
                }
            }

            val forecastUrl = buildForecastUrl(resolvedLocation.latitude, resolvedLocation.longitude)
            val forecastJson = httpFetcher(forecastUrl)
                ?: return@withContext WeatherRefreshResult.Failed("天气服务暂不可用，已保留缓存")
            val forecast = OpenMeteoParser.parseForecastJson(forecastJson)
                ?: return@withContext WeatherRefreshResult.Failed("天气数据解析异常")

            val snapshot = WeatherSnapshot(
                locationName = resolvedLocation.displayName,
                latitude = resolvedLocation.latitude,
                longitude = resolvedLocation.longitude,
                temperatureCelsius = forecast.temperatureCelsius,
                weatherCode = forecast.weatherCode,
                isDay = forecast.isDay,
                updatedAtMillis = clockMs()
            )
            preferencesRepository.saveCachedWeather(
                resolvedQuery = rawQuery,
                snapshot = snapshot
            )
            WeatherRefreshResult.Updated(snapshot)
        }
    }

    private data class ResolvedTarget(
        val displayName: String,
        val latitude: Double,
        val longitude: Double
    )

    companion object {
        private const val CONNECT_TIMEOUT_MS = 5_000
        private const val READ_TIMEOUT_MS = 6_000

        internal fun buildGeocodingUrl(cityName: String): String {
            val encoded = URLEncoder.encode(cityName, "UTF-8")
            return "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=1&language=zh&format=json"
        }

        internal fun buildForecastUrl(latitude: Double, longitude: Double): String {
            val latStr = String.format(Locale.US, "%.4f", latitude)
            val lonStr = String.format(Locale.US, "%.4f", longitude)
            return "https://api.open-meteo.com/v1/forecast?latitude=$latStr&longitude=$lonStr&current=temperature_2m,weather_code,is_day&timezone=auto"
        }

        private fun defaultHttpGet(urlString: String): String? {
            var connection: HttpURLConnection? = null
            return try {
                connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "NiagaraLauncher/1.0")
                }
                if (connection.responseCode !in 200..299) {
                    return null
                }
                BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            } catch (_: Exception) {
                null
            } finally {
                connection?.disconnect()
            }
        }
    }
}
