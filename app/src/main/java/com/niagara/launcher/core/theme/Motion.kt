package com.niagara.launcher.core.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.Dp

/**
 * Unified damped spring motion curves shared across the launcher UI.
 */
object LauncherMotion {
    val SnappySpring: SpringSpec<Float> = spring(
        dampingRatio = 0.78f,
        stiffness = 460f
    )

    val FluidSpring: SpringSpec<Float> = spring(
        dampingRatio = 0.82f,
        stiffness = 340f
    )

    val GentleSettleSpring: SpringSpec<Float> = spring(
        dampingRatio = 0.86f,
        stiffness = 280f
    )

    val WaveExpandSpring: SpringSpec<Float> = spring(
        dampingRatio = 0.68f,
        stiffness = 380f
    )

    val WaveReboundSpring: SpringSpec<Float> = spring(
        dampingRatio = 0.84f,
        stiffness = 420f
    )

    val DpFluidSpring: SpringSpec<Dp> = spring(
        dampingRatio = 0.82f,
        stiffness = 340f
    )
}
