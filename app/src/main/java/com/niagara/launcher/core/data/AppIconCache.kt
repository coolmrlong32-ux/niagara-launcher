package com.niagara.launcher.core.data

import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.niagara.launcher.core.model.IconAppearance
import com.niagara.launcher.core.model.IconStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

val LocalAppIconCache = staticCompositionLocalOf<AppIconCache?> { null }
val LocalIconAppearance = staticCompositionLocalOf { IconAppearance() }

/**
 * Thread-safe in-memory LRU cache for asynchronously decoded application icons.
 * Supports built-in icon styles (squircle/circle full-bleed vs adaptive) and third-party Icon Packs.
 */
class AppIconCache(
    private val packageManager: PackageManager,
    private val iconPackManager: IconPackManager? = null
) {
    private data class CachedIcon(
        val bitmap: ImageBitmap,
        val sizeKb: Int
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inFlight = ConcurrentHashMap<String, Deferred<ImageBitmap?>>()

    private val cache: LruCache<String, CachedIcon> = run {
        val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024L).toInt()
        val cacheSizeKb = (maxMemoryKb / 12).coerceIn(MIN_CACHE_KB, MAX_CACHE_KB)
        object : LruCache<String, CachedIcon>(cacheSizeKb) {
            override fun sizeOf(key: String, value: CachedIcon): Int = value.sizeKb
        }
    }

    /**
     * Returns the cached [ImageBitmap] synchronously for an exact cache key, or null on miss.
     */
    fun getCached(key: String): ImageBitmap? = cache.get(key)?.bitmap

    /**
     * Returns the cached [ImageBitmap] synchronously for an app and icon appearance, or null on miss.
     */
    fun getCached(
        packageName: String,
        activityName: String,
        iconStyle: IconStyle = IconStyle.IOS_SQUIRCLE,
        iconPackPackage: String? = null
    ): ImageBitmap? {
        return getCached(buildCacheKey(packageName, activityName, iconStyle, iconPackPackage))
    }

    /**
     * Returns the cached [ImageBitmap] for a package's own launcher/app icon (used for icon pack rows).
     */
    fun getCachedPackageIcon(packageName: String): ImageBitmap? {
        return getCached("appicon|$packageName")
    }

    /**
     * Loads and caches the icon for the specified component on [Dispatchers.IO].
     */
    suspend fun loadIcon(
        packageName: String,
        activityName: String,
        targetSizePx: Int = DEFAULT_ICON_SIZE_PX,
        iconStyle: IconStyle = IconStyle.IOS_SQUIRCLE,
        iconPackPackage: String? = null
    ): ImageBitmap? {
        val normalizedPack = iconPackPackage?.trim()?.takeIf { it.isNotEmpty() }
        val key = buildCacheKey(packageName, activityName, iconStyle, normalizedPack)
        getCached(key)?.let { return it }

        val safeSizePx = targetSizePx.coerceIn(MIN_ICON_SIZE_PX, MAX_ICON_SIZE_PX)
        val deferred = inFlight.computeIfAbsent(key) {
            scope.async {
                try {
                    val themedBitmap = if (normalizedPack != null && iconPackManager != null) {
                        iconPackManager.loadThemedBitmap(
                            packPackageName = normalizedPack,
                            appPackageName = packageName,
                            activityName = activityName,
                            fallbackDrawable = { loadRawComponentDrawable(packageName, activityName) },
                            targetSizePx = safeSizePx
                        )
                    } else {
                        null
                    }

                    val androidBitmap = themedBitmap ?: renderSystemIconBitmap(
                        drawable = loadRawComponentDrawable(packageName, activityName),
                        sizePx = safeSizePx,
                        iconStyle = iconStyle
                    )
                    val imageBitmap = androidBitmap.asImageBitmap()
                    val sizeKb = (androidBitmap.allocationByteCount / 1024).coerceAtLeast(1)
                    cache.put(key, CachedIcon(bitmap = imageBitmap, sizeKb = sizeKb))
                    imageBitmap
                } catch (_: Exception) {
                    null
                } finally {
                    inFlight.remove(key)
                }
            }
        }

        return deferred.await()
    }

    /**
     * Loads and caches an application's top-level icon (used to display third-party Icon Packs).
     */
    suspend fun loadPackageIcon(
        packageName: String,
        targetSizePx: Int = DEFAULT_ICON_SIZE_PX
    ): ImageBitmap? {
        val key = "appicon|$packageName"
        getCached(key)?.let { return it }

        val safeSizePx = targetSizePx.coerceIn(MIN_ICON_SIZE_PX, MAX_ICON_SIZE_PX)
        val deferred = inFlight.computeIfAbsent(key) {
            scope.async {
                try {
                    val drawable = try {
                        packageManager.getApplicationIcon(packageName)
                    } catch (_: Exception) {
                        packageManager.defaultActivityIcon
                    }
                    val androidBitmap = renderSystemIconBitmap(
                        drawable = drawable,
                        sizePx = safeSizePx,
                        iconStyle = IconStyle.IOS_SQUIRCLE
                    )
                    val imageBitmap = androidBitmap.asImageBitmap()
                    val sizeKb = (androidBitmap.allocationByteCount / 1024).coerceAtLeast(1)
                    cache.put(key, CachedIcon(bitmap = imageBitmap, sizeKb = sizeKb))
                    imageBitmap
                } catch (_: Exception) {
                    null
                } finally {
                    inFlight.remove(key)
                }
            }
        }

        return deferred.await()
    }

    private fun loadRawComponentDrawable(packageName: String, activityName: String): Drawable {
        return try {
            packageManager.getActivityIcon(ComponentName(packageName, activityName))
        } catch (_: Exception) {
            try {
                packageManager.getApplicationIcon(packageName)
            } catch (_: Exception) {
                packageManager.defaultActivityIcon
            }
        }
    }

    private fun renderSystemIconBitmap(
        drawable: Drawable,
        sizePx: Int,
        iconStyle: IconStyle
    ): Bitmap {
        if (iconStyle != IconStyle.ADAPTIVE &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            drawable is AdaptiveIconDrawable
        ) {
            val background = drawable.background
            val foreground = drawable.foreground
            if (background != null || foreground != null) {
                val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                val bleed = (sizePx * ADAPTIVE_BLEED_FRACTION).roundToInt()
                val left = -bleed
                val top = -bleed
                val right = sizePx + bleed
                val bottom = sizePx + bleed

                background?.let { bg ->
                    val copy = bg.constantState?.newDrawable()?.mutate() ?: bg
                    copy.setBounds(left, top, right, bottom)
                    copy.draw(canvas)
                }
                foreground?.let { fg ->
                    val copy = fg.constantState?.newDrawable()?.mutate() ?: fg
                    copy.setBounds(left, top, right, bottom)
                    copy.draw(canvas)
                }
                return bitmap
            }
        }

        return drawable.toBitmap(
            width = sizePx,
            height = sizePx,
            config = Bitmap.Config.ARGB_8888
        )
    }

    /**
     * Evicts all cached icons and cancels in-flight loads belonging to [packageName].
     */
    fun evictPackage(packageName: String) {
        val keysToRemove = cache.snapshot().keys.filter { key ->
            keyMatchesPackage(key, packageName)
        }
        for (key in keysToRemove) {
            cache.remove(key)
        }
        inFlight.keys.filter { key -> keyMatchesPackage(key, packageName) }.forEach { key ->
            inFlight.remove(key)?.cancel()
        }
        iconPackManager?.evictPack(packageName)
    }

    private fun keyMatchesPackage(key: String, packageName: String): Boolean {
        return key.endsWith("|$packageName") ||
            key.contains("|$packageName/") ||
            key.startsWith("pack:$packageName|")
    }

    /**
     * Clears the entire icon cache (e.g. on memory pressure or full reload).
     */
    fun clear() {
        cache.evictAll()
        iconPackManager?.clear()
    }

    companion object {
        private const val MIN_CACHE_KB = 4 * 1024
        private const val MAX_CACHE_KB = 24 * 1024
        private const val DEFAULT_ICON_SIZE_PX = 144
        private const val MIN_ICON_SIZE_PX = 48
        private const val MAX_ICON_SIZE_PX = 256
        private const val ADAPTIVE_BLEED_FRACTION = 0.25f

        /**
         * Builds a deterministic cache key isolating themed packs and adaptive vs full-bleed layers.
         * [IconStyle.IOS_SQUIRCLE] and [IconStyle.CIRCLE] share the full-bleed bitmap so contour
         * switching is zero-latency.
         */
        fun buildCacheKey(
            packageName: String,
            activityName: String,
            iconStyle: IconStyle = IconStyle.IOS_SQUIRCLE,
            iconPackPackage: String? = null
        ): String {
            val normalizedPack = iconPackPackage?.trim()?.takeIf { it.isNotEmpty() }
            if (normalizedPack != null) {
                return "pack:$normalizedPack|$packageName/$activityName"
            }
            val styleBucket = if (iconStyle == IconStyle.ADAPTIVE) "adaptive" else "fullbleed"
            return "$styleBucket|$packageName/$activityName"
        }
    }
}
