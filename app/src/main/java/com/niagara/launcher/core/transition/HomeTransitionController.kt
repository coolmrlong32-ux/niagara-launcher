package com.niagara.launcher.core.transition

import kotlin.math.roundToInt

/**
 * Pixel bounds of an app icon or list row in window coordinates, used to anchor
 * [android.app.ActivityOptions.makeClipRevealAnimation] / [android.app.ActivityOptions.makeScaleUpAnimation].
 */
data class LaunchSourceBounds(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int
) {
    /**
     * Clamps bounds safely within a host view's dimensions so ActivityOptions never receives
     * negative sizes or out-of-range coordinates on strict OEM ROMs.
     */
    fun toClampedRelativeBounds(hostWidth: Int, hostHeight: Int): LaunchSourceBounds? {
        if (hostWidth <= 0 || hostHeight <= 0) return null
        val safeWidth = width.coerceIn(1, hostWidth)
        val safeHeight = height.coerceIn(1, hostHeight)
        val maxLeft = (hostWidth - safeWidth).coerceAtLeast(0)
        val maxTop = (hostHeight - safeHeight).coerceAtLeast(0)
        return LaunchSourceBounds(
            left = left.coerceIn(0, maxLeft),
            top = top.coerceIn(0, maxTop),
            width = safeWidth,
            height = safeHeight
        )
    }

    companion object {
        fun fromCoordinates(
            x: Float,
            y: Float,
            width: Float,
            height: Float
        ): LaunchSourceBounds? {
            if (!x.isFinite() || !y.isFinite() || !width.isFinite() || !height.isFinite()) {
                return null
            }
            val w = width.roundToInt()
            val h = height.roundToInt()
            if (w <= 0 || h <= 0) return null
            return LaunchSourceBounds(
                left = x.roundToInt(),
                top = y.roundToInt(),
                width = w,
                height = h
            )
        }
    }
}

/**
 * Lightweight mutable holder to store layout bounds during onGloballyPositioned
 * without triggering Compose recomposition.
 */
class LaunchBoundsRef(var bounds: LaunchSourceBounds? = null)

/**
 * Debounced state machine guarding the launcher's return-to-home spring transition.
 *
 * Guarantees:
 * - Never fires on initial cold start (instant first paint)
 * - Never fires on in-app dialog / modal bottom sheet dismiss (which does not invoke onStop)
 * - Collapses back-to-back onNewIntent + onResume + onWindowFocusChanged into a single animation
 */
class HomeReturnAnimationGuard(
    private val minCooldownMs: Long = DEFAULT_COOLDOWN_MS,
    private val clockMs: () -> Long = { System.currentTimeMillis() }
) {
    private var wasBackgrounded: Boolean = false
    private var lastTriggeredAtMs: Long = Long.MIN_VALUE

    fun markBackgrounded() {
        wasBackgrounded = true
    }

    fun tryConsumeReturnTrigger(): Boolean {
        if (!wasBackgrounded) return false
        val now = clockMs()
        val elapsed = if (lastTriggeredAtMs == Long.MIN_VALUE) Long.MAX_VALUE else now - lastTriggeredAtMs
        wasBackgrounded = false
        if (elapsed < minCooldownMs) {
            return false
        }
        lastTriggeredAtMs = now
        return true
    }

    companion object {
        const val DEFAULT_COOLDOWN_MS = 340L
    }
}
