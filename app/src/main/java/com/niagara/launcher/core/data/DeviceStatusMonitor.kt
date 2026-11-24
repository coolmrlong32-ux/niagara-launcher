package com.niagara.launcher.core.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.niagara.launcher.core.model.DeviceStatusCalculator
import com.niagara.launcher.core.model.DeviceStatusSnapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Lightweight, zero-service phone status reader for RAM usage and battery temperature/charge.
 * Intended to be collected strictly while the launcher is in the foreground.
 */
class DeviceStatusMonitor(private val context: Context) {

    fun readSnapshot(): DeviceStatusSnapshot {
        val memoryInfo = readMemoryUsage()
        val batteryIntent = try {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        } catch (_: Exception) {
            null
        }

        val batteryPercent = batteryIntent?.let { intent ->
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            DeviceStatusCalculator.computeBatteryPercent(level, scale)
        } ?: readBatteryPercentFallback()

        val tempCelsius = batteryIntent?.let { intent ->
            val rawTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            DeviceStatusCalculator.parseBatteryTempTenths(rawTenths)
        }

        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        return DeviceStatusSnapshot(
            memory = memoryInfo,
            batteryPercent = batteryPercent,
            batteryTempCelsius = tempCelsius,
            isCharging = isCharging
        )
    }

    fun observeStatus(intervalMs: Long = DEFAULT_POLL_INTERVAL_MS): Flow<DeviceStatusSnapshot> = flow {
        val safeInterval = intervalMs.coerceAtLeast(2_000L)
        while (true) {
            emit(readSnapshot())
            delay(safeInterval)
        }
    }

    private fun readMemoryUsage() = try {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        if (activityManager != null) {
            val info = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(info)
            DeviceStatusCalculator.computeMemoryUsage(
                availBytes = info.availMem,
                totalBytes = info.totalMem
            )
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }

    private fun readBatteryPercentFallback(): Int? = try {
        val manager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val pct = manager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        if (pct != null && pct in 0..100) pct else null
    } catch (_: Exception) {
        null
    }

    companion object {
        const val DEFAULT_POLL_INTERVAL_MS = 4_000L
    }
}
