package com.prolife.finance.ui.animations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

// ============================================
// 动画配置常量
// ============================================
object AnimationDurations {
    const val QUICK = 150
    const val NORMAL = 300
    const val SMOOTH = 500
    const val DRAMATIC = 800
    const val EPIC = 1200
}

object AnimationEasing {
    val Standard = FastOutSlowInEasing
    val Decelerate = CubicBezierEasing(0f, 0f, 0.2f, 1f)
    val Accelerate = CubicBezierEasing(0.4f, 0f, 1f, 1f)
    val Bounce = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
    val Elastic = CubicBezierEasing(0.68f, -0.55f, 0.265f, 1.55f)
}

// ============================================
// 进入/退出动画
// ============================================
@Composable
fun <T> AnimatedListItem(
    item: T,
    index: Int,
    content: @Composable (T) -> Unit
) {
    val delay = index * 50

    val enterTransition = fadeIn(
        animationSpec = tween(AnimationDurations.NORMAL, delayMillis = delay, easing = AnimationEasing.Standard)
    ) + slideInVertically(
        animationSpec = tween(AnimationDurations.NORMAL, delayMillis = delay, easing = AnimationEasing.Standard),
        initialOffsetY = { it / 3 }
    )

    AnimatedVisibility(
        visible = true,
        enter = enterTransition
    ) {
        content(item)
    }
}

@Composable
fun FadeInContainer(
    delayMillis: Int = 0,
    durationMillis: Int = AnimationDurations.NORMAL,
    content: @Composable () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis, delayMillis, AnimationEasing.Standard),
        label = "fade"
    )

    Box(modifier = Modifier.alpha(alpha)) {
        content()
    }
}

@Composable
fun SlideInContainer(
    from: SlideDirection = SlideDirection.BOTTOM,
    delayMillis: Int = 0,
    durationMillis: Int = AnimationDurations.NORMAL,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMillis.toLong())
        visible = true
    }

    val initialOffset: (IntSize) -> IntOffset = when (from) {
        SlideDirection.LEFT -> { size -> IntOffset(-size.width, 0) }
        SlideDirection.RIGHT -> { size -> IntOffset(size.width, 0) }
        SlideDirection.TOP -> { size -> IntOffset(0, -size.height) }
        SlideDirection.BOTTOM -> { size -> IntOffset(0, size.height) }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideIn(
            animationSpec = tween(durationMillis, easing = AnimationEasing.Standard),
            initialOffset = initialOffset
        ) + fadeIn(tween(durationMillis))
    ) {
        content()
    }
}

enum class SlideDirection {
    LEFT, RIGHT, TOP, BOTTOM
}

// ============================================
// 微交互动画
// ============================================
fun Modifier.animatedScaleOnPress(
    pressedScale: Float = 0.95f,
    durationMillis: Int = AnimationDurations.QUICK
): Modifier = composed {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = tween(durationMillis, easing = AnimationEasing.Bounce),
        label = "scale"
    )

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

fun Modifier.animatedElevationOnPress(
    pressedElevation: Float = 2f,
    normalElevation: Float = 8f,
    durationMillis: Int = AnimationDurations.QUICK
): Modifier = composed {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val elevation by animateFloatAsState(
        targetValue = if (isPressed) pressedElevation else normalElevation,
        animationSpec = tween(durationMillis, easing = AnimationEasing.Standard),
        label = "elevation"
    )

    this.graphicsLayer { shadowElevation = elevation }
}

fun Modifier.shimmerEffect(durationMillis: Int = 2000): Modifier = composed {
    val shimmerAnimation = rememberInfiniteTransition(label = "shimmer")
    val shimmerProgress by shimmerAnimation.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = AnimationEasing.Standard),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    this.graphicsLayer {
        alpha = 0.3f + (0.4f * kotlin.math.abs(shimmerProgress - 0.5f) * 2)
    }
}

// ============================================
// 数字动画
// ============================================
@Composable
fun AnimatedCounter(
    value: Double,
    format: (Double) -> String = { "%.2f".format(it) },
    durationMillis: Int = AnimationDurations.SMOOTH,
    style: androidx.compose.ui.text.TextStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge
) {
    val animatedValue by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = tween(durationMillis, easing = AnimationEasing.Standard),
        label = "counter"
    )

    Text(
        text = format(animatedValue.toDouble()),
        style = style
    )
}

@Composable
fun CountUpAnimation(
    targetValue: Int,
    durationMillis: Int = AnimationDurations.SMOOTH,
    content: @Composable (Int) -> Unit
) {
    val animatedValue by animateIntAsState(
        targetValue = targetValue,
        animationSpec = tween(durationMillis, easing = AnimationEasing.Standard),
        label = "countUp"
    )

    content(animatedValue)
}

// ============================================
// 图表动画
// ============================================
@Composable
fun AnimatedChartProgress(
    progress: Float,
    durationMillis: Int = AnimationDurations.SMOOTH,
    content: @Composable (Float) -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis, easing = AnimationEasing.Standard),
        label = "chartProgress"
    )

    content(animatedProgress)
}

// ============================================
// 页面过渡动画
// ============================================
@Composable
fun <T> CrossfadeAnimation(
    targetState: T,
    durationMillis: Int = AnimationDurations.NORMAL,
    content: @Composable (T) -> Unit
) {
    androidx.compose.animation.Crossfade(
        targetState = targetState,
        animationSpec = tween(durationMillis, easing = AnimationEasing.Standard),
        label = "crossfade"
    ) { state ->
        content(state)
    }
}

// ============================================
// 呼吸效果
// ============================================
@Composable
fun BreathingEffect(
    minScale: Float = 0.98f,
    maxScale: Float = 1.02f,
    durationMillis: Int = 2000,
    content: @Composable (Float) -> Unit
) {
    val breathingAnimation = rememberInfiniteTransition(label = "breathing")
    val scale by breathingAnimation.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis / 2, easing = AnimationEasing.Standard),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingScale"
    )

    content(scale)
}

// ============================================
// 震动效果
// ============================================
@Composable
fun ShakeEffect(
    shakeDuration: Int = 500,
    shakeIntensity: Float = 10f,
    content: @Composable (Modifier) -> Unit
) {
    var shakeTrigger by remember { mutableStateOf(0) }

    val shakeAnimation = remember(shakeTrigger) {
        if (shakeTrigger > 0) {
            Animatable(0f)
        } else null
    }

    LaunchedEffect(shakeAnimation) {
        shakeAnimation?.let { animatable ->
            animatable.animateTo(
                targetValue = 1f,
                animationSpec = keyframes {
                    durationMillis = shakeDuration
                    0f at 0 with LinearEasing
                    1f at (shakeDuration * 0.25).toInt() with LinearEasing
                    -1f at (shakeDuration * 0.5).toInt() with LinearEasing
                    0.5f at (shakeDuration * 0.75).toInt() with LinearEasing
                    0f at shakeDuration with LinearEasing
                }
            )
        }
    }

    val shakeOffset = shakeAnimation?.value?.times(shakeIntensity)?.dp ?: 0.dp

    content(Modifier.offset(x = shakeOffset))
}

