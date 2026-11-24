package com.niagara.launcher.core.system

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Immutable
import com.niagara.launcher.R

enum class ScreenLockResult {
    LOCKED,
    PERMISSION_REQUIRED,
    UNSUPPORTED
}

@Immutable
data class ScreenLockPermissionStatus(
    val accessibilityEnabled: Boolean,
    val deviceAdminEnabled: Boolean,
    val supportsAccessibilityLock: Boolean
) {
    val hasAnyPermission: Boolean
        get() = (supportsAccessibilityLock && accessibilityEnabled) || deviceAdminEnabled
}

/**
 * Unified screen-lock helper preferring Android 9+ Accessibility global action
 * and falling back cleanly to DevicePolicyManager when configured.
 */
object ScreenLockController {

    fun getPermissionStatus(context: Context): ScreenLockPermissionStatus {
        val supportsAccessibility = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
        val accessibilityEnabled = isAccessibilityServiceActive(context)
        val deviceAdminEnabled = isDeviceAdminActive(context)
        return ScreenLockPermissionStatus(
            accessibilityEnabled = accessibilityEnabled,
            deviceAdminEnabled = deviceAdminEnabled,
            supportsAccessibilityLock = supportsAccessibility
        )
    }

    fun tryLockScreen(context: Context): ScreenLockResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val service = ScreenLockAccessibilityService.activeInstance
            if (service != null && service.performLockScreen()) {
                return ScreenLockResult.LOCKED
            }
        }

        if (isDeviceAdminActive(context)) {
            val dpm = context.getSystemService(DevicePolicyManager::class.java)
            if (dpm != null) {
                return try {
                    dpm.lockNow()
                    ScreenLockResult.LOCKED
                } catch (_: SecurityException) {
                    ScreenLockResult.PERMISSION_REQUIRED
                }
            }
        }

        return ScreenLockResult.PERMISSION_REQUIRED
    }

    fun openAccessibilitySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openSystemSettingsFallback(context)
        }
    }

    fun openDeviceAdminActivation(context: Context) {
        try {
            val adminComponent = ComponentName(context, ScreenLockDeviceAdminReceiver::class.java)
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    context.getString(R.string.device_admin_lock_desc)
                )
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openSystemSettingsFallback(context)
        }
    }

    private fun isAccessibilityServiceActive(context: Context): Boolean {
        if (ScreenLockAccessibilityService.activeInstance != null) return true
        return try {
            val enabledFlag = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED,
                0
            )
            if (enabledFlag != 1) return false
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val component = ComponentName(context, ScreenLockAccessibilityService::class.java)
            val fullName = component.flattenToString()
            val shortName = component.flattenToShortString()
            enabledServices.split(':').any { entry ->
                entry.equals(fullName, ignoreCase = true) || entry.equals(shortName, ignoreCase = true)
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun isDeviceAdminActive(context: Context): Boolean {
        return try {
            val dpm = context.getSystemService(DevicePolicyManager::class.java) ?: return false
            val admin = ComponentName(context, ScreenLockDeviceAdminReceiver::class.java)
            dpm.isAdminActive(admin)
        } catch (_: Exception) {
            false
        }
    }

    private fun openSystemSettingsFallback(context: Context) {
        try {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        } catch (_: Exception) {
            // Ignore if settings cannot open
        }
    }
}
