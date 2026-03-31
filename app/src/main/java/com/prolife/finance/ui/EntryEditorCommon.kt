package com.prolife.finance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.prolife.finance.ui.theme.LedgerBloomTokens
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val EntryMonthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年M月")
private val EntryDateButtonFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M月d日")

internal fun parseEditorDate(raw: String): LocalDate =
    runCatching { LocalDate.parse(raw) }.getOrElse { LocalDate.now() }

internal fun entryDateButtonLabel(date: LocalDate): String =
    if (date == LocalDate.now()) "今天" else date.format(EntryDateButtonFormatter)

internal fun appendNumericToken(current: String, token: String, maxDecimals: Int): String {
    var next = current
    token.forEach { next = appendSingleNumericToken(next, it, maxDecimals) }
    return next
}

private fun appendSingleNumericToken(current: String, char: Char, maxDecimals: Int): String {
    val value = current.trim()
    return when (char) {
        '.' -> when {
            value.contains('.') -> value
            value.isBlank() -> "0."
            else -> "$value."
        }

        in '0'..'9' -> {
            val base = if (value == "0" && !value.contains('.')) "" else value
            val candidate = base + char
            val normalized = normalizeNumericString(candidate)
            if (normalized.contains('.')) {
                val decimals = normalized.substringAfter('.', missingDelimiterValue = "")
                if (decimals.length > maxDecimals) value else normalized
            } else {
                normalized
            }
        }

        else -> value
    }
}

private fun normalizeNumericString(value: String): String {
    if (value.isBlank()) return ""
    val hasTrailingDot = value.endsWith('.')
    val parts = value.split('.', limit = 2)
    val integerPart = parts[0].trimStart('0').ifBlank {
        if (value.startsWith('0') || value.startsWith('.')) "0" else ""
    }
    return when {
        parts.size == 1 -> integerPart.ifBlank { "0" }
        hasTrailingDot -> "$integerPart."
        else -> "$integerPart.${parts[1]}"
    }
}

internal fun deleteNumericToken(current: String): String = current.dropLast(1)

internal fun formatEntryCurrency(raw: String): String =
    raw.toDoubleOrNull()?.let(::formatCurrency) ?: if (raw.isBlank()) formatCurrency(0.0) else "¥$raw"

internal fun formatEntryCount(raw: String, suffix: String): String = when {
    raw.isBlank() -> "0$suffix"
    else -> "${raw.toBigDecimalOrNull()?.stripTrailingZeros()?.toPlainString() ?: raw}$suffix"
}

internal fun categoryIcon(iconName: String): ImageVector = when (iconName) {
    "utensils" -> Icons.Rounded.Restaurant
    "car" -> Icons.Rounded.DirectionsCar
    "bag" -> Icons.Rounded.ShoppingBag
    "gamepad" -> Icons.Rounded.SportsEsports
    "home" -> Icons.Rounded.Home
    "heart" -> Icons.Rounded.Favorite
    "school" -> Icons.Rounded.School
    "plane" -> Icons.Rounded.Flight
    "gift" -> Icons.Rounded.Redeem
    "phone" -> Icons.Rounded.Smartphone
    "mug-hot" -> Icons.Rounded.LocalDrink
    "coffee" -> Icons.Rounded.LocalCafe
    "salary" -> Icons.Rounded.Payments
    "award" -> Icons.Rounded.EmojiEvents
    "briefcase" -> Icons.Rounded.BusinessCenter
    "chart" -> Icons.Rounded.ShowChart
    "more" -> Icons.Rounded.MoreHoriz
    else -> Icons.Rounded.Label
}

internal fun defaultBackIcon(): ImageVector = Icons.Rounded.ArrowBack

@Composable
internal fun EntryFullscreenDialog(
    onDismiss: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                content = content
            )
        }
    }
}

@Composable
internal fun EntryHeaderBar(
    title: String,
    subtitle: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
    onNavigationClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (navigationIcon != null && onNavigationClick != null) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    onClick = onNavigationClick
                ) {
                    Box(
                        modifier = Modifier.size(42.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = navigationIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            onClick = onClose
        ) {
            Box(
                modifier = Modifier.size(42.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "关闭",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
internal fun EntryModeTabs(
    options: List<String>,
    selectedIndex: Int,
    accent: Color,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(999.dp),
                    color = if (selected) accent.copy(alpha = 0.16f) else Color.Transparent,
                    onClick = { onSelect(index) }
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun EntryTypeTabs(
    options: List<Pair<Any, String>>,
    selectedIndex: Int,
    accent: Color,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        options.forEachIndexed { index, (_, label) ->
            val selected = index == selectedIndex
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(index) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Surface(
                    modifier = Modifier
                        .width(72.dp)
                        .height(4.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = if (selected) accent.copy(alpha = 0.24f) else Color.Transparent
                ) {}
            }
        }
    }
}

@Composable
internal fun EntryPageDots(
    pageCount: Int,
    currentPage: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (pageCount <= 1) return
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (selected) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    )
                    .clickable { onSelect(index) }
            )
        }
    }
}

@Composable
internal fun EntryNumberPad(
    accent: Color,
    dateLabel: String,
    onDateClick: () -> Unit,
    onInput: (String) -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    doneLabel: String = "完成",
    topContent: (@Composable ColumnScope.() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            topContent?.invoke(this)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EntryPadButton(text = "7", onClick = { onInput("7") }, modifier = Modifier.weight(1f))
                EntryPadButton(text = "8", onClick = { onInput("8") }, modifier = Modifier.weight(1f))
                EntryPadButton(text = "9", onClick = { onInput("9") }, modifier = Modifier.weight(1f))
                EntryPadButton(
                    text = dateLabel,
                    icon = Icons.Rounded.Today,
                    onClick = onDateClick,
                    modifier = Modifier.weight(1f),
                    compact = true
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EntryPadButton(text = "4", onClick = { onInput("4") }, modifier = Modifier.weight(1f))
                EntryPadButton(text = "5", onClick = { onInput("5") }, modifier = Modifier.weight(1f))
                EntryPadButton(text = "6", onClick = { onInput("6") }, modifier = Modifier.weight(1f))
                EntryPadButton(text = "00", onClick = { onInput("00") }, modifier = Modifier.weight(1f), compact = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EntryPadButton(text = "1", onClick = { onInput("1") }, modifier = Modifier.weight(1f))
                EntryPadButton(text = "2", onClick = { onInput("2") }, modifier = Modifier.weight(1f))
                EntryPadButton(text = "3", onClick = { onInput("3") }, modifier = Modifier.weight(1f))
                EntryPadButton(icon = Icons.Rounded.Backspace, onClick = onDelete, modifier = Modifier.weight(1f), compact = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EntryPadButton(text = ".", onClick = { onInput(".") }, modifier = Modifier.weight(1f))
                EntryPadButton(text = "0", onClick = { onInput("0") }, modifier = Modifier.weight(1f))
                EntryPadButton(icon = Icons.Rounded.Clear, onClick = onClear, modifier = Modifier.weight(1f), compact = true)
                EntryPadButton(
                    text = doneLabel,
                    onClick = onDone,
                    modifier = Modifier.weight(1f),
                    accent = accent,
                    filled = true,
                    compact = true
                )
            }
        }
    }
}

@Composable
private fun EntryPadButton(
    text: String? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    filled: Boolean = false,
    compact: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        modifier = modifier.height(if (compact) 64.dp else 70.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (filled) accent else MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 3.dp,
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (icon != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                    text?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                Text(
                    text = text.orEmpty(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
internal fun EntryDatePickerDialog(
    initialDate: LocalDate,
    accent: Color,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    var displayMonth by remember(initialDate) { mutableStateOf(YearMonth.from(initialDate)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.32f))
                    .clickable(onClick = onDismiss)
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 12.dp,
                shadowElevation = 18.dp
            ) {
                Column(
                    modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "选择日期",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "关闭")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { displayMonth = displayMonth.minusMonths(1) }) {
                            Icon(Icons.Rounded.ChevronLeft, contentDescription = "上个月")
                        }
                        Text(
                            text = displayMonth.format(EntryMonthFormatter),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { displayMonth = displayMonth.plusMonths(1) }) {
                            Icon(Icons.Rounded.ChevronRight, contentDescription = "下个月")
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("一", "二", "三", "四", "五", "六", "日").forEachIndexed { index, label ->
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (index >= 5) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    buildCalendarRows(displayMonth).forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            week.forEach { date ->
                                val isSelected = date == initialDate
                                val isCurrentMonth = YearMonth.from(date) == displayMonth
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f),
                                    shape = RoundedCornerShape(18.dp),
                                    color = if (isSelected) accent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
                                    onClick = { onDateSelected(date) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = date.dayOfMonth.toString(),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = when {
                                                isSelected -> Color.White
                                                !isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
                                                else -> MaterialTheme.colorScheme.onSurface
                                            },
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun buildCalendarRows(month: YearMonth): List<List<LocalDate>> {
    val firstDay = month.atDay(1)
    val lastDay = month.atEndOfMonth()
    val startOffset = (firstDay.dayOfWeek.value - 1).toLong()
    val gridStart = firstDay.minusDays(startOffset)
    val endOffset = (7 - lastDay.dayOfWeek.value) % 7
    val gridEnd = lastDay.plusDays(endOffset.toLong())
    val days = mutableListOf<LocalDate>()
    var cursor = gridStart
    while (!cursor.isAfter(gridEnd)) {
        days += cursor
        cursor = cursor.plusDays(1)
    }
    return days.chunked(7)
}

@Composable
internal fun EntryTextEditDialog(
    title: String,
    value: String,
    placeholder: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    singleLine: Boolean = false,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    var draft by remember(value) { mutableStateOf(value) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            FilledTonalButton(onClick = { onConfirm(draft) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text(placeholder) },
                singleLine = singleLine,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = editorTextFieldColors(accent)
            )
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
internal fun EntrySettingRow(
    icon: ImageVector,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(58.dp),
            shape = CircleShape,
            color = tint.copy(alpha = 0.12f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        if (onDelete != null) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
internal fun EntryValuePill(
    label: String,
    value: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(22.dp)
    Surface(
        modifier = modifier.border(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) accent.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant,
            shape = shape
        ),
        shape = shape,
        color = if (selected) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
        tonalElevation = if (selected) 1.dp else 0.dp,
        shadowElevation = 0.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (selected) accent else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
internal fun EntryInfoRow(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.12f)
                ) {
                    Box(
                        modifier = Modifier.size(34.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = accent)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


