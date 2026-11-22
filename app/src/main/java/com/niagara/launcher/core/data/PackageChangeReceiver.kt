package com.niagara.launcher.core.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.niagara.launcher.NiagaraApplication

/**
 * Listens for system package changes (install, uninstall, update) to keep app drawer synchronized.
 */
class PackageChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        when (action) {
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_REMOVED,
            Intent.ACTION_PACKAGE_CHANGED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                val app = context.applicationContext as? NiagaraApplication
                app?.appRepository?.notifyAppListChanged()
            }
        }
    }
}
