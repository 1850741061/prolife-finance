package com.prolife.finance.ui.charts

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prolife.finance.ui.theme.LedgerBloomTokens
import kotlin.math.*

/**
 * 可交互的折线图
 * 支持点击显示数值、峰值高亮、长按锁定、无障碍支持
 */
@Composable
fun InteractiveLineChart(
    data: List<Pair<String, Double>>,
    modifier: Modifier = Modifier,
    lineColor: Color = LedgerBloomTokens.palette.chartAccent,
    highlightColor: Color = LedgerBloomTokens.palette.expense,
    showPoints: Boolean = true,
    onPointSelected: ((Int) -> Unit)? = null
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var lockedIndex by remember { mutableStateOf<Int?>(null) }
    val textMeasurer = rememberTextMeasurer()
    val haptic = LocalHapticFeedback.current

    // 动画进度
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animProgress.animateTo(1f, tween(400, easing = EaseOutQuart))
    }

    if (data.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无数据",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val maxValue = data.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1.0
    val maxIndex = data.indices.maxByOrNull { data[it].second } ?: -1

    // 预先获取主题颜色，避免在 Canvas 中调用 @Composable
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    // 无障碍描述
    val chartDescription = remember(data) {
        buildString {
            append("折线图，共${data.size}个数据点。")
            val peak = data.maxByOrNull { it.second }
            peak?.let { append("峰值在${it.first}，值为${it.second.toInt()}元。") }
            val avg = data.sumOf { it.second } / data.size
            append("平均值约${avg.toInt()}元。")
        }
    }

    Box(modifier = modifier.semantics { contentDescription = chartDescription }) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(data) {
                    detectTapGestures(
                        onTap = { offset ->
                            val chartWidth = size.width
                            val padding = 32.dp.toPx()
                            val usableWidth = chartWidth - padding * 2
                            val itemWidth = usableWidth / (data.size - 1).coerceAtLeast(1)
                            val index = ((offset.x - padding) / itemWidth).toInt().coerceIn(0, data.size - 1)
                            selectedIndex = index
                            onPointSelected?.invoke(index)
                        },
                        onLongPress = { offset ->
                            val chartWidth = size.width
                            val padding = 32.dp.toPx()
                            val usableWidth = chartWidth - padding * 2
                            val itemWidth = usableWidth / (data.size - 1).coerceAtLeast(1)
                            val index = ((offset.x - padding) / itemWidth).toInt().coerceIn(0, data.size - 1)
                            lockedIndex = if (lockedIndex == index) null else index
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                }
        ) {
            val width = size.width
            val height = size.height
            val padding = 32.dp.toPx()
            val chartHeight = height - padding * 2
            val chartWidth = width - padding * 2

            // 绘制网格线
            val gridLines = 4
            for (i in 0..gridLines) {
                val y = padding + chartHeight * i / gridLines
                drawLine(
                    color = outlineColor.copy(alpha = 0.3f),
                    start = Offset(padding, y),
                    end = Offset(width - padding, y),
                    strokeWidth = 1f
                )
            }

            // 计算点位置
            val points = data.mapIndexed { index, (_, value) ->
                val x = padding + chartWidth * index / (data.size - 1).coerceAtLeast(1)
                val y = padding + chartHeight - (chartHeight * (value / maxValue) * animProgress.value).toFloat()
                Offset(x, y)
            }

            // 绘制渐变填充区域
            if (points.size > 1) {
                val fillPath = Path().apply {
                    moveTo(points.first().x, height - padding)
                    lineTo(points.first().x, points.first().y)

                    for (i in 1 until points.size) {
                        val prev = points[i - 1]
                        val curr = points[i]
                        val cx1 = (prev.x + curr.x) / 2
                        val cx2 = (prev.x + curr.x) / 2
                        cubicTo(cx1, prev.y, cx2, curr.y, curr.x, curr.y)
                    }

                    lineTo(points.last().x, height - padding)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = 0.3f),
                            lineColor.copy(alpha = 0.05f)
                        ),
                        startY = padding,
                        endY = height - padding
                    )
                )
            }

            // 绘制折线
            if (points.size > 1) {
                val linePath = Path().apply {
                    moveTo(points.first().x, points.first().y)

                    for (i in 1 until points.size) {
                        val prev = points[i - 1]
                        val curr = points[i]
                        val cx1 = (prev.x + curr.x) / 2
                        val cx2 = (prev.x + curr.x) / 2
                        cubicTo(cx1, prev.y, cx2, curr.y, curr.x, curr.y)
                    }
                }

                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                )
            }

            // 绘制数据点和峰值高亮
            points.forEachIndexed { index, point ->
                val isPeak = index == maxIndex
                val isSelected = index == selectedIndex || index == lockedIndex
                val value = data[index].second

                val pointRadius = when {
                    isSelected -> 10f
                    isPeak -> 7f
                    else -> 4f
                }

                val pointColor = when {
                    isSelected -> highlightColor
                    isPeak -> highlightColor
                    else -> lineColor
                }

                // 外圈光晕
                if (isPeak || isSelected) {
                    drawCircle(
                        color = pointColor.copy(alpha = 0.2f),
                        radius = pointRadius * 2.5f,
                        center = point
                    )
                }

                // 数据点
                drawCircle(
                    color = Color.White,
                    radius = pointRadius,
                    center = point
                )
                drawCircle(
                    color = pointColor,
                    radius = pointRadius * 0.6f,
                    center = point
                )

                // 选中时显示数值标签
                if (isSelected) {
                    val label = data[index].first
                    val valueText = "¥${value.toInt()}"

                    val textLayoutResult = textMeasurer.measure(
                        text = valueText,
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = highlightColor
                        )
                    )

                    val labelLayoutResult = textMeasurer.measure(
                        text = label,
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = onSurfaceVariantColor
                        )
                    )

                    val boxWidth = maxOf(textLayoutResult.size.width, labelLayoutResult.size.width) + 16
                    val boxHeight = textLayoutResult.size.height + labelLayoutResult.size.height + 12

                    val boxLeft = (point.x - boxWidth / 2).coerceIn(0f, width - boxWidth)
                    val boxTop = (point.y - boxHeight - 12).coerceAtLeast(0f)

                    // 背景框
                    drawRoundRect(
                        color = surfaceColor,
                        topLeft = Offset(boxLeft, boxTop),
                        size = Size(boxWidth.toFloat(), boxHeight.toFloat()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                    )

                    // 连接线
                    drawLine(
                        color = highlightColor.copy(alpha = 0.5f),
                        start = Offset(point.x, point.y - 8),
                        end = Offset(point.x, boxTop + boxHeight),
                        strokeWidth = 1f
                    )
                }
            }

            // X轴标签 - 智能显示
            val labelStep = maxOf(1, data.size / 5)
            data.forEachIndexed { index, (label, _) ->
                if (index % labelStep == 0 || index == data.size - 1) {
                    val x = padding + chartWidth * index / (data.size - 1).coerceAtLeast(1)
                    val textLayoutResult = textMeasurer.measure(
                        text = label,
                        style = TextStyle(fontSize = 10.sp, color = onSurfaceVariantColor)
                    )

                    drawText(
                        textMeasurer = textMeasurer,
                        text = label,
                        style = TextStyle(fontSize = 10.sp, color = onSurfaceVariantColor),
                        topLeft = Offset(
                            x - textLayoutResult.size.width / 2,
                            height - padding + 8
                        )
                    )
                }
            }
        }

        // 选中时的浮动提示
        (selectedIndex ?: lockedIndex)?.let { index ->
            val (_, value) = data[index]
            FloatingValueTip(
                value = value,
                label = data[index].first,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

/**
 * 可交互的环形图
 * 支持点击扇区高亮、显示详情、长按锁定、无障碍支持
 */
@Composable
fun InteractiveDonutChart(
    slices: List<CategorySlice>,
    modifier: Modifier = Modifier,
    centerContent: @Composable () -> Unit = {},
    onSliceSelected: ((Int) -> Unit)? = null
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var lockedIndex by remember { mutableStateOf<Int?>(null) }
    val haptic = LocalHapticFeedback.current

    if (slices.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无数据",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val total = slices.sumOf { it.amount }

    // 动画进度
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        animProgress.animateTo(1f, tween(400, easing = EaseOutQuart))
    }

    // 无障碍描述
    val chartDescription = remember(slices) {
        buildString {
            append("环形图，共${slices.size}个类别。")
            slices.forEachIndexed { index, slice ->
                append("${slice.name}占${(slice.ratio * 100).toInt()}%，")
            }
        }
    }

    Box(
        modifier = modifier.semantics { contentDescription = chartDescription },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(slices) {
                    detectTapGestures(
                        onTap = { offset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val touchVector = offset - center
                            val touchDistance = touchVector.getDistance()
                            val touchAngle = (atan2(touchVector.y, touchVector.x) * 180 / PI).toFloat()

                            // 标准化角度到 -90 到 270
                            val normalizedAngle = ((touchAngle + 90) % 360 + 360) % 360

                            val outerRadius = minOf(size.width, size.height) / 2f * 0.8f
                            val innerRadius = outerRadius * 0.6f

                            if (touchDistance in innerRadius..outerRadius) {
                                var currentAngle = 0f
                                slices.forEachIndexed { index, slice ->
                                    val sweep = (slice.ratio * 360 * animProgress.value).toFloat()
                                    if (normalizedAngle in currentAngle..(currentAngle + sweep)) {
                                        selectedIndex = index
                                        onSliceSelected?.invoke(index)
                                        return@detectTapGestures
                                    }
                                    currentAngle += sweep
                                }
                            } else {
                                selectedIndex = null
                            }
                        },
                        onLongPress = { offset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val touchVector = offset - center
                            val touchDistance = touchVector.getDistance()
                            val touchAngle = (atan2(touchVector.y, touchVector.x) * 180 / PI).toFloat()
                            val normalizedAngle = ((touchAngle + 90) % 360 + 360) % 360

                            val outerRadius = minOf(size.width, size.height) / 2f * 0.8f
                            val innerRadius = outerRadius * 0.6f

                            if (touchDistance in innerRadius..outerRadius) {
                                var currentAngle = 0f
                                var foundIndex = -1
                                slices.forEachIndexed { index, slice ->
                                    val sweep = (slice.ratio * 360 * animProgress.value).toFloat()
                                    if (normalizedAngle in currentAngle..(currentAngle + sweep)) {
                                        foundIndex = index
                                        return@forEachIndexed
                                    }
                                    currentAngle += sweep
                                }
                                if (foundIndex >= 0) {
                                    lockedIndex = if (lockedIndex == foundIndex) null else foundIndex
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        }
                    )
                }
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val outerRadius = minOf(size.width, size.height) / 2f * 0.8f
            val innerRadius = outerRadius * 0.6f
            val strokeWidth = outerRadius - innerRadius

            var currentAngle = -90f

            slices.forEachIndexed { index, slice ->
                val isSelected = index == selectedIndex || index == lockedIndex
                val sweep = (slice.ratio * 360 * animProgress.value).toFloat()
                val expansion = if (isSelected) 8f else 0f

                // 计算扇区中心角度
                val midAngle = currentAngle + sweep / 2
                val expansionOffset = if (isSelected) {
                    Offset(
                        cos(Math.toRadians(midAngle.toDouble())).toFloat() * expansion,
                        sin(Math.toRadians(midAngle.toDouble())).toFloat() * expansion
                    )
                } else Offset.Zero

                // 绘制扇区
                drawArc(
                    color = slice.color.copy(alpha = if (isSelected) 1f else 0.85f),
                    startAngle = currentAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(
                        center.x - outerRadius + expansionOffset.x,
                        center.y - outerRadius + expansionOffset.y
                    ),
                    size = Size(outerRadius * 2, outerRadius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // 选中时添加光晕
                if (isSelected) {
                    drawArc(
                        color = slice.color.copy(alpha = 0.3f),
                        startAngle = currentAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(
                            center.x - outerRadius - 4 + expansionOffset.x,
                            center.y - outerRadius - 4 + expansionOffset.y
                        ),
                        size = Size((outerRadius + 4) * 2, (outerRadius + 4) * 2),
                        style = Stroke(width = strokeWidth + 8, cap = StrokeCap.Round)
                    )
                }

                currentAngle += sweep
            }
        }

        // 中心内容
        Box(modifier = Modifier.fillMaxSize(0.5f)) {
            val displayIndex = selectedIndex ?: lockedIndex
            if (displayIndex != null && displayIndex < slices.size) {
                val slice = slices[displayIndex]
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = slice.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = slice.color,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Text(
                        text = "${(slice.ratio * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
                    )
                    Text(
                        text = "¥${slice.amount.toInt()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                centerContent()
            }
        }

        // 选中时的详细信息
        (selectedIndex ?: lockedIndex)?.let { index ->
            if (index < slices.size) {
                val slice = slices[index]
                SelectedSliceDetail(
                    slice = slice,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

/**
 * 增强的柱状图
 * 支持圆角、渐变、点击效果
 */
@Composable
fun EnhancedBarChart(
    data: List<BarData>,
    modifier: Modifier = Modifier,
    barColor: Color = LedgerBloomTokens.palette.chartAccent,
    highlightColor: Color = LedgerBloomTokens.palette.hero
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    
    if (data.isEmpty()) return
    
    val maxValue = data.maxOfOrNull { it.value }?.takeIf { it > 0 } ?: 1.0
    val textMeasurer = rememberTextMeasurer()
    
    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val chartWidth = size.width
                        val itemWidth = chartWidth / data.size
                        val index = (offset.x / itemWidth).toInt().coerceIn(0, data.size - 1)
                        selectedIndex = if (selectedIndex == index) null else index
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val padding = 24.dp.toPx()
            val chartHeight = height - padding * 2
            val barWidth = (width / data.size) * 0.6f
            val spacing = (width / data.size) * 0.4f
            
            data.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                val isMax = item.value == maxValue
                
                val barHeight = (chartHeight * (item.value / maxValue)).toFloat()
                val x = spacing / 2 + index * (barWidth + spacing)
                val y = height - padding - barHeight
                
                val color = when {
                    isSelected -> highlightColor
                    isMax -> highlightColor.copy(alpha = 0.8f)
                    else -> barColor.copy(alpha = 0.6f)
                }
                
                // 柱状图圆角
                val cornerRadius = 4.dp.toPx()
                
                // 绘制渐变柱状图
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            color,
                            color.copy(alpha = 0.7f)
                        ),
                        startY = y,
                        endY = y + barHeight
                    ),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
                )
                
                // 选中或最大值时显示数值
                if (isSelected || isMax) {
                    val valueText = "¥${item.value.toInt()}"
                    val textLayoutResult = textMeasurer.measure(
                        text = valueText,
                        style = TextStyle(
                            fontSize = if (isSelected) 12.sp else 10.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = if (isSelected) highlightColor else color
                        )
                    )
                    
                    drawText(
                        textMeasurer = textMeasurer,
                        text = valueText,
                        style = TextStyle(
                            fontSize = if (isSelected) 12.sp else 10.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = if (isSelected) highlightColor else color
                        ),
                        topLeft = Offset(
                            x + (barWidth - textLayoutResult.size.width) / 2,
                            y - textLayoutResult.size.height - 4
                        )
                    )
                }
                
                // X轴标签
                val labelLayoutResult = textMeasurer.measure(
                    text = item.label,
                    style = TextStyle(fontSize = 9.sp, color = Color.Gray)
                )
                
                if (index % maxOf(1, data.size / 5) == 0) {
                    drawText(
                        textMeasurer = textMeasurer,
                        text = item.label,
                        style = TextStyle(fontSize = 9.sp, color = Color.Gray),
                        topLeft = Offset(
                            x + (barWidth - labelLayoutResult.size.width) / 2,
                            height - padding + 4
                        )
                    )
                }
            }
        }
    }
}

/**
 * 浮动数值提示
 */
@Composable
private fun FloatingValueTip(
    value: Double,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "¥${value.toInt()}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            label?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * 选中扇区详情
 */
@Composable
private fun SelectedSliceDetail(
    slice: CategorySlice,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(slice.color)
            )
            Text(
                text = slice.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            )
            Text(
                text = "${(slice.ratio * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = slice.color,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Text(
                text = "¥${slice.amount.toInt()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// 数据类
data class CategorySlice(
    val name: String,
    val amount: Double,
    val ratio: Double,
    val color: Color
)

data class BarData(
    val label: String,
    val value: Double,
    val color: Color? = null
)

private val EaseOutQuart = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
private val EaseInOutSine = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)
