package com.prolife.finance.ui.designsystem

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Ledger Bloom 设计系统
 * 统一的间距、圆角、阴影和动画规范
 */
object LedgerDesignSystem {
    // 间距系统 - 8dp 网格
    object Spacing {
        val XS = 4.dp
        val S = 8.dp
        val SM = 12.dp      // 新增：小中间距
        val M = 16.dp
        val ML = 20.dp      // 新增：中大间距
        val L = 24.dp
        val XL = 32.dp
        val XXL = 48.dp
    }

    // 圆角系统
    object Shapes {
        val None = RoundedCornerShape(0.dp)      // Swiss 风格
        val Small = RoundedCornerShape(4.dp)     // Ink Wash 风格
        val Medium = RoundedCornerShape(8.dp)
        val Large = RoundedCornerShape(16.dp)
        val XLarge = RoundedCornerShape(24.dp)   // Pop Art 风格
        val Full = RoundedCornerShape(percent = 50)
    }

    // 阴影层级系统
    object Elevation {
        val None = 0.dp
        val Low = 2.dp      // 静态内容
        val Medium = 4.dp   // 可交互卡片
        val High = 8.dp     // 浮动元素
        val Highest = 16.dp // 模态/导航
    }

    // 动画时长
    object Animation {
        val Fast = 150
        val Normal = 300
        val Slow = 500
    }

    // 内容边距常量
    object ContentPadding {
        val ScreenHorizontal = 20.dp    // 屏幕水平边距
        val CardInternal = 16.dp        // 卡片内部边距
        val ListVertical = 12.dp        // 列表项垂直间距
        val SectionVertical = 24.dp     // 区块垂直间距
    }
}

/**
 * 按压缩放效果修饰符
 */
fun Modifier.pressableScale(
    scale: Float = 0.97f,
    enabled: Boolean = true
): Modifier = composed {
    if (!enabled) return@composed this
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) scale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScale"
    )
    
    this
        .scale(scaleAnim)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {}
        )
}

/**
 * 涟漪点击效果
 */
@Composable
fun PressableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = LedgerDesignSystem.Shapes.XLarge,
    colors: CardColors = CardDefaults.elevatedCardColors(),
    elevation: Dp = LedgerDesignSystem.Elevation.Medium,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardScale"
    )
    
    val elevationAnim by animateDpAsState(
        targetValue = if (isPressed && enabled) elevation - 2.dp else elevation,
        animationSpec = tween(150),
        label = "cardElevation"
    )
    
    ElevatedCard(
        onClick = onClick,
        modifier = modifier
            .scale(scale)
            .graphicsLayer {
                this.shape = shape
                this.clip = true
            },
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = elevationAnim,
            pressedElevation = elevation - 4.dp
        ),
        interactionSource = interactionSource,
        content = content
    )
}

/**
 * 带动画的按钮
 */
@Composable
fun AnimatedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.elevatedButtonColors(),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "buttonScale"
    )
    
    ElevatedButton(
        onClick = onClick,
        modifier = modifier.scale(scale),
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
        content = content
    )
}

/**
 * 层次化卡片 - 根据重要性自动应用不同阴影
 */
@Composable
fun HierarchicalCard(
    importance: CardImportance = CardImportance.Normal,
    modifier: Modifier = Modifier,
    shape: Shape = LedgerDesignSystem.Shapes.XLarge,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val elevation = when (importance) {
        CardImportance.Low -> LedgerDesignSystem.Elevation.Low
        CardImportance.Normal -> LedgerDesignSystem.Elevation.Medium
        CardImportance.High -> LedgerDesignSystem.Elevation.High
        CardImportance.Hero -> LedgerDesignSystem.Elevation.Highest
    }
    
    val containerColor = when (importance) {
        CardImportance.Hero -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
    }
    
    if (onClick != null) {
        PressableCard(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            elevation = elevation,
            colors = CardDefaults.elevatedCardColors(
                containerColor = containerColor
            ),
            content = content
        )
    } else {
        ElevatedCard(
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.elevatedCardColors(
                containerColor = containerColor
            ),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = elevation
            ),
            content = content
        )
    }
}

enum class CardImportance {
    Low,      // 列表项
    Normal,   // 普通卡片
    High,     // 重点卡片
    Hero      // 主卡片（结余等）
}

/**
 * 入场动画容器
 * 使用 key 确保只在首次组合时播放入场动画，
 * 切换 Tab 时不会重复触发（因为 content 处于同一 slot）。
 */
@Composable
fun AnimatedEntrance(
    visible: Boolean = true,
    delayMillis: Int = 0,
    content: @Composable () -> Unit
) {
    val hasAnimated = rememberSaveable { mutableStateOf(false) }
    val shouldAnimate = visible && !hasAnimated.value

    if (shouldAnimate) {
        LaunchedEffect(delayMillis) {
            kotlinx.coroutines.delay(delayMillis.toLong())
            hasAnimated.value = true
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (hasAnimated.value) 1f else 0f,
        animationSpec = tween(100, delayMillis),
        label = "entranceAlpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (hasAnimated.value) 0f else 12f,
        animationSpec = tween(120, delayMillis),
        label = "entranceOffset"
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                this.alpha = alpha
                this.translationY = offsetY
            }
    ) {
        content()
    }
}

/**
 * 错开动画列表
 */
@Composable
fun <T> StaggeredList(
    items: List<T>,
    modifier: Modifier = Modifier,
    itemContent: @Composable (index: Int, item: T) -> Unit
) {
    Column(modifier = modifier) {
        items.forEachIndexed { index, item ->
            AnimatedEntrance(
                delayMillis = index * 20
            ) {
                itemContent(index, item)
            }
        }
    }
}

/**
 * 智能洞察卡片组件
 */
@Composable
fun InsightCard(
    type: InsightType,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    onAction: (() -> Unit)? = null
) {
    val (icon, color) = when (type) {
        InsightType.Success -> Icons.AutoMirrored.Rounded.TrendingUp to MaterialTheme.colorScheme.tertiary
        InsightType.Warning -> Icons.Rounded.Warning to MaterialTheme.colorScheme.error
        InsightType.Info -> Icons.Rounded.Lightbulb to MaterialTheme.colorScheme.primary
        InsightType.Tip -> Icons.Rounded.Star to MaterialTheme.colorScheme.secondary
    }
    
    HierarchicalCard(
        importance = CardImportance.High,
        modifier = modifier,
        onClick = onAction
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (onAction != null) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

enum class InsightType {
    Success, Warning, Info, Tip
}

/**
 * 数字变化动画
 */
@Composable
fun AnimatedNumber(
    value: Double,
    formatter: (Double) -> String = { it.toString() },
    style: androidx.compose.ui.text.TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified
) {
    var displayedValue by remember { mutableDoubleStateOf(0.0) }
    
    LaunchedEffect(value) {
        animate(
            initialValue = displayedValue.toFloat(),
            targetValue = value.toFloat(),
            animationSpec = tween(800, easing = EaseOutQuart)
        ) { animatedValue, _ ->
            displayedValue = animatedValue.toDouble()
        }
    }
    
    Text(
        text = formatter(displayedValue),
        style = style,
        color = color
    )
}

private val EaseOutQuart = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
