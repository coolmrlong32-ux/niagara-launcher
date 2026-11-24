package com.niagara.launcher.core.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Build
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Immutable
data class SystemWidgetProviderEntry(
    val providerPackage: String,
    val providerClass: String,
    val appLabel: String,
    val widgetLabel: String,
    val minWidthDp: Int,
    val minHeightDp: Int,
    val recommendedHeightDp: Int,
    val cellSpanLabel: String
)

/**
 * Discovers installed Android AppWidget providers and calculates sensible home-screen spans.
 */
object SystemWidgetManager {

    suspend fun loadInstalledWidgets(context: Context): List<SystemWidgetProviderEntry> =
        withContext(Dispatchers.IO) {
            val manager = try {
                AppWidgetManager.getInstance(context)
            } catch (_: Exception) {
                null
            } ?: return@withContext emptyList()

            val pm = context.packageManager
            val providers = try {
                manager.installedProviders.orEmpty()
            } catch (_: Exception) {
                emptyList()
            }

            providers.mapNotNull { info ->
                val provider = info.provider ?: return@mapNotNull null
                val category = info.widgetCategory
                if (category != 0 && (category and AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN) == 0) {
                    return@mapNotNull null
                }

                val pkg = provider.packageName
                val cls = provider.className
                val appLabel = try {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    pm.getApplicationLabel(appInfo).toString().ifBlank { pkg }
                } catch (_: Exception) {
                    pkg
                }
                val widgetLabel = try {
                    info.loadLabel(pm).orEmpty().ifBlank { cls.substringAfterLast('.') }
                } catch (_: Exception) {
                    cls.substringAfterLast('.')
                }

                val minW = info.minWidth.coerceAtLeast(40)
                val minH = info.minHeight.coerceAtLeast(40)
                val cols = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && info.targetCellWidth > 0) {
                    info.targetCellWidth.coerceIn(1, 5)
                } else {
                    ((minW + 30) / 70).coerceIn(1, 5)
                }
                val rows = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && info.targetCellHeight > 0) {
                    info.targetCellHeight.coerceIn(1, 5)
                } else {
                    ((minH + 30) / 70).coerceIn(1, 5)
                }

                val recommendedHeight = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && info.targetCellHeight > 0) {
                    (info.targetCellHeight * 80).coerceIn(104, 260)
                } else {
                    minH.coerceIn(104, 260)
                }

                SystemWidgetProviderEntry(
                    providerPackage = pkg,
                    providerClass = cls,
                    appLabel = appLabel,
                    widgetLabel = widgetLabel,
                    minWidthDp = minW,
                    minHeightDp = minH,
                    recommendedHeightDp = recommendedHeight,
                    cellSpanLabel = "${cols}×${rows}"
                )
            }.sortedWith(compareBy({ it.appLabel.lowercase() }, { it.widgetLabel.lowercase() }))
        }

    fun findProviderInfo(
        context: Context,
        providerPackage: String,
        providerClass: String
    ): AppWidgetProviderInfo? {
        val manager = try {
            AppWidgetManager.getInstance(context)
        } catch (_: Exception) {
            null
        } ?: return null
        return try {
            manager.installedProviders.orEmpty().firstOrNull { info ->
                val provider = info.provider
                provider != null &&
                    provider.packageName == providerPackage &&
                    provider.className == providerClass
            }
        } catch (_: Exception) {
            null
        }
    }
}
