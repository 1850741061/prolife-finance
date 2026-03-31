package com.prolife.finance.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.prolife.finance.model.StatsGranularity
import kotlin.math.roundToInt

private val TrendAccentColor = Color(0xFFE9789C)

@Composable
fun ReferenceTrendInsightContent(
    granularity: StatsGranularity,
    buckets: List<ExpenseBucket>,
    highlightBucket: ExpenseBucket?,
    averageExpensePerBucket: Double,
    activeExpenseDays: Int
) {
    val scopeLabel = when (granularity) {
        StatsGranularity.WEEK -> "本周内单日最高支出"
        StatsGranularity.MONTH -> "本月内单日最高支出"
        StatsGranularity.YEAR -> "本年内单月最高支出"
    }
    val averageLabel = when (granularity) {
        StatsGranularity.WEEK -> "本周内平均每日支出"
        StatsGranularity.MONTH -> "本月内平均每日支出"
        StatsGranularity.YEAR -> "本年内平均每月支出"
    }
    val activeLabel = if (granularity == StatsGranularity.YEAR) "本年内活跃月份" else "本周期活跃天数"
    val peakAmount = highlightBucket?.amount ?: 0.0
    val peakLabel = highlightBucket?.label ?: "-"
    val activeUnit = if (granularity == StatsGranularity.YEAR) "个月" else "天"

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = scopeLabel,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "在$peakLabel，你支出了",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatCurrency(peakAmount),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = TrendAccentColor
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            ReferenceMetricBlock(
                modifier = Modifier.weight(1f),
                title = averageLabel,
                value = formatCurrency(averageExpensePerBucket)
            )
            ReferenceMetricBlock(
                modifier = Modifier.weight(1f),
                title = activeLabel,
                value = "${activeExpenseDays}${activeUnit}"
            )
        }

        ReferenceTrendChart(
            buckets = buckets,
            highlightIndex = buckets.indexOf(highlightBucket).coerceAtLeast(0),
            accent = TrendAccentColor
        )
    }
}

@Composable
fun ReferenceShareInsightContent(slices: List<CategorySlice>) {
    val topSlices = slices.sortedByDescending { it.amount }.take(3)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp),
        contentAlignment = Alignment.Center
    ) {
        ReferenceShareDonut(
            slices = slices,
            modifier = Modifier.size(220.dp)
        )

        topSlices.getOrNull(0)?.let { slice ->
            ShareCallout(
                slice = slice,
                alignRight = true,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (-6).dp, y = (-34).dp)
            )
        }
        topSlices.getOrNull(1)?.let { slice ->
            ShareCallout(
                slice = slice,
                alignRight = false,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 6.dp, y = (-34).dp)
            )
        }
        topSlices.getOrNull(2)?.let { slice ->
            ShareCallout(
                slice = slice,
                alignRight = false,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = 4.dp, y = 42.dp)
            )
        }
    }
}

@Composable
private fun ReferenceMetricBlock(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ReferenceTrendChart(
    buckets: List<ExpenseBucket>,
    highlightIndex: Int,
    accent: Color
) {
    val maxAmount = buckets.maxOfOrNull { it.amount }?.takeIf { it > 0 } ?: 1.0
    val sampledIndexes = trendAxisIndexes(buckets.size)
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val baselineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
    val dotColor = MaterialTheme.colorScheme.outline
    val surfaceColor = MaterialTheme.colorScheme.surface

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
    ) {
        val density = LocalDensity.current
        val chartTop = 40.dp
        val chartHeight = 148.dp
        val chartBottomGap = 12.dp
        val leftPadding = 8.dp
        val rightPadding = 8.dp
        val bubbleWidth = 94.dp
        val bubbleHeight = 42.dp
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val chartTopPx = with(density) { chartTop.toPx() }
        val chartHeightPx = with(density) { chartHeight.toPx() }
        val leftPaddingPx = with(density) { leftPadding.toPx() }
        val rightPaddingPx = with(density) { rightPadding.toPx() }
        val baselineInsetPx = with(density) { 4.dp.toPx() }
        val peakOffsetPx = with(density) { 10.dp.toPx() }
        val bubbleWidthPx = with(density) { bubbleWidth.toPx() }
        val bubbleHeightPx = with(density) { bubbleHeight.toPx() }
        // Cache dp values used inside Canvas draw
        val gridLineStrokePx = with(density) { 1.dp.toPx() }
        val baselineStrokePx = with(density) { 2.dp.toPx() }
        val highlightFallbackWidthPx = with(density) { 28.dp.toPx() }
        val chartLineStrokePx = with(density) { 3.dp.toPx() }
        val peakDotRadiusPx = with(density) { 6.dp.toPx() }
        val normalDotRadiusPx = with(density) { 4.dp.toPx() }
        val peakInnerDotPx = with(density) { 3.dp.toPx() }
        val normalInnerDotPx = with(density) { 2.dp.toPx() }
        val canvasTopPaddingPx = with(density) { 8.dp.toPx() }
        // Reuse Path object
        val chartLinePath = remember { androidx.compose.ui.graphics.Path() }
        val stepPx = if (buckets.size <= 1) 0f else (maxWidthPx - leftPaddingPx - rightPaddingPx) / buckets.lastIndex
        val baselineY = chartHeightPx - baselineInsetPx
        val peakIndex = highlightIndex.coerceIn(0, (buckets.size - 1).coerceAtLeast(0))
        val peakPointX = leftPaddingPx + stepPx * peakIndex
        val peakPointY = if (buckets.isEmpty()) baselineY else baselineY - ((buckets[peakIndex].amount / maxAmount).toFloat() * (baselineY - peakOffsetPx))
        val bubbleX = (peakPointX - bubbleWidthPx / 2f).coerceIn(0f, maxWidthPx - bubbleWidthPx)
        val bubbleY = (chartTopPx + peakPointY - bubbleHeightPx - peakOffsetPx).coerceAtLeast(0f)

        if (buckets.isNotEmpty() && buckets[peakIndex].amount > 0.0) {
            Surface(
                modifier = Modifier.offset { IntOffset(bubbleX.roundToInt(), bubbleY.roundToInt()) },
                color = surfaceColor,
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 2.dp,
                shadowElevation = 2.dp
            ) {
                Text(
                    text = formatCurrency(buckets[peakIndex].amount),
                    modifier = Modifier
                        .background(accent.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = accent
                )
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(chartTop))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
            ) {
                val usableWidth = size.width - leftPaddingPx - rightPaddingPx
                val usableHeight = size.height - canvasTopPaddingPx - baselineInsetPx
                val step = if (buckets.size <= 1) 0f else usableWidth / buckets.lastIndex
                val baseline = canvasTopPaddingPx + usableHeight

                buckets.forEachIndexed { index, _ ->
                    val x = leftPaddingPx + step * index
                    drawLine(
                        color = gridColor,
                        start = Offset(x, canvasTopPaddingPx),
                        end = Offset(x, baseline),
                        strokeWidth = gridLineStrokePx
                    )
                }

                drawLine(
                    color = baselineColor,
                    start = Offset(leftPaddingPx, baseline),
                    end = Offset(size.width - rightPaddingPx, baseline),
                    strokeWidth = baselineStrokePx
                )

                val points = buckets.mapIndexed { index, bucket ->
                    val ratio = (bucket.amount / maxAmount).toFloat()
                    Offset(
                        x = leftPaddingPx + step * index,
                        y = baseline - ratio * usableHeight
                    )
                }

                if (points.isNotEmpty()) {
                    val peak = points[peakIndex]
                    val highlightLeft = (peak.x - step / 2f).coerceAtLeast(leftPaddingPx)
                    val highlightWidth = if (step == 0f) highlightFallbackWidthPx else step
                    drawRect(
                        color = accent.copy(alpha = 0.16f),
                        topLeft = Offset(highlightLeft, peak.y),
                        size = Size(highlightWidth, baseline - peak.y)
                    )

                    chartLinePath.rewind()
                    chartLinePath.moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { chartLinePath.lineTo(it.x, it.y) }
                    drawPath(
                        path = chartLinePath,
                        color = accent,
                        style = Stroke(width = chartLineStrokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    points.forEachIndexed { index, point ->
                        val isPeak = index == peakIndex && buckets[index].amount > 0.0
                        drawCircle(
                            color = if (isPeak) accent else dotColor,
                            radius = if (isPeak) peakDotRadiusPx else normalDotRadiusPx,
                            center = point
                        )
                        drawCircle(
                            color = surfaceColor,
                            radius = if (isPeak) peakInnerDotPx else normalInnerDotPx,
                            center = point
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(chartBottomGap))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                sampledIndexes.forEach { index ->
                    val bucket = buckets[index]
                    Text(
                        text = bucket.label.padStart(2, '0'),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (index == highlightIndex) FontWeight.Black else FontWeight.Medium,
                        color = if (index == highlightIndex) accent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun trendAxisIndexes(count: Int): List<Int> {
    return when {
        count <= 8 -> (0 until count).toList()
        count <= 12 -> (0 until count).toList()
        count <= 18 -> listOf(0, 3, 6, 9, 12, 15, count - 1).distinct().filter { it in 0 until count }
        else -> listOf(0, 4, 9, 14, 19, 24, count - 1).distinct().filter { it in 0 until count }
    }
}

@Composable
private fun ReferenceShareDonut(
    slices: List<CategorySlice>,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier = modifier) {
        val strokeWidth = 34.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)
        var startAngle = -90f

        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth)
        )

        slices.forEach { slice ->
            val sweep = (slice.ratio * 360f).toFloat()
            drawArc(
                color = slice.color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun ShareCallout(
    slice: CategorySlice,
    alignRight: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignRight) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "${slice.name} ${formatPercent(slice.ratio)}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
        ShareConnector(color = slice.color, alignRight = alignRight)
    }
}

@Composable
private fun ShareConnector(color: Color, alignRight: Boolean) {
    Canvas(modifier = Modifier.width(92.dp).height(24.dp)) {
        val strokeWidth = 3.dp.toPx()
        val y = size.height * 0.72f
        val bendX = if (alignRight) size.width * 0.42f else size.width * 0.58f
        val dotX = if (alignRight) 10.dp.toPx() else size.width - 10.dp.toPx()
        val endX = if (alignRight) size.width else 0f

        drawCircle(color = color, radius = 4.dp.toPx(), center = Offset(dotX, y))
        drawLine(
            color = color,
            start = Offset(dotX, y),
            end = Offset(bendX, y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(bendX, y),
            end = Offset(endX, if (alignRight) y - 10.dp.toPx() else y + 10.dp.toPx()),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}
