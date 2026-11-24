package com.niagara.launcher

import com.niagara.launcher.core.transition.HomeReturnAnimationGuard
import com.niagara.launcher.core.transition.LaunchSourceBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeTransitionLogicTest {

    @Test
    fun testReturnGuardIgnoresColdStartAndDeduplicatesBurstLifecycleCallbacks() {
        var nowMs = 1_000L
        val guard = HomeReturnAnimationGuard(
            minCooldownMs = 340L,
            clockMs = { nowMs }
        )

        // Cold start: onResume + onWindowFocusChanged should NOT animate before any backgrounding
        assertFalse(guard.tryConsumeReturnTrigger())
        assertFalse(guard.tryConsumeReturnTrigger())

        // User launches an app -> onStop marks backgrounded
        guard.markBackgrounded()

        // User returns home: onNewIntent, onResume, onWindowFocusChanged fire back-to-back
        nowMs += 500L
        assertTrue(guard.tryConsumeReturnTrigger()) // onNewIntent triggers once
        assertFalse(guard.tryConsumeReturnTrigger()) // onResume suppressed
        assertFalse(guard.tryConsumeReturnTrigger()) // onWindowFocusChanged suppressed

        // Re-backgrounded quickly within cooldown -> still suppressed to avoid jarring double bounce
        guard.markBackgrounded()
        nowMs += 100L
        assertFalse(guard.tryConsumeReturnTrigger())

        // After cooldown elapses and user returns from another app -> triggers cleanly again
        guard.markBackgrounded()
        nowMs += 400L
        assertTrue(guard.tryConsumeReturnTrigger())
    }

    @Test
    fun testLaunchSourceBoundsClampsSafelyWithinHostView() {
        assertNull(LaunchSourceBounds.fromCoordinates(10f, 10f, 0f, 100f))
        assertNull(LaunchSourceBounds.fromCoordinates(Float.NaN, 10f, 50f, 50f))

        val raw = LaunchSourceBounds.fromCoordinates(-20f, 2350f, 160f, 160f)
        assertNotNull(raw)

        val clamped = raw!!.toClampedRelativeBounds(hostWidth = 1080, hostHeight = 2400)
        assertEquals(LaunchSourceBounds(left = 0, top = 2240, width = 160, height = 160), clamped)
        assertNull(raw.toClampedRelativeBounds(hostWidth = 0, hostHeight = 2400))
    }
}
