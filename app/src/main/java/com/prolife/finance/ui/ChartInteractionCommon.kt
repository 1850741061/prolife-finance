package com.prolife.finance.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

internal fun chartIndexFromTap(offsetX: Float, chartWidth: Float, count: Int): Int {
    if (count <= 1) return 0
    val itemWidth = chartWidth / (count - 1)
    return (offsetX / itemWidth).toInt().coerceIn(0, count - 1)
}

internal fun lineChartPoint(
    index: Int,
    itemCount: Int,
    amount: Double,
    maxAmount: Double,
    widthPx: Float,
    heightPx: Float,
    topPaddingPx: Float,
    bottomPaddingPx: Float,
    progress: Float
): Offset {
    val chartHeight = heightPx - topPaddingPx - bottomPaddingPx
    val x = if (itemCount <= 1) widthPx / 2f else widthPx * index / (itemCount - 1)
    val ratio = if (maxAmount <= 0.0) 0f else (amount / maxAmount).toFloat().coerceIn(0f, 1f)
    val y = topPaddingPx + chartHeight - (chartHeight * ratio * progress)
    return Offset(x, y)
}

internal fun lineChartTooltipOffset(
    point: Offset,
    tooltipWidthPx: Int,
    tooltipHeightPx: Int,
    containerWidthPx: Float,
    containerHeightPx: Float,
    marginPx: Float
): IntOffset {
    val tooltipWidth = tooltipWidthPx.toFloat()
    val tooltipHeight = tooltipHeightPx.toFloat()
    val clampedX = (point.x - tooltipWidth / 2f).coerceIn(0f, (containerWidthPx - tooltipWidth).coerceAtLeast(0f))
    val preferredAbove = point.y - tooltipHeight - marginPx
    val fallbackBelow = point.y + marginPx
    val resolvedY = if (preferredAbove >= 0f) {
        preferredAbove
    } else {
        fallbackBelow.coerceAtMost((containerHeightPx - tooltipHeight).coerceAtLeast(0f))
    }
    return IntOffset(clampedX.roundToInt(), resolvedY.roundToInt())
}