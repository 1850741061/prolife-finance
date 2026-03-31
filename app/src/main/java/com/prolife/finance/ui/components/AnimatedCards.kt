package com.prolife.finance.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prolife.finance.ui.animations.AnimationDurations
import com.prolife.finance.ui.animations.AnimationEasing
import com.prolife.finance.ui.animations.BreathingEffect
import com.prolife.finance.ui.formatCurrency
import com.prolife.finance.ui.theme.LedgerBloomTokens
import kotlinx.coroutines.delay

/**
 * 3D 卡片效果组件 - 支持倾斜和光泽效果
 */
@Composable
fun Animated3DCard(
    modifier: Modifier = Modifier,
    elevation: Float = 8f,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.02f else 1f,
        animationSpec = tween(AnimationDurations.QUICK, easing = AnimationEasing.Bounce),
        label = "scale"
    )

    val elevationAnim by animateFloatAsState(
        targetValue = if (isHovered) elevation + 12f else elevation,
        animationSpec = tween(AnimationDurations.NORMAL, easing = AnimationEasing.Standard),
        label = "elevation"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(elevationAnim.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onClick?.invoke() }
            )
    ) {
        content()
    }

    LaunchedEffect(Unit) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is androidx.compose.foundation.interaction.PressInteraction.Press -> isHovered = true
                is androidx.compose.foundation.interaction.PressInteraction.Release -> isHovered = false
                is androidx.compose.foundation.interaction.PressInteraction.Cancel -> isHovered = false
            }
        }
    }
}

/**
 * 玻璃态效果卡片
 */
@Composable
fun GlassmorphismCard(
    modifier: Modifier = Modifier,
    blurAlpha: Float = 0.7f,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = blurAlpha)
            )
    ) {
        content()
    }
}

/**
 * 带有呼吸动画的金额显示卡片
 */
@Composable
fun AnimatedBalanceCard(
    title: String,
    amount: Double,
    subtitle: String,
    isIncome: Boolean = true,
    modifier: Modifier = Modifier
) {
    val color = if (isIncome) LedgerBloomTokens.palette.income else LedgerBloomTokens.palette.expense
    val icon = if (isIncome) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown

    BreathingEffect(
        minScale = 0.99f,
        maxScale = 1.01f,
        durationMillis = 3000
    ) { scale ->
        Card(
            modifier = modifier
                .fillMaxWidth()
                .scale(scale),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = color.copy(alpha = 0.08f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = color.copy(alpha = 0.8f)
                    )
                }

                val animatedAmount by animateFloatAsState(
                    targetValue = amount.toFloat(),
                    animationSpec = tween(AnimationDurations.SMOOTH, easing = AnimationEasing.Standard),
                    label = "amount"
                )

                Text(
                    text = formatCurrency(animatedAmount.toDouble()),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = color
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * 带入场动画的列表项包装器
 */
@Composable
fun <T> AnimatedListItemWrapper(
    item: T,
    index: Int,
    @Suppress("UNUSED_PARAMETER") totalItems: Int = 0,
    animationType: ListAnimationType = ListAnimationType.FADE_SLIDE,
    content: @Composable (T) -> Unit
) {
    val delay = index * 60

    val visibleState = remember { MutableTransitionState(false) }

    LaunchedEffect(Unit) {
        delay(delay.toLong())
        visibleState.targetState = true
    }

    AnimatedVisibility(
        visibleState = visibleState,
        enter = when (animationType) {
            ListAnimationType.FADE -> fadeIn(tween(AnimationDurations.NORMAL, easing = AnimationEasing.Standard))
            ListAnimationType.SLIDE -> slideInVertically(
                tween(AnimationDurations.NORMAL, easing = AnimationEasing.Standard),
                initialOffsetY = { it / 4 }
            )
            ListAnimationType.FADE_SLIDE -> fadeIn(tween(AnimationDurations.NORMAL, easing = AnimationEasing.Standard)) +
                    slideInVertically(tween(AnimationDurations.NORMAL, easing = AnimationEasing.Standard), initialOffsetY = { it / 4 })
            ListAnimationType.SCALE -> scaleIn(tween(AnimationDurations.NORMAL, easing = AnimationEasing.Bounce), initialScale = 0.8f)
            ListAnimationType.FLIP -> expandIn(tween(AnimationDurations.NORMAL, easing = AnimationEasing.Standard))
        }
    ) {
        content(item)
    }
}

enum class ListAnimationType {
    FADE, SLIDE, FADE_SLIDE, SCALE, FLIP
}

/**
 * 脉冲光环效果
 */
@Composable
fun PulseRingEffect(
    content: @Composable () -> Unit
) {
    val pulseAnimation = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnimation.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = AnimationEasing.Standard),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val pulseAlpha by pulseAnimation.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = AnimationEasing.Standard),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    Box {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                    alpha = pulseAlpha
                }
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    RoundedCornerShape(50)
                )
        )
        content()
    }
}

/**
 * 滚动视差效果修饰符
 */
fun Modifier.parallaxEffect(scrollState: androidx.compose.foundation.ScrollState, speed: Float = 0.5f): Modifier = composed {
    graphicsLayer {
        translationY = scrollState.value * speed
    }
}
