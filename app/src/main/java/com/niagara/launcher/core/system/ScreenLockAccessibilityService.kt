package com.niagara.launcher.core.system

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent

/**
 * Lightweight, privacy-preserving AccessibilityService used exclusively to invoke
 * [GLOBAL_ACTION_LOCK_SCREEN] on Android 9+ without breaking biometric unlock.
 */
class ScreenLockAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        activeInstance = this
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (activeInstance === this) {
            activeInstance = null
        }
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        if (activeInstance === this) {
            activeInstance = null
        }
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    fun performLockScreen(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            } catch (_: Exception) {
                false
            }
        } else {
            false
        }
    }

    companion object {
        @Volatile
        var activeInstance: ScreenLockAccessibilityService? = null
            private set
    }
}
