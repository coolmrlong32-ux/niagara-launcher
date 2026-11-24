package com.niagara.launcher

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.niagara.launcher.core.data.OpenMeteoParser
import com.niagara.launcher.core.data.PreferencesRepository
import com.niagara.launcher.core.data.WeatherRefreshResult
import com.niagara.launcher.core.data.WeatherRepository
import com.niagara.launcher.core.model.DeviceStatusCalculator
import com.niagara.launcher.core.model.ParsedWeatherLocation
import com.niagara.launcher.core.model.WeatherCachePolicy
import com.niagara.launcher.core.model.WeatherIconType
import com.niagara.launcher.core.model.WeatherLocationParser
import com.niagara.launcher.core.model.WeatherSnapshot
import com.niagara.launcher.core.model.WmoWeatherMapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class GlanceStatusAndWeatherTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testDeviceStatusCalculatorMemoryAndBatteryMath() {
        val gb = 1024L * 1024L * 1024L
        val info = DeviceStatusCalculator.computeMemoryUsage(
            availBytes = 6L * gb,
            totalBytes = 12L * gb
        )
        assertNotNull(info)
        assertEquals(50, info!!.usedPercent)
        assertEquals(6.0f, info.usedGb, 0.01f)
        assertEquals(12.0f, info.totalGb, 0.01f)
        assertEquals("6.0/12G · 50%", DeviceStatusCalculator.formatMemoryCompact(info))

        // Invalid total returns null
        assertNull(DeviceStatusCalculator.computeMemoryUsage(100L, 0L))

        // Battery tenths conversion and sanity bounds
        assertEquals(32.5f, DeviceStatusCalculator.parseBatteryTempTenths(325)!!, 0.01f)
        assertEquals("32.5°C", DeviceStatusCalculator.formatTemperatureCelsius(32.5f))
        assertNull(DeviceStatusCalculator.parseBatteryTempTenths(-999))
        assertNull(DeviceStatusCalculator.parseBatteryTempTenths(1500))

        // Battery percentage calculation
        assertEquals(78, DeviceStatusCalculator.computeBatteryPercent(78, 100))
        assertNull(DeviceStatusCalculator.computeBatteryPercent(-1, 100))
        assertNull(DeviceStatusCalculator.computeBatteryPercent(50, 0))
    }

    @Test
    fun testWeatherLocationParserSupportsCoordinatesAndCityNames() {
        val coord = WeatherLocationParser.parse("31.2304, 121.4737")
        assertTrue(coord is ParsedWeatherLocation.Coordinates)
        val parsedCoord = coord as ParsedWeatherLocation.Coordinates
        assertEquals(31.2304, parsedCoord.coords.latitude, 0.0001)
        assertEquals(121.4737, parsedCoord.coords.longitude, 0.0001)

        // Supports Chinese full-width comma
        assertTrue(WeatherLocationParser.parse("39.90，116.40") is ParsedWeatherLocation.Coordinates)

        // Out-of-range coordinates rejected safely
        assertNull(WeatherLocationParser.parse("95.0, 121.0"))
        assertNull(WeatherLocationParser.parse("31.0, 190.0"))

        // City names parsed cleanly
        val city = WeatherLocationParser.parse("  上海  ")
        assertEquals(ParsedWeatherLocation.CityName("上海"), city)
        assertNull(WeatherLocationParser.parse("   "))
    }

    @Test
    fun testOpenMeteoParserAndWmoMapping() {
        val geoJson = """
            {
              "results": [
                {
                  "id": 1796236,
                  "name": "上海",
                  "latitude": 31.22222,
                  "longitude": 121.45806,
                  "country": "中国",
                  "admin1": "上海市"
                }
              ]
            }
        """.trimIndent()
        val geo = OpenMeteoParser.parseGeocodingJson(geoJson)
        assertNotNull(geo)
        assertEquals("上海", geo!!.name)
        assertEquals(31.22222, geo.latitude, 0.0001)
        assertEquals(121.45806, geo.longitude, 0.0001)

        val forecastJson = """
            {
              "latitude": 31.25,
              "longitude": 121.5,
              "current": {
                "time": "2026-11-24T16:00",
                "temperature_2m": 18.6,
                "weather_code": 2,
                "is_day": 1
              }
            }
        """.trimIndent()
        val forecast = OpenMeteoParser.parseForecastJson(forecastJson)
        assertNotNull(forecast)
        assertEquals(18.6, forecast!!.temperatureCelsius, 0.01)
        assertEquals(2, forecast.weatherCode)
        assertTrue(forecast.isDay)

        // Malformed JSON never throws
        assertNull(OpenMeteoParser.parseGeocodingJson("{bad json"))
        assertNull(OpenMeteoParser.parseForecastJson("{}"))

        // WMO code mapping
        assertEquals(WeatherIconType.CLEAR_DAY, WmoWeatherMapper.map(0, isDay = true).iconType)
        assertEquals(WeatherIconType.CLEAR_NIGHT, WmoWeatherMapper.map(0, isDay = false).iconType)
        assertEquals("雷阵雨", WmoWeatherMapper.map(95).label)
    }

    @Test
    fun testWeatherCachePolicyAndRepositoryCachingWithOfflineFallback() = runTest {
        val dataFile = tempFolder.newFile("weather_prefs.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { dataFile }
        )
        val prefsRepo = PreferencesRepository(dataStore)

        var nowMs = 10_000_000L
        var networkAvailable = true
        var fetchCalls = 0

        val fakeFetcher: (String) -> String? = { url ->
            fetchCalls++
            if (!networkAvailable) {
                null
            } else if (url.contains("geocoding-api")) {
                """{"results":[{"name":"杭州","latitude":30.2741,"longitude":120.1551}]}"""
            } else {
                """{"current":{"temperature_2m":21.4,"weather_code":0,"is_day":1}}"""
            }
        }

        val weatherRepo = WeatherRepository(
            preferencesRepository = prefsRepo,
            httpFetcher = fakeFetcher,
            clockMs = { nowMs }
        )

        // 1. Unconfigured returns NotConfigured with zero HTTP calls
        val initialPrefs = prefsRepo.launcherPreferencesFlow.first()
        assertEquals(WeatherRefreshResult.NotConfigured, weatherRepo.refreshIfNeeded(initialPrefs))
        assertEquals(0, fetchCalls)

        // 2. Configure city and refresh -> geocodes + fetches forecast + saves to DataStore
        prefsRepo.setWeatherLocationQuery("杭州")
        advanceUntilIdle()
        val configuredPrefs = prefsRepo.launcherPreferencesFlow.first()
        val updated = weatherRepo.refreshIfNeeded(configuredPrefs)
        assertTrue(updated is WeatherRefreshResult.Updated)
        assertEquals(2, fetchCalls)
        advanceUntilIdle()

        val savedPrefs = prefsRepo.launcherPreferencesFlow.first()
        assertNotNull(savedPrefs.cachedWeather)
        assertEquals("杭州", savedPrefs.cachedWeather!!.locationName)
        assertEquals("21°", savedPrefs.cachedWeather!!.formattedTemperature)

        // 3. Within TTL (10 minutes later) -> cache stays fresh, zero HTTP calls
        nowMs += 10L * 60L * 1000L
        assertFalse(WeatherCachePolicy.isStale(savedPrefs.cachedWeather, nowMs))
        assertEquals(WeatherRefreshResult.CacheFresh, weatherRepo.refreshIfNeeded(savedPrefs))
        assertEquals(2, fetchCalls)

        // 4. After TTL (35 minutes later) while offline -> gracefully fails and preserves cached weather
        nowMs += 35L * 60L * 1000L
        assertTrue(WeatherCachePolicy.isStale(savedPrefs.cachedWeather, nowMs))
        networkAvailable = false
        val offlineResult = weatherRepo.refreshIfNeeded(savedPrefs)
        assertTrue(offlineResult is WeatherRefreshResult.Failed)
        // Only forecast was attempted (cached geocoded coordinates were reused!)
        assertEquals(3, fetchCalls)
        // Cached snapshot remains intact
        val afterOfflinePrefs = prefsRepo.launcherPreferencesFlow.first()
        assertEquals("杭州", afterOfflinePrefs.cachedWeather?.locationName)
        assertEquals(21.4, afterOfflinePrefs.cachedWeather!!.temperatureCelsius, 0.01)
    }
}
