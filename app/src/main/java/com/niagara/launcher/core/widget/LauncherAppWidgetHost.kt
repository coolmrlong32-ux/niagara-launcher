package com.niagara.launcher.core.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.SizeF

/**
 * Custom [AppWidgetHost] enabling third-party and system Android AppWidgets on Page 0.
 */
class LauncherAppWidgetHost(
    context: Context,
    hostId: Int = HOST_ID
) : AppWidgetHost(context, hostId) {

    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?
    ): AppWidgetHostView {
        return LauncherAppWidgetHostView(context)
    }

    companion object {
        const val HOST_ID = 0x4E494147
    }
}

/**
 * HostView wrapper that cleanly updates widget size hints across API 26–35.
 */
class LauncherAppWidgetHostView(context: Context) : AppWidgetHostView(context) {

    private var lastAppliedWidthDp: Int = -1
    private var lastAppliedHeightDp: Int = -1

    fun applySizeDp(widthDp: Int, heightDp: Int) {
        if (widthDp <= 0 || heightDp <= 0) return
        if (widthDp == lastAppliedWidthDp && heightDp == lastAppliedHeightDp) return
        lastAppliedWidthDp = widthDp
        lastAppliedHeightDp = heightDp

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                updateAppWidgetSize(
                    Bundle.EMPTY,
                    listOf(SizeF(widthDp.toFloat(), heightDp.toFloat()))
                )
            } else {
                @Suppress("DEPRECATION")
                updateAppWidgetSize(
                    Bundle.EMPTY,
                    widthDp,
                    heightDp,
                    widthDp,
                    heightDp
                )
            }
        } catch (_: Exception) {
            // Safe guard if remote widget provider rejects size update
        }
    }
}
