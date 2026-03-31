package com.prolife.finance.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class SwipeActionConfig(
    val threshold: Float = 0.35f,
    val actionWidth: Dp = 80.dp,
    val resistance: Float = 1f
)

enum class SwipeState {
    COLLAPSED,
    EXPANDED
}

@Composable
fun SwipeableItemWithActions(
    modifier: Modifier = Modifier,
    isRevealed: Boolean = false,
    actionCount: Int = 2,
    config: SwipeActionConfig = SwipeActionConfig(),
    actions: @Composable RowScope.() -> Unit,
    onExpanded: () -> Unit = {},
    onCollapsed: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val actionWidthPx = with(density) { config.actionWidth.toPx() * actionCount }
    val revealedOffset = if (layoutDirection == LayoutDirection.Rtl) actionWidthPx else -actionWidthPx
    val minOffset = min(revealedOffset, 0f)
    val maxOffset = max(revealedOffset, 0f)
    val restingOffset = if (isRevealed) revealedOffset else 0f

    var dragOffset by remember(isRevealed, revealedOffset) { mutableFloatStateOf(restingOffset) }
    val animatedOffset by animateIntOffsetAsState(
        targetValue = IntOffset(dragOffset.roundToInt(), 0),
        animationSpec = tween(220),
        label = "swipeOffset"
    )

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(20.dp)
                ),
            horizontalArrangement = if (layoutDirection == LayoutDirection.Rtl) Arrangement.Start else Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            actions()
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { animatedOffset }
                .pointerInput(isRevealed, actionWidthPx, config.threshold, config.resistance, layoutDirection) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            val next = (dragOffset + dragAmount * config.resistance).coerceIn(minOffset, maxOffset)
                            dragOffset = next
                            change.consume()
                        },
                        onDragEnd = {
                            val shouldReveal = abs(dragOffset) >= actionWidthPx * config.threshold
                            if (shouldReveal) {
                                dragOffset = revealedOffset
                                onExpanded()
                            } else {
                                dragOffset = 0f
                                onCollapsed()
                            }
                        },
                        onDragCancel = {
                            dragOffset = restingOffset
                        }
                    )
                }
        ) {
            content()
        }
    }
}

@Composable
fun SwipeableItemWithActionsSimple(
    modifier: Modifier = Modifier,
    isRevealed: Boolean = false,
    actions: @Composable RowScope.() -> Unit,
    onExpanded: () -> Unit = {},
    onCollapsed: () -> Unit = {},
    content: @Composable () -> Unit
) {
    SwipeableItemWithActions(
        modifier = modifier,
        isRevealed = isRevealed,
        actions = actions,
        onExpanded = onExpanded,
        onCollapsed = onCollapsed,
        content = content
    )
}

@Composable
fun DeleteSwipeAction(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(),
        label = "deleteScale"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(80.dp)
            .background(
                color = MaterialTheme.colorScheme.error,
                shape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)
            )
            .clickable(onClick = onDelete)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = "\u5220\u9664",
                tint = MaterialTheme.colorScheme.onError,
                modifier = Modifier.scale(scale)
            )
            Text(
                text = "\u5220\u9664",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onError
            )
        }
    }
}

@Composable
fun EditSwipeAction(
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(),
        label = "editScale"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(80.dp)
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
            )
            .clickable(onClick = onEdit)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = "\u7f16\u8f91",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.scale(scale)
            )
            Text(
                text = "\u7f16\u8f91",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun SwipeConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "\u786e\u8ba4",
    cancelText: String = "\u53d6\u6d88",
    confirmColor: Color = MaterialTheme.colorScheme.error,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = confirmText, color = confirmColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelText)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    )
}