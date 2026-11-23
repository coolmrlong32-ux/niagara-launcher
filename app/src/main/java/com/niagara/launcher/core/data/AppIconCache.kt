package com.niagara.launcher.core.data

import android.content.ComponentName
import android.content.pm.PackageManager
import android.util.LruCache
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import java.util.concurrent.ConcurrentHashMap

val LocalAppIconCache = staticCompositionLocalOf<AppIconCache?> { null }

/**
 * Thread-safe in-memory LRU cache for asynchronously decoded application icons.
 * Deduplicates concurrent requests for the same component and supports per-package eviction.
 */
class AppIconCache(
    private val packageManager: PackageManager
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
     * Returns the cached [ImageBitmap] synchronously if available, or null on cache miss.
     */
    fun getCached(key: String): ImageBitmap? = cache.get(key)?.bitmap

    /**
     * Loads and caches the icon for the specified component on [Dispatchers.IO].
     */
    suspend fun loadIcon(
        packageName: String,
        activityName: String,
        targetSizePx: Int = DEFAULT_ICON_SIZE_PX
    ): ImageBitmap? {
        val key = "$packageName/$activityName"
        getCached(key)?.let { return it }

        val safeSizePx = targetSizePx.coerceIn(MIN_ICON_SIZE_PX, MAX_ICON_SIZE_PX)
        val deferred = inFlight.computeIfAbsent(key) {
            scope.async {
                try {
                    val drawable = try {
                        packageManager.getActivityIcon(ComponentName(packageName, activityName))
                    } catch (_: Exception) {
                        try {
                            packageManager.getApplicationIcon(packageName)
                        } catch (_: Exception) {
                            packageManager.defaultActivityIcon
                        }
                    }

                    val androidBitmap = drawable.toBitmap(width = safeSizePx, height = safeSizePx)
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
     * Evicts all cached icons and cancels in-flight loads belonging to [packageName].
     */
    fun evictPackage(packageName: String) {
        val prefix = "$packageName/"
        val keysToRemove = cache.snapshot().keys.filter { it.startsWith(prefix) }
        for (key in keysToRemove) {
            cache.remove(key)
        }
        inFlight.keys.filter { it.startsWith(prefix) }.forEach { key ->
            inFlight.remove(key)?.cancel()
        }
    }

    /**
     * Clears the entire icon cache (e.g. on memory pressure or full reload).
     */
    fun clear() {
        cache.evictAll()
    }

    companion object {
        private const val MIN_CACHE_KB = 4 * 1024
        private const val MAX_CACHE_KB = 24 * 1024
        private const val DEFAULT_ICON_SIZE_PX = 144
        private const val MIN_ICON_SIZE_PX = 48
        private const val MAX_ICON_SIZE_PX = 256
    }
}
