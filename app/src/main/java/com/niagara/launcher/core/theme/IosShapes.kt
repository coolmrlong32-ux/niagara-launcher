package com.niagara.launcher.core.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Pure Kotlin math helpers for iOS G2 continuous-curvature squircle geometry.
 */
object IosSquircleMath {
    private const val MIN_CORNER_FRACTION = 0.05f
    private const val MAX_CORNER_FRACTION = 0.45f
    private const val TANGENT_EXTENSION_FACTOR = 1.528665f
    private const val BEZIER_CONTROL_FACTOR = 0.447715f

    fun effectiveCornerRadius(width: Float, height: Float, cornerFraction: Float): Float {
        if (width <= 0f || height <= 0f) return 0f
        val minDim = minOf(width, height)
        val clampedFraction = cornerFraction.coerceIn(MIN_CORNER_FRACTION, MAX_CORNER_FRACTION)
        return (minDim * clampedFraction).coerceAtMost(minDim / 2f)
    }

    fun smoothTangentLength(radius: Float, width: Float, height: Float): Float {
        if (radius <= 0f || width <= 0f || height <= 0f) return 0f
        val minHalf = minOf(width, height) / 2f
        return minOf(radius * TANGENT_EXTENSION_FACTOR, minHalf)
    }

    fun cubicControlOffset(radius: Float): Float {
        if (radius <= 0f) return 0f
        return radius * BEZIER_CONTROL_FACTOR
    }
}

private fun buildSquirclePath(width: Float, height: Float, radius: Float): Path {
    val path = Path()
    if (width <= 0f || height <= 0f) return path

    val r = radius.coerceIn(0f, minOf(width, height) / 2f)
    val p = IosSquircleMath.smoothTangentLength(r, width, height)
    val c = (p - IosSquircleMath.cubicControlOffset(r)).coerceAtLeast(r * 0.55f)
    // Diagonal transition anchor approximating Apple G2 superellipse corner curvature
    val d = r * 0.292893f

    // Start at top edge after top-left corner
    path.moveTo(p, 0f)

    // Top edge -> Top-right corner
    path.lineTo(width - p, 0f)
    path.cubicTo(
        width - c, 0f,
        width - r * 0.52f, r * 0.08f,
        width - d, d
    )
    path.cubicTo(
        width - r * 0.08f, r * 0.52f,
        width, c,
        width, p
    )

    // Right edge -> Bottom-right corner
    path.lineTo(width, height - p)
    path.cubicTo(
        width, height - c,
        width - r * 0.08f, height - r * 0.52f,
        width - d, height - d
    )
    path.cubicTo(
        width - r * 0.52f, height - r * 0.08f,
        width - c, height,
        width - p, height
    )

    // Bottom edge -> Bottom-left corner
    path.lineTo(p, height)
    path.cubicTo(
        c, height,
        r * 0.52f, height - r * 0.08f,
        d, height - d
    )
    path.cubicTo(
        r * 0.08f, height - r * 0.52f,
        0f, height - c,
        0f, height - p
    )

    // Left edge -> Top-left corner
    path.lineTo(0f, p)
    path.cubicTo(
        0f, c,
        r * 0.08f, r * 0.52f,
        d, d
    )
    path.cubicTo(
        r * 0.52f, r * 0.08f,
        c, 0f,
        p, 0f
    )

    path.close()
    return path
}

/**
 * iOS continuous-curvature squircle shape for app icons.
 */
class IosSquircleShape(
    private val cornerFraction: Float = 0.225f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val radius = IosSquircleMath.effectiveCornerRadius(size.width, size.height, cornerFraction)
        return Outline.Generic(buildSquirclePath(size.width, size.height, radius))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IosSquircleShape) return false
        return cornerFraction == other.cornerFraction
    }

    override fun hashCode(): Int = cornerFraction.hashCode()
}

/**
 * iOS continuous-curvature squircle card shape with fixed corner radius in [Dp].
 */
class IosRoundedCardSquircleShape(
    private val cornerRadiusDp: Dp = 28.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val radiusPx = with(density) { cornerRadiusDp.toPx() }
            .coerceAtMost(minOf(size.width, size.height) / 2f)
        return Outline.Generic(buildSquirclePath(size.width, size.height, radiusPx))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IosRoundedCardSquircleShape) return false
        return cornerRadiusDp == other.cornerRadiusDp
    }

    override fun hashCode(): Int = cornerRadiusDp.hashCode()
}
