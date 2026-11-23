package com.niagara.launcher

import com.niagara.launcher.ui.home.components.AlphabetWaveMath
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
}
