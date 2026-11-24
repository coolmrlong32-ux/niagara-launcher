package com.niagara.launcher

import com.niagara.launcher.ui.home.components.AlphabetWaveMath
import com.niagara.launcher.ui.home.components.EdgeGestureIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlphabetWaveMathTest {

    @Test
    fun testYToLetterIndexClampsBoundsSafely() {
        assertNull(AlphabetWaveMath.yToLetterIndex(10f, 0f, 27))
        assertNull(AlphabetWaveMath.yToLetterIndex(10f, 540f, 0))

        // 540px / 27 letters = 20px per letter
        assertEquals(0, AlphabetWaveMath.yToLetterIndex(-50f, 540f, 27))
        assertEquals(0, AlphabetWaveMath.yToLetterIndex(0f, 540f, 27))
        assertEquals(1, AlphabetWaveMath.yToLetterIndex(25f, 540f, 27))
        assertEquals(26, AlphabetWaveMath.yToLetterIndex(539f, 540f, 27))
        assertEquals(26, AlphabetWaveMath.yToLetterIndex(999f, 540f, 27))
    }

    @Test
    fun testGaussianSymmetryAndPeak() {
        val peak = AlphabetWaveMath.calculateGaussian(0f, 40f)
        assertEquals(1.0f, peak, 0.0001f)

        val left = AlphabetWaveMath.calculateGaussian(30f, 40f)
        val right = AlphabetWaveMath.calculateGaussian(-30f, 40f)
        assertEquals(left, right, 0.0001f)
        assertTrue(left in 0.0f..1.0f)
    }

    @Test
    fun testBubbleOffsetYNeverOverflowsContainer() {
        // Total height 500px, bubble 52px -> max offset is 448px
        assertEquals(0f, AlphabetWaveMath.calculateBubbleOffsetY(-10f, 500f, 52f), 0.01f)
        assertEquals(0f, AlphabetWaveMath.calculateBubbleOffsetY(10f, 500f, 52f), 0.01f)
        assertEquals(224f, AlphabetWaveMath.calculateBubbleOffsetY(250f, 500f, 52f), 0.01f)
        assertEquals(448f, AlphabetWaveMath.calculateBubbleOffsetY(490f, 500f, 52f), 0.01f)
        assertEquals(448f, AlphabetWaveMath.calculateBubbleOffsetY(600f, 500f, 52f), 0.01f)
    }

    @Test
    fun testErgonomicBandStaysInMiddleLowerOneHandedZone() {
        val band = AlphabetWaveMath.calculateErgonomicBand(
            containerHeightPx = 2200f,
            minBandHeightPx = 900f,
            maxBandHeightPx = 1260f,
            heightFraction = 0.56f,
            verticalBias = 0.64f,
            topSafeMarginPx = 200f,
            bottomSafeMarginPx = 150f
        )

        // Available height = 1850px, 56% = 1036px (within [900, 1260])
        assertEquals(1036f, band.bandHeightPx, 1f)
        // Positioned in middle-lower zone (below center bias) and never exceeding bottom safe margin
        assertTrue(band.topOffsetPx > 600f)
        assertTrue(band.topOffsetPx + band.bandHeightPx <= 2050f + 0.5f)
    }

    @Test
    fun testErgonomicBandDegradesGracefullyOnShortScreens() {
        val compact = AlphabetWaveMath.calculateErgonomicBand(
            containerHeightPx = 600f,
            minBandHeightPx = 900f,
            maxBandHeightPx = 1260f,
            topSafeMarginPx = 60f,
            bottomSafeMarginPx = 40f
        )

        assertEquals(500f, compact.bandHeightPx, 0.5f)
        assertEquals(60f, compact.topOffsetPx, 0.5f)
        assertEquals(0f, AlphabetWaveMath.calculateErgonomicBand(0f, 300f, 400f).bandHeightPx, 0.01f)
    }

    @Test
    fun testLeftEdgeGestureDisambiguatesVerticalScrubFromHorizontalSwipe() {
        // Below slop stays undecided (allows normal taps)
        assertEquals(
            EdgeGestureIntent.UNDECIDED,
            AlphabetWaveMath.classifyEdgeGesture(dx = 3f, dy = 4f, lockSlopPx = 14f)
        )

        // Clear vertical drag locks invisible alphabet scrubbing
        assertEquals(
            EdgeGestureIntent.VERTICAL_ALPHABET,
            AlphabetWaveMath.classifyEdgeGesture(dx = 6f, dy = 24f, lockSlopPx = 14f)
        )
        assertEquals(
            EdgeGestureIntent.VERTICAL_ALPHABET,
            AlphabetWaveMath.classifyEdgeGesture(dx = -5f, dy = -20f, lockSlopPx = 14f)
        )

        // Horizontal swipe (page swipe or system edge-back) passes through
        assertEquals(
            EdgeGestureIntent.HORIZONTAL_PASS_THROUGH,
            AlphabetWaveMath.classifyEdgeGesture(dx = 22f, dy = 8f, lockSlopPx = 14f)
        )
        // Diagonal slight swipe yields to horizontal paging for safety
        assertEquals(
            EdgeGestureIntent.HORIZONTAL_PASS_THROUGH,
            AlphabetWaveMath.classifyEdgeGesture(dx = 16f, dy = 16f, lockSlopPx = 14f)
        )
    }
}
