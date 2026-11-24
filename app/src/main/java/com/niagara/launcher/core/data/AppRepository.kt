package com.niagara.launcher.core.data

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings
import android.view.View
import com.niagara.launcher.core.model.AppItem
import com.niagara.launcher.core.model.IconPackInfo
import com.niagara.launcher.core.pinyin.PinyinEngine
import com.niagara.launcher.core.transition.LaunchSourceBounds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Handles package management, incremental application querying, icon caching,
 * application launching, and system app actions.
 */
class AppRepository(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val eventChannel = Channel<PackageChangeEvent>(Channel.UNLIMITED)
    private val observingStarted = AtomicBoolean(false)

    val iconPackManager: IconPackManager = IconPackManager(context)
    val iconCache: AppIconCache = AppIconCache(packageManager, iconPackManager)

    private val _installedApps = MutableStateFlow<List<AppItem>>(emptyList())
    val installedAppsFlow: StateFlow<List<AppItem>> = _installedApps.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    private val _installedIconPacks = MutableStateFlow<List<IconPackInfo>>(emptyList())
    val installedIconPacksFlow: StateFlow<List<IconPackInfo>> = _installedIconPacks.asStateFlow()

    private val _iconPacksLoaded = MutableStateFlow(false)
    val iconPacksLoaded: StateFlow<Boolean> = _iconPacksLoaded.asStateFlow()

    private val launcherAppsCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) {
            onPackageEvent(PackageChangeEvent.PackageRemoved(packageName))
        }

        override fun onPackageAdded(packageName: String, user: UserHandle) {
            onPackageEvent(PackageChangeEvent.PackageAdded(packageName))
        }

        override fun onPackageChanged(packageName: String, user: UserHandle) {
            onPackageEvent(PackageChangeEvent.PackageChanged(packageName))
        }

        override fun onPackagesAvailable(
            packageNames: Array<out String>,
            user: UserHandle,
            replacing: Boolean
        ) {
            packageNames.forEach { packageName ->
                onPackageEvent(PackageChangeEvent.PackageAdded(packageName))
            }
        }

        override fun onPackagesUnavailable(
            packageNames: Array<out String>,
            user: UserHandle,
            replacing: Boolean
        ) {
            if (!replacing) {
                packageNames.forEach { packageName ->
                    onPackageEvent(PackageChangeEvent.PackageRemoved(packageName))
                }
            }
        }
    }

    /**
     * Starts initial app loading and registers [LauncherApps.Callback] for incremental updates.
     * Safe to invoke multiple times.
     */
    fun startObservingPackages() {
        if (!observingStarted.compareAndSet(false, true)) return

        try {
            val launcherApps = context.getSystemService(LauncherApps::class.java)
            launcherApps?.registerCallback(launcherAppsCallback)
        } catch (_: Exception) {
            // Safe fallback if LauncherApps service is unavailable in restricted environments
        }

        repositoryScope.launch {
            reloadAllApps()

            for (firstEvent in eventChannel) {
                val batch = mutableListOf(firstEvent)
                delay(EVENT_DEBOUNCE_MS)
                while (true) {
                    val next = eventChannel.tryReceive().getOrNull() ?: break
                    batch.add(next)
                }
                processEventBatch(batch)
            }
        }
    }

    fun onPackageEvent(event: PackageChangeEvent) {
        eventChannel.trySend(event)
    }

    fun notifyAppListChanged() {
        onPackageEvent(PackageChangeEvent.FullReload)
    }

    fun onTrimMemory() {
        iconCache.clear()
    }

    private suspend fun reloadAllApps() {
        val loaded = loadInstalledApps()
        _installedApps.value = loaded
        _isLoaded.value = true
        refreshIconPacks()
    }

    suspend fun refreshIconPacks(): List<IconPackInfo> = withContext(Dispatchers.IO) {
        val packs = iconPackManager.loadInstalledIconPacks()
        _installedIconPacks.value = packs
        _iconPacksLoaded.value = true
        packs
    }

    private suspend fun processEventBatch(batch: List<PackageChangeEvent>) {
        if (batch.any { it is PackageChangeEvent.FullReload }) {
            iconCache.clear()
            reloadAllApps()
            return
        }

        val latestByPackage = linkedMapOf<String, PackageChangeEvent>()
        for (event in batch) {
            when (event) {
                is PackageChangeEvent.PackageAdded -> latestByPackage[event.packageName] = event
                is PackageChangeEvent.PackageRemoved -> latestByPackage[event.packageName] = event
                is PackageChangeEvent.PackageChanged -> latestByPackage[event.packageName] = event
                PackageChangeEvent.FullReload -> Unit
            }
        }

        var updatedList = _installedApps.value
        for ((packageName, event) in latestByPackage) {
            iconCache.evictPackage(packageName)

            val replacementApps = when (event) {
                is PackageChangeEvent.PackageRemoved -> emptyList()
                is PackageChangeEvent.PackageAdded,
                is PackageChangeEvent.PackageChanged -> queryLaunchableAppsForPackage(packageName)
                PackageChangeEvent.FullReload -> continue
            }
            updatedList = applyPackageDelta(updatedList, packageName, replacementApps)
        }

        _installedApps.value = updatedList
        _isLoaded.value = true
        refreshIconPacks()
    }

    /**
     * Loads all launchable apps sorted alphabetically without synchronously decoding icons.
     */
    suspend fun loadInstalledApps(): List<AppItem> = withContext(Dispatchers.IO) {
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos: List<ResolveInfo> = packageManager.queryIntentActivities(
            launcherIntent,
            PackageManager.MATCH_ALL
        )

        resolveInfos
            .mapNotNull { toAppItem(it) }
            .sortedWith(APP_COMPARATOR)
    }

    private suspend fun queryLaunchableAppsForPackage(packageName: String): List<AppItem> =
        withContext(Dispatchers.IO) {
            val packageIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(packageName)
            }

            val resolveInfos: List<ResolveInfo> = try {
                packageManager.queryIntentActivities(packageIntent, PackageManager.MATCH_ALL)
            } catch (_: Exception) {
                emptyList()
            }

            resolveInfos.mapNotNull { toAppItem(it) }
        }

    private fun toAppItem(resolveInfo: ResolveInfo): AppItem? {
        val activityInfo = resolveInfo.activityInfo ?: return null
        val packageName = activityInfo.packageName
        val activityName = activityInfo.name

        val label = resolveInfo.loadLabel(packageManager).toString()
        val pinyinIndex = PinyinEngine.buildIndex(label)

        return AppItem(
            packageName = packageName,
            activityName = activityName,
            label = label,
            firstLetter = pinyinIndex.firstLetter,
            pinyinInitials = pinyinIndex.primaryInitials,
            fullPinyin = pinyinIndex.primaryFullPinyin,
            pinyinIndex = pinyinIndex
        )
    }

    /**
     * Launches the target application safely with an optional icon-anchored reveal/scale transition.
     */
    fun launchApp(
        app: AppItem,
        anchorView: View? = null,
        sourceBounds: LaunchSourceBounds? = null
    ): Boolean {
        val launchContext = anchorView?.context ?: context
        val optionsBundle = buildLaunchOptionsBundle(anchorView, sourceBounds)

        return try {
            val componentName = ComponentName(app.packageName, app.activityName)
            val launchIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = componentName
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }
            startActivitySafely(launchContext, launchIntent, optionsBundle)
            true
        } catch (_: Exception) {
            try {
                val fallbackIntent = packageManager.getLaunchIntentForPackage(app.packageName)
                if (fallbackIntent != null) {
                    fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivitySafely(launchContext, fallbackIntent, optionsBundle)
                    true
                } else {
                    false
                }
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun startActivitySafely(launchContext: Context, intent: Intent, optionsBundle: Bundle?) {
        if (optionsBundle != null) {
            try {
                launchContext.startActivity(intent, optionsBundle)
                return
            } catch (_: Exception) {
                // Fall back to standard launch if OEM rejects custom ActivityOptions
            }
        }
        launchContext.startActivity(intent)
    }

    private fun buildLaunchOptionsBundle(
        anchorView: View?,
        sourceBounds: LaunchSourceBounds?
    ): Bundle? {
        if (anchorView == null || anchorView.width <= 0 || anchorView.height <= 0) return null
        val clamped = sourceBounds?.toClampedRelativeBounds(anchorView.width, anchorView.height)

        return try {
            if (clamped != null) {
                ActivityOptions.makeClipRevealAnimation(
                    anchorView,
                    clamped.left,
                    clamped.top,
                    clamped.width,
                    clamped.height
                ).toBundle()
            } else {
                ActivityOptions.makeScaleUpAnimation(
                    anchorView,
                    anchorView.width / 2,
                    anchorView.height / 2,
                    0,
                    0
                ).toBundle()
            }
        } catch (_: Exception) {
            try {
                val fallback = clamped ?: LaunchSourceBounds(
                    left = anchorView.width / 2,
                    top = anchorView.height / 2,
                    width = 1,
                    height = 1
                )
                ActivityOptions.makeScaleUpAnimation(
                    anchorView,
                    fallback.left,
                    fallback.top,
                    fallback.width,
                    fallback.height
                ).toBundle()
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Opens system app details settings page.
     */
    fun openAppDetails(app: AppItem) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", app.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    /**
     * Requests system uninstallation of the application.
     */
    fun uninstallApp(app: AppItem) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.fromParts("package", app.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // Handle error
        }
    }

    companion object {
        private const val EVENT_DEBOUNCE_MS = 150L

        internal val APP_COMPARATOR: Comparator<AppItem> =
            compareBy({ it.firstLetter }, { it.label.lowercase() })

        /**
         * Pure incremental merge helper: replaces all entries belonging to [packageName]
         * with [updatedPackageApps] and returns a deterministically sorted list.
         */
        internal fun applyPackageDelta(
            currentApps: List<AppItem>,
            packageName: String,
            updatedPackageApps: List<AppItem>
        ): List<AppItem> {
            val remaining = currentApps.filterNot { it.packageName == packageName }
            return (remaining + updatedPackageApps).sortedWith(APP_COMPARATOR)
        }
    }
}
