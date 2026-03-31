package com.prolife.finance.ui.components
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prolife.finance.ui.animations.AnimationDurations
import com.prolife.finance.ui.animations.AnimationEasing
import com.prolife.finance.ui.animations.BreathingEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch/**
 * 带动画效果的浮动操作按钮
 * @param onClick 点击回调
 * @param modifier 修饰符
 * @param enabled 是否启用
 * @param icon 图标内容
 * @param text 可选文本内容
 */
@Composable
fun AnimatedFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable () -> Unit = { Icon(Icons.Rounded.Add, contentDescription = "Add") },
    text: @Composable (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            !enabled -> 1f
            isPressed -> 0.92f
            isHovered -> 1.08f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "fabScale"
    )

    val rotation by animateFloatAsState(
        targetValue = if (isPressed && enabled) 45f else 0f,
        animationSpec = tween(300, easing = AnimationEasing.Bounce),
        label = "fabRotation"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.5f,
        label = "fabContentAlpha"
    )

    val containerColor = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    }

    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(if (enabled) 20.dp else 4.dp, RoundedCornerShape(50), clip = false)
    ) {
        if (text != null) {
            ExtendedFloatingActionButton(
                onClick = { if (enabled) onClick() },
                icon = {
                    Box(
                        modifier = Modifier
                            .graphicsLayer { rotationZ = rotation }
                            .alpha(contentAlpha)
                    ) {
                        icon()
                    }
                },
                text = { Box(modifier = Modifier.alpha(contentAlpha)) { text() } },
                containerColor = containerColor,
                contentColor = contentColor,
                shape = RoundedCornerShape(50)
            )
        } else {
            FloatingActionButton(
                onClick = { if (enabled) onClick() },
                containerColor = containerColor,
                contentColor = contentColor,
                shape = RoundedCornerShape(50)
            ) {
                Box(
                    modifier = Modifier
                        .graphicsLayer { rotationZ = rotation }
                        .alpha(contentAlpha)
                ) {
                    icon()
                }
            }
        }
    }
}

/**
 * 带动画效果的按钮
 */
@Composable
fun AnimatedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            !enabled -> 1f
            isPressed -> 0.96f
            isHovered -> 1.02f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "buttonScale"
    )

    val elevationValue by animateFloatAsState(
        targetValue = when {
            !enabled -> 0f
            isPressed -> 2f
            isHovered -> 8f
            else -> 4f
        },
        animationSpec = tween(150, easing = AnimationEasing.Standard),
        label = "buttonElevation"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(elevationValue.dp, RoundedCornerShape(12.dp), clip = false)
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            interactionSource = interactionSource,
            colors = colors,
            elevation = elevation,
            shape = RoundedCornerShape(12.dp),
            content = content
        )
    }
}

/**
 * 图标按钮带动画效果
 * @param onClick 点击回调
 * @param modifier 修饰符
 * @param enabled 是否启用
 * @param icon 图标内容
 */
@Composable
fun AnimatedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (!enabled) 1f else if (isPressed) 0.8f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "iconButtonScale"
    )

    val rotation by animateFloatAsState(
        targetValue = if (isPressed && enabled) 15f else 0f,
        animationSpec = tween(200, easing = AnimationEasing.Bounce),
        label = "iconButtonRotation"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.5f,
        label = "iconButtonAlpha"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .graphicsLayer { rotationZ = rotation }
            .clip(CircleShape)
            .alpha(contentAlpha)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

/**
 * 带涟漪扩散效果的按钮
 * @param onClick 点击回调
 * @param modifier 修饰符
 * @param enabled 是否启用
 * @param content 按钮内容
 */
@Composable
fun RippleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    var rippleCenter by remember { mutableStateOf(Offset.Zero) }
    var isRippling by remember { mutableStateOf(false) }

    // 禁用状态的视觉反馈
    val contentAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.5f,
        label = "contentAlpha"
    )

    val rippleScale by animateFloatAsState(
        targetValue = if (isRippling && enabled) 3f else 0f,
        animationSpec = tween(600, easing = AnimationEasing.Decelerate),
        label = "rippleScale"
    )

    val rippleAlpha by animateFloatAsState(
        targetValue = if (isRippling && enabled) 0f else 0.3f,
        animationSpec = tween(600, easing = AnimationEasing.Standard),
        label = "rippleAlpha"
    )

    val backgroundColor = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .pointerInput(enabled) {
                detectTapGestures { offset ->
                    if (enabled) {
                        // 关键修复：记录点击位置
                        rippleCenter = offset
                        scope.launch {
                            isRippling = true
                            delay(600)
                            isRippling = false
                        }
                        onClick()
                    }
                }
            }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        // 涟漪效果 - 使用正确的中心点
        if (isRippling && enabled) {
            val density = LocalDensity.current
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        // 使用点击位置作为涟漪中心
                        translationX = rippleCenter.x - with(density) { 50.dp.toPx() }
                        translationY = rippleCenter.y - with(density) { 50.dp.toPx() }
                        scaleX = rippleScale
                        scaleY = rippleScale
                        alpha = rippleAlpha
                    }
                    .size(100.dp)
                    .background(Color.White, CircleShape)
            )
        }

        // 内容带禁用状态透明度
        Box(modifier = Modifier.alpha(contentAlpha)) {
            content()
        }
    }
}

