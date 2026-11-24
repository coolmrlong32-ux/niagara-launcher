package com.niagara.launcher.core.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import com.niagara.launcher.core.model.IconPackInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Discovers installed third-party Android Icon Packs, parses and caches their `appfilter.xml`,
 * and renders themed icons or template-masked fallback icons.
 */
class IconPackManager(private val context: Context) {

    private data class LoadedIconPack(
        val packageName: String,
        val resources: Resources,
        val filterData: IconPackFilterData
    )

    private val packageManager: PackageManager = context.packageManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val loadedPacks = ConcurrentHashMap<String, LoadedIconPack>()
    private val inFlightLoads = ConcurrentHashMap<String, Deferred<LoadedIconPack?>>()

    suspend fun loadInstalledIconPacks(): List<IconPackInfo> = withContext(Dispatchers.IO) {
        val discovered = LinkedHashMap<String, IconPackInfo>()

        for (action in ICON_PACK_ACTIONS) {
            queryIntent(Intent(action), discovered)
        }
        for (category in ICON_PACK_CATEGORIES) {
            val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(category) }
            queryIntent(intent, discovered)
        }

        discovered.values.sortedBy { it.label.lowercase() }
    }

    private fun queryIntent(intent: Intent, out: MutableMap<String, IconPackInfo>) {
        val resolveInfos = try {
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        } catch (_: Exception) {
            emptyList()
        }

        for (info in resolveInfos) {
            val activityInfo = info.activityInfo ?: continue
            val packageName = activityInfo.packageName ?: continue
            if (packageName == context.packageName || out.containsKey(packageName)) continue

            val label = try {
                info.loadLabel(packageManager)?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            } catch (_: Exception) {
                null
            } ?: try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                packageName
            }

            out[packageName] = IconPackInfo(packageName = packageName, label = label)
        }
    }

    suspend fun preloadPack(packPackageName: String): Boolean {
        return getOrLoadPack(packPackageName) != null
    }

    suspend fun loadThemedBitmap(
        packPackageName: String,
        appPackageName: String,
        activityName: String,
        fallbackDrawable: () -> Drawable,
        targetSizePx: Int
    ): Bitmap? = withContext(Dispatchers.IO) {
        val pack = getOrLoadPack(packPackageName) ?: return@withContext null
        val dayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val drawableName = pack.filterData.resolveDrawableName(
            packageName = appPackageName,
            activityName = activityName,
            dayOfMonth = dayOfMonth
        )

        if (drawableName != null) {
            val themedDrawable = loadPackDrawable(pack, drawableName)
            if (themedDrawable != null) {
                return@withContext themedDrawable.toBitmap(
                    width = targetSizePx,
                    height = targetSizePx,
                    config = Bitmap.Config.ARGB_8888
                )
            }
        }

        if (!pack.filterData.hasFallbackTemplate) {
            return@withContext null
        }

        renderTemplatedFallback(
            pack = pack,
            componentKey = IconPackAppFilterParser.normalizeComponentKey(appPackageName, activityName),
            fallbackDrawable = fallbackDrawable(),
            sizePx = targetSizePx
        )
    }

    private suspend fun getOrLoadPack(packPackageName: String): LoadedIconPack? {
        loadedPacks[packPackageName]?.let { return it }

        val deferred = inFlightLoads.computeIfAbsent(packPackageName) {
            scope.async {
                try {
                    val resources = packageManager.getResourcesForApplication(packPackageName)
                    val filterData = parseAppFilter(resources, packPackageName)
                    LoadedIconPack(
                        packageName = packPackageName,
                        resources = resources,
                        filterData = filterData
                    ).also { loaded ->
                        loadedPacks[packPackageName] = loaded
                    }
                } catch (_: Exception) {
                    null
                } finally {
                    inFlightLoads.remove(packPackageName)
                }
            }
        }
        return deferred.await()
    }

    private fun parseAppFilter(resources: Resources, packPackageName: String): IconPackFilterData {
        val xmlResId = resources.getIdentifier("appfilter", "xml", packPackageName)
        if (xmlResId != 0) {
            try {
                val parser = resources.getXml(xmlResId)
                try {
                    return IconPackAppFilterParser.parsePullParser(parser)
                } finally {
                    parser.close()
                }
            } catch (_: Exception) {
                // Fall through to assets/raw
            }
        }

        try {
            resources.assets.open("appfilter.xml").use { stream ->
                return IconPackAppFilterParser.parseStream(stream)
            }
        } catch (_: Exception) {
            // Fall through to raw
        }

        val rawResId = resources.getIdentifier("appfilter", "raw", packPackageName)
        if (rawResId != 0) {
            try {
                resources.openRawResource(rawResId).use { stream ->
                    return IconPackAppFilterParser.parseStream(stream)
                }
            } catch (_: Exception) {
                // Ignore
            }
        }

        return IconPackFilterData()
    }

    private fun loadPackDrawable(pack: LoadedIconPack, drawableName: String): Drawable? {
        val drawableResId = pack.resources.getIdentifier(drawableName, "drawable", pack.packageName)
        val resId = if (drawableResId != 0) {
            drawableResId
        } else {
            pack.resources.getIdentifier(drawableName, "mipmap", pack.packageName)
        }
        if (resId == 0) return null
        return try {
            ResourcesCompat.getDrawable(pack.resources, resId, null)
        } catch (_: Exception) {
            null
        }
    }

    private fun renderTemplatedFallback(
        pack: LoadedIconPack,
        componentKey: String,
        fallbackDrawable: Drawable,
        sizePx: Int
    ): Bitmap? {
        return try {
            val result = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            val filter = pack.filterData

            if (filter.iconBacks.isNotEmpty()) {
                val backIndex = abs(componentKey.hashCode()) % filter.iconBacks.size
                loadPackDrawable(pack, filter.iconBacks[backIndex])?.let { backDrawable ->
                    val copy = backDrawable.constantState?.newDrawable(pack.resources)?.mutate() ?: backDrawable
                    copy.setBounds(0, 0, sizePx, sizePx)
                    copy.draw(canvas)
                }
            }

            val fgBitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val fgCanvas = Canvas(fgBitmap)
            val scaledSize = (sizePx * filter.scale).roundToInt().coerceIn(1, sizePx * 2)
            val offset = (sizePx - scaledSize) / 2
            val fgDrawable = fallbackDrawable.constantState?.newDrawable()?.mutate() ?: fallbackDrawable
            fgDrawable.setBounds(offset, offset, offset + scaledSize, offset + scaledSize)
            fgDrawable.draw(fgCanvas)

            filter.iconMask?.let { maskName ->
                loadPackDrawable(pack, maskName)?.let { maskDrawable ->
                    val maskBitmap = maskDrawable.toBitmap(
                        width = sizePx,
                        height = sizePx,
                        config = Bitmap.Config.ARGB_8888
                    )
                    val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                    }
                    fgCanvas.drawBitmap(maskBitmap, 0f, 0f, maskPaint)
                    maskBitmap.recycle()
                }
            }

            canvas.drawBitmap(fgBitmap, 0f, 0f, null)
            fgBitmap.recycle()

            filter.iconUpon?.let { uponName ->
                loadPackDrawable(pack, uponName)?.let { uponDrawable ->
                    val copy = uponDrawable.constantState?.newDrawable(pack.resources)?.mutate() ?: uponDrawable
                    copy.setBounds(0, 0, sizePx, sizePx)
                    copy.draw(canvas)
                }
            }

            result
        } catch (_: Exception) {
            null
        }
    }

    fun evictPack(packageName: String) {
        loadedPacks.remove(packageName)
        inFlightLoads.remove(packageName)?.cancel()
    }

    fun clear() {
        loadedPacks.clear()
        inFlightLoads.values.forEach { it.cancel() }
        inFlightLoads.clear()
    }

    companion object {
        private val ICON_PACK_ACTIONS = listOf(
            "org.adw.launcher.THEMES",
            "com.novalauncher.THEME",
            "com.anddoes.launcher.THEME",
            "com.teslacoilsw.launcher.THEME",
            "com.fede.launcher.THEME_ICONPACK",
            "com.gau.go.launcherex.theme",
            "com.sonymobile.home.ICON_PACK",
            "com.dlto.atom.launcher.THEME"
        )

        private val ICON_PACK_CATEGORIES = listOf(
            "com.anddoes.launcher.THEME",
            "com.teslacoilsw.launcher.THEME",
            "com.fede.launcher.THEME_ICONPACK",
            "com.novalauncher.THEME",
            "com.gau.go.launcherex.theme"
        )
    }
}
