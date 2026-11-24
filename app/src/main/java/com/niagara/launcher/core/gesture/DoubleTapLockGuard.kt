package com.niagara.launcher.core.gesture

import kotlin.math.hypot

/**
 * Outcome of evaluating a single tap on genuine empty launcher background space.
 */
enum class TapOutcome {
    IGNORED,
    FIRST_TAP_RECORDED,
    DOUBLE_TAP_LOCK
}

/**
 * Strict anti-accidental-touch state machine for empty-space double-tap screen lock.
 *
 * Guarantees:
 * - Never fires when disabled, in edit mode, while dragging, or while an overlay is open.
 * - Rejects sub-45ms bounce chatter and taps separated by more than [maxIntervalMs] or [maxDistancePx].
 * - Enforces a post-lock cooldown so rapid triple taps cannot re-fire on wakeup.
 */
class DoubleTapLockGuard(
    private val minIntervalMs: Long = DEFAULT_MIN_INTERVAL_MS,
    private val maxIntervalMs: Long = DEFAULT_MAX_INTERVAL_MS,
    private val lockCooldownMs: Long = DEFAULT_LOCK_COOLDOWN_MS
) {
    private var lastTapX: Float = Float.NaN
    private var lastTapY: Float = Float.NaN
    private var lastTapTimeMs: Long = Long.MIN_VALUE
    private var lastLockTimeMs: Long = Long.MIN_VALUE

    fun onEmptySpaceTap(
        x: Float,
        y: Float,
        maxDistancePx: Float,
        nowMs: Long,
        enabled: Boolean,
        isBlockedByMode: Boolean
    ): TapOutcome {
        if (!enabled || isBlockedByMode || !x.isFinite() || !y.isFinite() || !maxDistancePx.isFinite() || maxDistancePx <= 0f) {
            resetPendingTap()
            return TapOutcome.IGNORED
        }

        if (lastLockTimeMs != Long.MIN_VALUE && (nowMs - lastLockTimeMs) in 0 until lockCooldownMs) {
            resetPendingTap()
            return TapOutcome.IGNORED
        }

        if (lastTapTimeMs != Long.MIN_VALUE) {
            val dt = nowMs - lastTapTimeMs
            if (dt in 0 until minIntervalMs) {
                // Ignore accidental bounce chatter without triggering or overwriting the first tap
                return TapOutcome.IGNORED
            }
            if (dt in minIntervalMs..maxIntervalMs) {
                val distance = hypot((x - lastTapX).toDouble(), (y - lastTapY).toDouble())
                if (distance <= maxDistancePx.toDouble()) {
                    lastLockTimeMs = nowMs
                    resetPendingTap()
                    return TapOutcome.DOUBLE_TAP_LOCK
                }
            }
        }

        lastTapX = x
        lastTapY = y
        lastTapTimeMs = nowMs
        return TapOutcome.FIRST_TAP_RECORDED
    }

    fun reset() {
        resetPendingTap()
    }

    private fun resetPendingTap() {
        lastTapX = Float.NaN
        lastTapY = Float.NaN
        lastTapTimeMs = Long.MIN_VALUE
    }

    companion object {
        const val DEFAULT_MIN_INTERVAL_MS = 45L
        const val DEFAULT_MAX_INTERVAL_MS = 280L
        const val DEFAULT_LOCK_COOLDOWN_MS = 800L
    }
}
