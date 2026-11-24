package com.niagara.launcher.core.model

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Top-left origin of a grid slot in pixels inside its parent grid container.
 */
data class GridCellOrigin(
    val x: Float,
    val y: Float
)

/**
 * Pure Kotlin coordinate and list-reordering math for free-drag iOS home & favorites grids.
 */
object GridReorderMath {

    fun rowCount(itemCount: Int, columns: Int): Int {
        if (itemCount <= 0 || columns <= 0) return 0
        return (itemCount + columns - 1) / columns
    }

    fun calculateGridHeightPx(
        itemCount: Int,
        columns: Int,
        cellHeightPx: Float,
        verticalSpacingPx: Float
    ): Float {
        val rows = rowCount(itemCount, columns)
        if (rows <= 0 || cellHeightPx <= 0f) return 0f
        return rows * cellHeightPx + (rows - 1) * verticalSpacingPx.coerceAtLeast(0f)
    }

    fun indexToCellOrigin(
        index: Int,
        columns: Int,
        cellWidthPx: Float,
        cellHeightPx: Float,
        horizontalSpacingPx: Float,
        verticalSpacingPx: Float
    ): GridCellOrigin {
        if (columns <= 0 || index < 0) return GridCellOrigin(0f, 0f)
        val col = index % columns
        val row = index / columns
        val strideX = cellWidthPx + horizontalSpacingPx.coerceAtLeast(0f)
        val strideY = cellHeightPx + verticalSpacingPx.coerceAtLeast(0f)
        return GridCellOrigin(
            x = col * strideX,
            y = row * strideY
        )
    }

    /**
     * Maps the visual center of a dragged icon to the nearest valid target slot index.
     * Applies optional boundary hysteresis around [currentIndex] to eliminate rapid slot oscillation.
     */
    fun pointToTargetIndex(
        centerX: Float,
        centerY: Float,
        columns: Int,
        itemCount: Int,
        cellWidthPx: Float,
        cellHeightPx: Float,
        horizontalSpacingPx: Float,
        verticalSpacingPx: Float,
        currentIndex: Int? = null,
        hysteresisFraction: Float = 0.10f
    ): Int {
        if (itemCount <= 1 || columns <= 0 || cellWidthPx <= 0f || cellHeightPx <= 0f) {
            return 0
        }

        val strideX = (cellWidthPx + horizontalSpacingPx.coerceAtLeast(0f)).coerceAtLeast(1f)
        val strideY = (cellHeightPx + verticalSpacingPx.coerceAtLeast(0f)).coerceAtLeast(1f)
        val maxRow = (rowCount(itemCount, columns) - 1).coerceAtLeast(0)

        val colFloat = (centerX - cellWidthPx / 2f) / strideX
        val rowFloat = (centerY - cellHeightPx / 2f) / strideY

        val candidateCol = colFloat.roundToInt().coerceIn(0, columns - 1)
        val candidateRow = rowFloat.roundToInt().coerceIn(0, maxRow)
        val candidateIndex = (candidateRow * columns + candidateCol).coerceIn(0, itemCount - 1)

        if (currentIndex == null || currentIndex == candidateIndex || currentIndex !in 0 until itemCount) {
            return candidateIndex
        }

        val clampedHysteresis = hysteresisFraction.coerceIn(0f, 0.35f)
        if (clampedHysteresis <= 0f) return candidateIndex

        val currentOrigin = indexToCellOrigin(
            index = currentIndex,
            columns = columns,
            cellWidthPx = cellWidthPx,
            cellHeightPx = cellHeightPx,
            horizontalSpacingPx = horizontalSpacingPx,
            verticalSpacingPx = verticalSpacingPx
        )
        val currentCenterX = currentOrigin.x + cellWidthPx / 2f
        val currentCenterY = currentOrigin.y + cellHeightPx / 2f
        val dxNormalized = abs(centerX - currentCenterX) / strideX
        val dyNormalized = abs(centerY - currentCenterY) / strideY

        // Only leave current slot once dragged clearly past the midpoint threshold
        val leaveThreshold = 0.5f + clampedHysteresis
        return if (dxNormalized >= leaveThreshold || dyNormalized >= leaveThreshold) {
            candidateIndex
        } else {
            currentIndex
        }
    }

    /**
     * Returns a new list with the element at [fromIndex] moved to [toIndex].
     */
    fun <T> reorder(items: List<T>, fromIndex: Int, toIndex: Int): List<T> {
        if (items.size <= 1) return items
        if (fromIndex !in items.indices || toIndex !in items.indices || fromIndex == toIndex) {
            return items
        }
        val mutable = items.toMutableList()
        val moved = mutable.removeAt(fromIndex)
        mutable.add(toIndex, moved)
        return mutable
    }
}
