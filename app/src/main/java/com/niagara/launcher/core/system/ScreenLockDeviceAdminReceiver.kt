package com.niagara.launcher.core.system

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/**
 * Optional DeviceAdmin fallback for locking the screen on Android 8.0/8.1
 * or devices where the user prefers Device Administrator over Accessibility.
 */
class ScreenLockDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
    }
}
