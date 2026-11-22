package com.niagara.launcher.core.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.provider.Settings
import com.niagara.launcher.core.model.AppItem
import com.niagara.launcher.core.pinyin.PinyinEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext

/**
 * Handles package management, application querying, execution, and app actions.
 */
class AppRepository(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val _appUpdateTrigger = MutableSharedFlow<Unit>(replay = 1)
    val appUpdateTrigger: SharedFlow<Unit> = _appUpdateTrigger

    init {
        _appUpdateTrigger.tryEmit(Unit)
    }

    fun notifyAppListChanged() {
        _appUpdateTrigger.tryEmit(Unit)
    }

    /**
     * Loads all launchable apps sorted alphabetically.
     */
    suspend fun loadInstalledApps(): List<AppItem> = withContext(Dispatchers.IO) {
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos: List<ResolveInfo> = packageManager.queryIntentActivities(
            launcherIntent,
            PackageManager.MATCH_ALL
        )

        val appList = resolveInfos.mapNotNull { resolveInfo ->
            val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
            val packageName = activityInfo.packageName
            val activityName = activityInfo.name
            // Skip launcher itself
            if (packageName == context.packageName) return@mapNotNull null

            val label = resolveInfo.loadLabel(packageManager).toString()
            val icon = try {
                resolveInfo.loadIcon(packageManager)
            } catch (_: Exception) {
                packageManager.defaultActivityIcon
            }

            val firstLetter = PinyinEngine.getFirstLetter(label)
            val pinyinInitials = PinyinEngine.getPinyinInitials(label)
            val fullPinyin = PinyinEngine.getFullPinyin(label)

            AppItem(
                packageName = packageName,
                activityName = activityName,
                label = label,
                icon = icon,
                firstLetter = firstLetter,
                pinyinInitials = pinyinInitials,
                fullPinyin = fullPinyin
            )
        }

        appList.sortedWith(compareBy({ it.firstLetter }, { it.label.lowercase() }))
    }

    /**
     * Launches the specified application.
     */
    fun launchApp(app: AppItem): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = ComponentName(app.packageName, app.activityName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Opens system app details page in Settings.
     */
    fun openAppDetails(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback to general settings
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    /**
     * Triggers the system uninstall dialogue for the given package.
     */
    fun requestUninstall(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.fromParts("package", packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // Handle error
        }
    }
}
