package com.prolife.finance.ui.animations
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 图表动画数据类
 */
data class AnimatedChartState(
    val progress: Float,
    val waveOffset: Float,
    val pulseScale: Float
)

/**
 * 创建图表动画状态
 */
@Composable
fun rememberChartAnimation(
    durationMillis: Int = AnimationDurations.SMOOTH
): AnimatedChartState {
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis, easing = AnimationEasing.Standard),
        label = "chartProgress"
    )

    return AnimatedChartState(progress, waveOffset = 0f, pulseScale = 1f)
}

/**
 * 绘制带动画的折线图路径
 * strokeWidth/dotRadii 应由调用方在 Canvas 外部预计算 px 值传入。
 */
fun DrawScope.drawAnimatedLineChart(
    points: List<Offset>,
    color: Color,
    strokeWidth: Float,
    animationProgress: Float = 1f,
    showGradient: Boolean = true,
    gradientColors: List<Color> = listOf(color.copy(alpha = 0.3f), Color.Transparent),
    lastDotRadius: Float,
    normalDotRadius: Float
) {
    if (points.size < 2) return

    // 计算动画截止点
    val totalPoints = points.size
    val animatedPointCount = (totalPoints * animationProgress).toInt().coerceAtLeast(2)
    val visiblePoints = points.take(animatedPointCount)

    // 绘制渐变填充
    if (showGradient && visiblePoints.size >= 2) {
        val fillPath = Path().apply {
            moveTo(visiblePoints.first().x, size.height)
            visiblePoints.forEach { lineTo(it.x, it.y) }
            lineTo(visiblePoints.last().x, size.height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = gradientColors,
                startY = visiblePoints.minOf { it.y },
                endY = size.height
            )
        )
    }

    // 绘制线条（带动画效果）
    val path = Path().apply {
        moveTo(visiblePoints.first().x, visiblePoints.first().y)

        // 使用贝塞尔曲线使线条更平滑
        for (i in 1 until visiblePoints.size) {
            val prev = visiblePoints[i - 1]
            val curr = visiblePoints[i]
            val midX = (prev.x + curr.x) / 2

            if (i == 1) {
                quadraticBezierTo(prev.x, prev.y, midX, (prev.y + curr.y) / 2)
            }
            quadraticBezierTo(curr.x, curr.y, curr.x, curr.y)
        }
    }

    drawPath(
        path = path,
        color = color,
        style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = strokeWidth,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round
        )
    )

    // 绘制数据点
    visiblePoints.forEachIndexed { index, point ->
        val isLastPoint = index == visiblePoints.size - 1
        val radius = if (isLastPoint && animationProgress < 1f) {
            lastDotRadius * (1 + kotlin.math.sin(animationProgress * 10) * 0.3f)
        } else {
            if (isLastPoint) lastDotRadius else normalDotRadius
        }

        drawCircle(
            color = color,
            radius = radius,
            center = point
        )

        // 外圈光环
        if (isLastPoint) {
            drawCircle(
                color = color.copy(alpha = 0.3f),
                radius = radius * 1.5f,
                center = point
            )
        }
    }
}

/**
 * 绘制带动画的环形图
 */
fun DrawScope.drawAnimatedDonutChart(
    segments: List<DonutSegment>,
    strokeWidth: Float,
    animationProgress: Float = 1f,
    gapAngle: Float = 2f
) {
    val total = segments.sumOf { it.value }
    if (total == 0.0) return

    val center = Offset(size.width / 2, size.height / 2)
    val radius = (size.minDimension - strokeWidth) / 2

    var startAngle = -90f
    val totalAngle = 360f - (segments.size * gapAngle)

    segments.forEachIndexed { index, segment ->
        val sweepAngle = ((segment.value / total) * totalAngle).toFloat() * animationProgress

        // 绘制发光效果
        drawArc(
            color = segment.color.copy(alpha = 0.2f),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )

        // 绘制主弧形
        drawArc(
            color = segment.color,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = strokeWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )

        startAngle += sweepAngle + gapAngle
    }
}

data class DonutSegment(
    val value: Double,
    val color: Color,
    val label: String = ""
)

/**
 * 绘制动画柱状图
 */
fun DrawScope.drawAnimatedBarChart(
    bars: List<BarData>,
    animationProgress: Float = 1f,
    barWidth: Float,
    gap: Float,
    showGradient: Boolean = true,
    chartBottomPadding: Float,
    shadowOffset: Float,
    highlightHeight: Float,
    labelOffset: Float,
    labelTextSize: Float
) {
    if (bars.isEmpty()) return

    val maxValue = bars.maxOfOrNull { it.value }?.toFloat() ?: 1f
    val chartHeight = size.height - chartBottomPadding
    val totalBarWidth = bars.size * barWidth + (bars.size - 1) * gap
    val startX = (size.width - totalBarWidth) / 2

    bars.forEachIndexed { index, bar ->
        val barHeight = ((bar.value.toFloat() / maxValue) * chartHeight * animationProgress)
        val x = startX + index * (barWidth + gap)
        val bottomPaddingHalf = chartBottomPadding / 2f
        val y = size.height - barHeight - bottomPaddingHalf

        // 绘制柱子阴影
        drawRect(
            color = bar.color.copy(alpha = 0.2f),
            topLeft = Offset(x + shadowOffset, y + shadowOffset),
            size = Size(barWidth, barHeight)
        )

        // 绘制渐变柱子
        if (showGradient) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        bar.color.copy(alpha = 0.9f),
                        bar.color
                    ),
                    startY = y,
                    endY = y + barHeight
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight)
            )
        } else {
            drawRect(
                color = bar.color,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight)
            )
        }

        // 绘制顶部高光
        drawRect(
            color = Color.White.copy(alpha = 0.3f),
            topLeft = Offset(x, y),
            size = Size(barWidth, highlightHeight)
        )
        // 绘制数值标签（动画完成后显示）
        if (animationProgress > 0.8f) {
            val labelAlpha = ((animationProgress - 0.8f) / 0.2f).coerceIn(0f, 1f)
            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    "${bar.value.toInt()}",
                    x + barWidth / 2,
                    y - labelOffset,
                    android.graphics.Paint().apply {
                        color = bar.color.copy(alpha = labelAlpha).toArgb()
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = labelTextSize
                        isAntiAlias = true
                    }
                )
            }
        }
    }
}

data class BarData(
    val value: Double,
    val color: Color,
    val label: String = ""
)

