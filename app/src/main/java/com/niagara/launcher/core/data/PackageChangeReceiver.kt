package com.niagara.launcher.core.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.niagara.launcher.NiagaraApplication

/**
 * Listens for system package broadcasts (if dynamically registered) and forwards
 * typed incremental package change events to [AppRepository].
 */
class PackageChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val packageName = intent.data?.schemeSpecificPart
        val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
        val app = context.applicationContext as? NiagaraApplication ?: return

        val event = when (action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                if (packageName != null) PackageChangeEvent.PackageAdded(packageName)
                else PackageChangeEvent.FullReload
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                // Skip intermediate removal broadcast during an in-place app update
                if (isReplacing) return
                if (packageName != null) PackageChangeEvent.PackageRemoved(packageName)
                else PackageChangeEvent.FullReload
            }
            Intent.ACTION_PACKAGE_CHANGED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                if (packageName != null) PackageChangeEvent.PackageChanged(packageName)
                else PackageChangeEvent.FullReload
            }
            else -> return
        }

        app.appRepository.onPackageEvent(event)
    }
}
