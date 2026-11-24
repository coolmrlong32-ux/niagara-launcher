package com.niagara.launcher

import com.niagara.launcher.core.gesture.DoubleTapLockGuard
import com.niagara.launcher.core.gesture.TapOutcome
import org.junit.Assert.assertEquals
import org.junit.Test

class DoubleTapLockGuardTest {

    @Test
    fun testValidDoubleTapLocksAndEnforcesPostLockCooldown() {
        val guard = DoubleTapLockGuard()

        assertEquals(
            TapOutcome.FIRST_TAP_RECORDED,
            guard.onEmptySpaceTap(x = 300f, y = 500f, maxDistancePx = 56f, nowMs = 1_000L, enabled = true, isBlockedByMode = false)
        )
        assertEquals(
            TapOutcome.DOUBLE_TAP_LOCK,
            guard.onEmptySpaceTap(x = 312f, y = 510f, maxDistancePx = 56f, nowMs = 1_160L, enabled = true, isBlockedByMode = false)
        )

        // Immediate taps within 800ms post-lock cooldown are ignored
        assertEquals(
            TapOutcome.IGNORED,
            guard.onEmptySpaceTap(x = 300f, y = 500f, maxDistancePx = 56f, nowMs = 1_350L, enabled = true, isBlockedByMode = false)
        )

        // After cooldown elapses, double-tap works again
        assertEquals(
            TapOutcome.FIRST_TAP_RECORDED,
            guard.onEmptySpaceTap(x = 300f, y = 500f, maxDistancePx = 56f, nowMs = 2_100L, enabled = true, isBlockedByMode = false)
        )
        assertEquals(
            TapOutcome.DOUBLE_TAP_LOCK,
            guard.onEmptySpaceTap(x = 305f, y = 505f, maxDistancePx = 56f, nowMs = 2_250L, enabled = true, isBlockedByMode = false)
        )
    }

    @Test
    fun testSubMinIntervalBounceAndSlowOrFarTapsDoNotLock() {
        val guard = DoubleTapLockGuard()

        // First tap
        assertEquals(
            TapOutcome.FIRST_TAP_RECORDED,
            guard.onEmptySpaceTap(x = 200f, y = 200f, maxDistancePx = 50f, nowMs = 1_000L, enabled = true, isBlockedByMode = false)
        )
        // Sub-45ms hardware bounce is ignored without firing lock
        assertEquals(
            TapOutcome.IGNORED,
            guard.onEmptySpaceTap(x = 201f, y = 201f, maxDistancePx = 50f, nowMs = 1_020L, enabled = true, isBlockedByMode = false)
        )
        // Too slow (> 280ms) restarts sequence as first tap
        assertEquals(
            TapOutcome.FIRST_TAP_RECORDED,
            guard.onEmptySpaceTap(x = 202f, y = 202f, maxDistancePx = 50f, nowMs = 1_350L, enabled = true, isBlockedByMode = false)
        )
        // Too far (> maxDistancePx) restarts sequence as first tap
        assertEquals(
            TapOutcome.FIRST_TAP_RECORDED,
            guard.onEmptySpaceTap(x = 400f, y = 202f, maxDistancePx = 50f, nowMs = 1_480L, enabled = true, isBlockedByMode = false)
        )
    }

    @Test
    fun testDisabledBlockedAndResetClearPendingTap() {
        val guard = DoubleTapLockGuard()

        assertEquals(
            TapOutcome.FIRST_TAP_RECORDED,
            guard.onEmptySpaceTap(x = 100f, y = 100f, maxDistancePx = 50f, nowMs = 1_000L, enabled = true, isBlockedByMode = false)
        )
        guard.reset()
        // After reset, next tap starts fresh instead of locking
        assertEquals(
            TapOutcome.FIRST_TAP_RECORDED,
            guard.onEmptySpaceTap(x = 100f, y = 100f, maxDistancePx = 50f, nowMs = 1_120L, enabled = true, isBlockedByMode = false)
        )

        // Blocked by edit mode / dragging / overlay
        assertEquals(
            TapOutcome.IGNORED,
            guard.onEmptySpaceTap(x = 100f, y = 100f, maxDistancePx = 50f, nowMs = 1_220L, enabled = true, isBlockedByMode = true)
        )
        // Disabled
        assertEquals(
            TapOutcome.IGNORED,
            guard.onEmptySpaceTap(x = 100f, y = 100f, maxDistancePx = 50f, nowMs = 1_320L, enabled = false, isBlockedByMode = false)
        )
    }
}
