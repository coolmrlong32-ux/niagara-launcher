package com.niagara.launcher

import com.niagara.launcher.core.transition.LauncherIntentAction
import com.niagara.launcher.core.transition.LauncherIntentRouter
import com.niagara.launcher.ui.home.components.ClockFormatHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

class ClockAndIntentRouterTest {

    @Test
    fun testClockFormatHelperZeroPads24HourTimeAndLocalizesDates() {
        // 2026-11-24T04:07:09Z
        val epochMillis = 1795493229000L
        val utc = TimeZone.getTimeZone("UTC")

        val zhSnapshot = ClockFormatHelper.format(
            epochMillis = epochMillis,
            locale = Locale.SIMPLIFIED_CHINESE,
            timeZone = utc
        )
        assertEquals("04", zhSnapshot.hourText)
        assertEquals("07", zhSnapshot.minuteText)
        assertEquals("09", zhSnapshot.secondText)
        assertEquals("11月24日", zhSnapshot.dateText)
        assertTrue(zhSnapshot.weekdayText.isNotBlank())

        val enSnapshot = ClockFormatHelper.format(
            epochMillis = epochMillis,
            locale = Locale.US,
            timeZone = utc
        )
        assertEquals("04", enSnapshot.hourText)
        assertEquals("07", enSnapshot.minuteText)
        assertEquals("09", enSnapshot.secondText)
        assertEquals("Nov 24", enSnapshot.dateText)
        assertEquals("Tuesday", enSnapshot.weekdayText)
    }

    @Test
    fun testLauncherIntentRouterDistinguishesHomeAndLauncherTaps() {
        assertEquals(
            LauncherIntentAction.RETURN_HOME_PAGE,
            LauncherIntentRouter.resolveOnNewIntent(
                hasHomeCategory = true,
                hasLauncherCategory = false,
                isAlreadyActive = true
            )
        )
        assertEquals(
            LauncherIntentAction.OPEN_SETTINGS,
            LauncherIntentRouter.resolveOnNewIntent(
                hasHomeCategory = false,
                hasLauncherCategory = true,
                isAlreadyActive = true
            )
        )
        assertEquals(
            LauncherIntentAction.STAY_HOME,
            LauncherIntentRouter.resolveOnNewIntent(
                hasHomeCategory = false,
                hasLauncherCategory = true,
                isAlreadyActive = false
            )
        )
        assertEquals(
            LauncherIntentAction.STAY_HOME,
            LauncherIntentRouter.resolveOnNewIntent(
                hasHomeCategory = false,
                hasLauncherCategory = false,
                isAlreadyActive = true
            )
        )
    }
}
