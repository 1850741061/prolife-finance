package com.prolife.finance.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prolife.finance.model.StatsGranularity
import com.prolife.finance.model.TransactionType
import com.prolife.finance.ui.animations.AnimationEasing
import com.prolife.finance.ui.theme.LedgerBloomTokens
import com.prolife.finance.ui.designsystem.AnimatedEntrance
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import androidx.compose.ui.window.Dialog
import java.time.format.DateTimeFormatter
import java.time.YearMonth
import java.time.LocalDate
import androidx.compose.ui.window.DialogProperties
@Composable
fun StatsScreen(
    state: AppUiState,
    onAction: (AppAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPeriodPicker by rememberSaveable { mutableStateOf(false) }
    val analytics = remember(state.transactions, state.statsGranularity, state.statsAnchorDate) {
        buildAnalytics(state.transactions, state.statsGranularity, state.statsAnchorDate)
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StatsHeader(
                    granularity = state.statsGranularity,
                    periodLabel = analytics.periodLabel,
                    onOpenPeriodPicker = { showPeriodPicker = true },
                    onPrevious = { onAction(AppAction.PreviousStatsPeriod) },
                    onNext = { onAction(AppAction.NextStatsPeriod) }
                )
            }

            item {
                BalanceCard(
                    summary = analytics.summary,
                    periodTitle = when (state.statsGranularity) {
                        StatsGranularity.WEEK -> "本周结余"
                        StatsGranularity.MONTH -> "本月结余"
                        StatsGranularity.YEAR -> "本年结余"
                    },
                    previousPeriodLabel = when (state.statsGranularity) {
                        StatsGranularity.WEEK -> "上周结余"
                        StatsGranularity.MONTH -> "上月结余"
                        StatsGranularity.YEAR -> "上年结余"
                    },
                    previousBalance = analytics.previousPeriodBalance
                )
            }

            item {
                DrinkStatsInsightCard(
                    state = state,
                    onAction = onAction
                )
            }

            item {
                TrendCard(
                    buckets = analytics.trendBuckets,
                    granularity = state.statsGranularity,
                    anchorDate = state.statsAnchorDate,
                    allTransactions = state.transactions,
                    highlightBucket = analytics.highlightBucket,
                    activeDays = analytics.activeExpenseDays,
                    averageExpense = analytics.averageExpensePerBucket
                )
            }

            item {
                DonutChartCard(
                    slices = analytics.categorySlices,
                    totalExpense = analytics.summary.expense
                )
            }

            item {
                CategoryRankingCard(
                    entries = analytics.categoryRanking.take(5),
                    totalAmount = analytics.summary.expense
                )
            }

            item {
                TransactionRankingCard(
                    transactions = analytics.topExpenseTransactions.take(5)
                )
            }
        }

        if (showPeriodPicker) {
            StatsPeriodPickerDialog(
                initialGranularity = state.statsGranularity,
                initialAnchorDate = state.statsAnchorDate,
                preferredMonth = state.selectedMonth,
                onDismiss = { showPeriodPicker = false },
                onPeriodSelected = { granularity, anchorDate ->
                    onAction(AppAction.SelectStatsPeriod(granularity, anchorDate))
                    showPeriodPicker = false
                }
            )
        }
    }
}
@Composable
private fun StatsHeader(
    granularity: StatsGranularity,
    periodLabel: String,
    onOpenPeriodPicker: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                shadowElevation = 4.dp,
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .clickable(onClick = onOpenPeriodPicker)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = granularity.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "打开统计周期选择",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = periodLabel,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = "支出",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                    Text(
                        text = "收入",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevious) {
                    Text(
                        text = "‹",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = when (granularity) {
                        StatsGranularity.WEEK -> "周视图"
                        StatsGranularity.MONTH -> "月视图"
                        StatsGranularity.YEAR -> "年视图"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                IconButton(onClick = onNext) {
                    Text(
                        text = "›",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsPeriodPickerDialog(
    initialGranularity: StatsGranularity,
    initialAnchorDate: LocalDate,
    preferredMonth: YearMonth,
    onDismiss: () -> Unit,
    onPeriodSelected: (StatsGranularity, LocalDate) -> Unit
) {
    val today = remember { LocalDate.now() }
    val selectedWeekAnchor = remember(initialGranularity, initialAnchorDate, today) {
        if (initialGranularity == StatsGranularity.WEEK) startOfStatsWeek(initialAnchorDate) else startOfStatsWeek(today)
    }
    val selectedMonth = remember(initialGranularity, initialAnchorDate, preferredMonth) {
        if (initialGranularity == StatsGranularity.MONTH) YearMonth.from(initialAnchorDate) else preferredMonth
    }
    val selectedYear = remember(initialGranularity, initialAnchorDate, today) {
        if (initialGranularity == StatsGranularity.YEAR) initialAnchorDate.year else today.year
    }

    var pickerGranularity by remember(initialGranularity) { mutableStateOf(initialGranularity) }
    var weekDisplayMonth by remember(selectedWeekAnchor) { mutableStateOf(YearMonth.from(selectedWeekAnchor)) }
    var monthDisplayYear by remember(selectedMonth) { mutableIntStateOf(selectedMonth.year) }
    var yearPageEnd by remember(selectedYear) { mutableIntStateOf(selectedYear) }
    val pickerAccent = LedgerBloomTokens.palette.chartAccent

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
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PickerModeTab(
                            label = "按周查看",
                            selected = pickerGranularity == StatsGranularity.WEEK,
                            accent = pickerAccent,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                pickerGranularity = StatsGranularity.WEEK
                                weekDisplayMonth = YearMonth.from(selectedWeekAnchor)
                            }
                        )
                        PickerModeTab(
                            label = "按月查看",
                            selected = pickerGranularity == StatsGranularity.MONTH,
                            accent = pickerAccent,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                pickerGranularity = StatsGranularity.MONTH
                                monthDisplayYear = selectedMonth.year
                            }
                        )
                        PickerModeTab(
                            label = "按年查看",
                            selected = pickerGranularity == StatsGranularity.YEAR,
                            accent = pickerAccent,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                pickerGranularity = StatsGranularity.YEAR
                                yearPageEnd = selectedYear
                            }
                        )
                    }

                    when (pickerGranularity) {
                        StatsGranularity.WEEK -> {
                            PickerNavigationRow(
                                title = weekDisplayMonth.format(DateTimeFormatter.ofPattern("yyyy年 MM月")),
                                onPrevious = { weekDisplayMonth = weekDisplayMonth.minusMonths(1) },
                                onNext = { weekDisplayMonth = weekDisplayMonth.plusMonths(1) }
                            )
                            WeekdayHeader(accent = pickerAccent)
                            WeekCalendarGrid(
                                displayMonth = weekDisplayMonth,
                                selectedWeekAnchor = selectedWeekAnchor,
                                accent = pickerAccent,
                                onSelectDate = { onPeriodSelected(StatsGranularity.WEEK, it) }
                            )
                        }

                        StatsGranularity.MONTH -> {
                            PickerNavigationRow(
                                title = "${monthDisplayYear}年",
                                onPrevious = { monthDisplayYear -= 1 },
                                onNext = { monthDisplayYear += 1 }
                            )
                            MonthGrid(
                                displayYear = monthDisplayYear,
                                selectedMonth = selectedMonth,
                                accent = pickerAccent,
                                onSelectMonth = { onPeriodSelected(StatsGranularity.MONTH, it.atDay(1)) }
                            )
                        }

                        StatsGranularity.YEAR -> {
                            PickerNavigationRow(
                                title = "年份",
                                onPrevious = { yearPageEnd -= 12 },
                                onNext = { yearPageEnd += 12 }
                            )
                            YearGrid(
                                pageEndYear = yearPageEnd,
                                selectedYear = selectedYear,
                                accent = pickerAccent,
                                onSelectYear = { onPeriodSelected(StatsGranularity.YEAR, LocalDate.of(it, 1, 1)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerModeTab(
    label: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .width(54.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (selected) accent else Color.Transparent)
        )
    }
}

@Composable
private fun PickerNavigationRow(
    title: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Text(
                text = "‹",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onNext) {
            Text(
                text = "›",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun WeekdayHeader(accent: Color) {
    val labels = listOf("一", "二", "三", "四", "五", "六", "日")
    Row(modifier = Modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (index >= 5) accent else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun WeekCalendarGrid(
    displayMonth: YearMonth,
    selectedWeekAnchor: LocalDate,
    accent: Color,
    onSelectDate: (LocalDate) -> Unit
) {
    val rows = remember(displayMonth) { buildWeekCalendarRows(displayMonth) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEachIndexed { index, date ->
                    val isSelected = startOfStatsWeek(date) == selectedWeekAnchor
                    val isCurrentMonth = YearMonth.from(date) == displayMonth
                    val shape = when {
                        !isSelected -> RoundedCornerShape(0.dp)
                        index == 0 -> RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                        index == 6 -> RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                        else -> RoundedCornerShape(0.dp)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 2.dp)
                            .clip(shape)
                            .background(if (isSelected) accent else Color.Transparent)
                            .clickable { onSelectDate(date) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = date.dayOfMonth.toString().padStart(2, '0'),
                            style = MaterialTheme.typography.headlineSmall,
                            color = when {
                                isSelected -> Color.White
                                !isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                index >= 5 -> accent
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthGrid(
    displayYear: Int,
    selectedMonth: YearMonth,
    accent: Color,
    onSelectMonth: (YearMonth) -> Unit
) {
    val months = (1..12).map { YearMonth.of(displayYear, it) }.chunked(3)
    PickerGrid(
        rows = months,
        accent = accent,
        isSelected = { it == selectedMonth },
        label = { "${it.monthValue}月" },
        onClick = onSelectMonth
    )
}

@Composable
private fun YearGrid(
    pageEndYear: Int,
    selectedYear: Int,
    accent: Color,
    onSelectYear: (Int) -> Unit
) {
    val years = ((pageEndYear - 11)..pageEndYear).toList().chunked(3)
    PickerGrid(
        rows = years,
        accent = accent,
        isSelected = { it == selectedYear },
        label = { it.toString() },
        onClick = onSelectYear
    )
}

@Composable
private fun <T> PickerGrid(
    rows: List<List<T>>,
    accent: Color,
    isSelected: (T) -> Boolean,
    label: (T) -> String,
    onClick: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                rowItems.forEach { item ->
                    val selected = isSelected(item)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(78.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onClick(item) },
                        color = if (selected) accent else MaterialTheme.colorScheme.surface,
                        tonalElevation = if (selected) 4.dp else 0.dp,
                        shadowElevation = if (selected) 4.dp else 0.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label(item),
                                style = MaterialTheme.typography.headlineSmall,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun buildWeekCalendarRows(displayMonth: YearMonth): List<List<LocalDate>> {
    val firstDay = displayMonth.atDay(1)
    val lastDay = displayMonth.atEndOfMonth()
    val gridStart = startOfStatsWeek(firstDay)
    val trailingDays = (7 - lastDay.dayOfWeek.value) % 7
    val gridEnd = lastDay.plusDays(trailingDays.toLong())
    val dates = mutableListOf<LocalDate>()
    var cursor = gridStart
    while (!cursor.isAfter(gridEnd)) {
        dates += cursor
        cursor = cursor.plusDays(1)
    }
    return dates.chunked(7)
}

private fun startOfStatsWeek(date: LocalDate): LocalDate {
    return date.minusDays((date.dayOfWeek.value - 1).toLong())
}

/**
  *
 */
@Composable
private fun BalanceCard(
    summary: MoneySummary,
    periodTitle: String,
    previousPeriodLabel: String,
    previousBalance: Double
) {
    val expenseColor = LedgerBloomTokens.palette.expense
    val incomeColor = LedgerBloomTokens.palette.income
    val isNegative = summary.balance < 0
    val isPrevNegative = previousBalance < 0

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = periodTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$previousPeriodLabel: ${if (isPrevNegative) "-" else ""}¥ ${formatAmount(kotlin.math.abs(previousBalance))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (isNegative) "-" else "",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = expenseColor
                )
                Text(
                    text = "¥ ${formatAmount(kotlin.math.abs(summary.balance))}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isNegative) expenseColor else incomeColor
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "支出",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(48.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        val expenseProgress = if (summary.expense + summary.income > 0) {
                            (summary.expense / (summary.expense + summary.income)).toFloat()
                        } else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(expenseProgress.coerceIn(0f, 1f))
                                .height(8.dp)
                                .background(expenseColor)
                        )
                    }
                    Text(
                        text = formatCurrency(summary.expense),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "收入",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(48.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        val incomeProgress = if (summary.expense + summary.income > 0) {
                            (summary.income / (summary.expense + summary.income)).toFloat()
                        } else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(incomeProgress.coerceIn(0f, 1f))
                                .height(8.dp)
                                .background(incomeColor)
                        )
                    }
                    Text(
                        text = if (summary.income > 0) formatCurrency(summary.income) else "暂无",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (summary.income > 0) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
  *
 */
@Composable
private fun TrendCard(
    buckets: List<ExpenseBucket>,
    granularity: StatsGranularity,
    anchorDate: java.time.LocalDate,
    allTransactions: List<com.prolife.finance.model.TransactionRecord>,
    highlightBucket: ExpenseBucket?,
    activeDays: Int,
    averageExpense: Double
) {
    var showCalendarView by rememberSaveable { mutableStateOf(false) }
    val periodPrefix = when (granularity) {
        StatsGranularity.WEEK -> "\u672c\u5468"
        StatsGranularity.MONTH -> "\u672c\u6708"
        StatsGranularity.YEAR -> "\u672c\u5e74"
    }

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "\u652f\u51fa\u8d8b\u52bf\u6982\u51b5",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !showCalendarView,
                        onClick = { showCalendarView = false },
                        label = { Text("\u56fe\u8868") },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.ShowChart,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                    FilterChip(
                        selected = showCalendarView,
                        onClick = { showCalendarView = true },
                        label = { Text("\u65e5\u5386") },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }

            highlightBucket?.takeIf { it.amount > 0 }?.let { bucket ->
                Column {
                    Text(
                        text = "${periodPrefix}\u5185\u6700\u9ad8\u652f\u51fa",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row {
                        Text(
                            text = "\u5728",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = trendHighlightLabel(bucket, granularity),
                            style = MaterialTheme.typography.bodyMedium,
                            color = LedgerBloomTokens.palette.expense,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "\uff0c\u652f\u51fa\u8fbe\u5230",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = formatCurrency(bucket.amount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = LedgerBloomTokens.palette.expense,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = when (granularity) {
                            StatsGranularity.YEAR -> "${periodPrefix}\u5e73\u5747\u6bcf\u6708\u652f\u51fa"
                            else -> "${periodPrefix}\u5e73\u5747\u6bcf\u65e5\u652f\u51fa"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(averageExpense),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = when (granularity) {
                            StatsGranularity.YEAR -> "${periodPrefix}\u7d2f\u8ba1\u652f\u51fa\u6708\u4efd"
                            else -> "${periodPrefix}\u7d2f\u8ba1\u652f\u51fa\u5929\u6570"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$activeDays ${if (granularity == StatsGranularity.YEAR) "\u4e2a\u6708" else "\u5929"}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (showCalendarView) {
                CalendarExpenseViewWithDetail(
                    buckets = buckets,
                    granularity = granularity,
                    anchorDate = anchorDate,
                    allTransactions = allTransactions
                )
            } else {
                if (buckets.isNotEmpty()) {
                    SimpleLineChart(buckets = buckets, averageAmount = averageExpense)
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "\u6682\u65e0\u6570\u636e",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun trendHighlightLabel(bucket: ExpenseBucket, granularity: StatsGranularity): String {
    return when (granularity) {
        StatsGranularity.WEEK -> bucket.dateLabel.ifBlank { bucket.label }
        StatsGranularity.MONTH -> bucket.label
        StatsGranularity.YEAR -> bucket.label
    }
}

/**
  *
  *
 */
@Composable
private fun SimpleLineChart(
    buckets: List<ExpenseBucket>,
    averageAmount: Double
) {
    val maxAmount = remember(buckets) { buckets.maxOfOrNull { it.amount }?.takeIf { it > 0 } ?: 1.0 }
    val resolvedAverageAmount = averageAmount.coerceAtLeast(0.0)
    val avgRatio = if (maxAmount > 0) (resolvedAverageAmount / maxAmount).toFloat().coerceIn(0f, 1f) else 0f
    val showAvgLine = resolvedAverageAmount > 0
    val avgLabelTop = trendAverageLabelTop(avgRatio)
    val peakIndex = remember(buckets) { if (buckets.isEmpty()) -1 else buckets.indices.maxByOrNull { buckets[it].amount } ?: -1 }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var lockedIndex by remember { mutableStateOf<Int?>(null) }
    var tooltipSize by remember { mutableStateOf(IntSize.Zero) }
    val haptic = LocalHapticFeedback.current

    // Cache density conversions — avoids dp.toPx() on every draw frame
    val density = LocalDensity.current
    val topPaddingPx = with(density) { 8.dp.toPx() }
    val bottomPaddingPx = 0f
    val peakDotRadiusPx = with(density) { 6.dp.toPx() }
    val normalDotRadiusPx = with(density) { 4.dp.toPx() }
    val lineStrokeWidthPx = with(density) { 3.dp.toPx() }
    val tooltipMarginPx = with(density) { 10.dp.toPx() }

    // Cache immutable drawing objects — avoids allocations on every draw frame
    val expenseColor = LedgerBloomTokens.palette.expense
    val lightExpenseColor = remember(expenseColor) { expenseColor.copy(alpha = 0.26f) }
    val highlightColor = LedgerBloomTokens.palette.chartAccent
    val gridColor = remember { Color.LightGray.copy(alpha = 0.2f) }
    val avgLineColor = remember { Color(0xFFFF69B4).copy(alpha = 0.7f) }
    val avgDashEffect = remember { androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f) }
    val lineStroke = remember(lineStrokeWidthPx) {
        androidx.compose.ui.graphics.drawscope.Stroke(width = lineStrokeWidthPx, cap = StrokeCap.Round)
    }
    val gradientColors = remember(lightExpenseColor) { listOf(lightExpenseColor, Color.Transparent) }

    val chartDescription = remember(buckets) {
        buildString {
            append("支出趋势图。")
            if (buckets.isNotEmpty()) {
                val peak = buckets.maxByOrNull { it.amount }
                append("共 ${buckets.size} 个数据点，")
                peak?.let { append("峰值 ${it.label} 为 ${formatCurrency(it.amount)}") }
            } else {
                append("暂无数据")
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .semantics { contentDescription = chartDescription }
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(150.dp)
        ) {
            Text(
                text = formatAxisLabel(maxAmount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 8.dp)
            )
            if (showAvgLine) {
                Text(
                    text = formatAxisLabel(resolvedAverageAmount),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFF69B4).copy(alpha = 0.85f),
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = avgLabelTop.dp)
                )
            }
            Text(
                text = "0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(150.dp)
        ) {
            val chartWidthPx = with(density) { maxWidth.toPx() }
            val chartHeightPx = with(density) { maxHeight.toPx() }
            val displayIndex = selectedIndex ?: lockedIndex
            val selectedPoint = displayIndex
                ?.takeIf { it in buckets.indices }
                ?.let { index ->
                    lineChartPoint(
                        index = index,
                        itemCount = buckets.size,
                        amount = buckets[index].amount,
                        maxAmount = maxAmount,
                        widthPx = chartWidthPx,
                        heightPx = chartHeightPx,
                        topPaddingPx = topPaddingPx,
                        bottomPaddingPx = bottomPaddingPx,
                        progress = 1f
                    )
                }
            val tooltipOffset = if (selectedPoint != null && tooltipSize != IntSize.Zero) {
                lineChartTooltipOffset(
                    point = selectedPoint,
                    tooltipWidthPx = tooltipSize.width,
                    tooltipHeightPx = tooltipSize.height,
                    containerWidthPx = chartWidthPx,
                    containerHeightPx = chartHeightPx,
                    marginPx = tooltipMarginPx
                )
            } else {
                androidx.compose.ui.unit.IntOffset.Zero
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(buckets) {
                        detectTapGestures(
                            onTap = { offset ->
                                if (buckets.isEmpty()) return@detectTapGestures
                                selectedIndex = chartIndexFromTap(offset.x, size.width.toFloat(), buckets.size)
                            },
                            onLongPress = { offset ->
                                if (buckets.isEmpty()) return@detectTapGestures
                                val index = chartIndexFromTap(offset.x, size.width.toFloat(), buckets.size)
                                lockedIndex = if (lockedIndex == index) null else index
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        )
                    }
            ) {
                val width = size.width
                val height = size.height
                val chartHeight = height - topPaddingPx - bottomPaddingPx

                drawLine(color = gridColor, start = Offset(0f, topPaddingPx), end = Offset(width, topPaddingPx), strokeWidth = 1f)
                drawLine(color = gridColor, start = Offset(0f, topPaddingPx + chartHeight), end = Offset(width, topPaddingPx + chartHeight), strokeWidth = 1f)

                if (showAvgLine) {
                    val avgY = topPaddingPx + chartHeight * (1 - avgRatio)
                    drawLine(color = avgLineColor, start = Offset(0f, avgY), end = Offset(width, avgY), strokeWidth = 2f, pathEffect = avgDashEffect)
                }

                val points = if (buckets.isEmpty()) {
                    emptyList()
                } else {
                    buckets.mapIndexed { index, bucket ->
                        lineChartPoint(
                            index = index,
                            itemCount = buckets.size,
                            amount = bucket.amount,
                            maxAmount = maxAmount,
                            widthPx = width,
                            heightPx = height,
                            topPaddingPx = topPaddingPx,
                            bottomPaddingPx = bottomPaddingPx,
                            progress = 1f
                        )
                    }
                }

                if (points.size > 1) {
                    val fillPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(points.first().x, height - bottomPaddingPx)
                        points.forEach { lineTo(it.x, it.y) }
                        lineTo(points.last().x, height - bottomPaddingPx)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = gradientColors,
                            startY = topPaddingPx,
                            endY = height - bottomPaddingPx
                        )
                    )

                    val linePath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(points.first().x, points.first().y)
                        points.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(path = linePath, color = expenseColor, style = lineStroke)
                }

                points.forEachIndexed { index, point ->
                    val isPeak = index == peakIndex && buckets.isNotEmpty() && buckets[index].amount > 0
                    val isSelected = index == selectedIndex || index == lockedIndex
                    val baseRadius = if (isPeak) peakDotRadiusPx else normalDotRadiusPx
                    val radius = if (isSelected) baseRadius * 1.5f else baseRadius
                    val pointColor = if (isSelected) highlightColor else expenseColor

                    if (isPeak || isSelected) {
                        drawCircle(color = pointColor.copy(alpha = if (isSelected) 0.3f else 0.2f), radius = radius * 2.5f, center = point)
                    }

                    drawCircle(color = Color.White, radius = radius, center = point)
                    drawCircle(color = pointColor, radius = radius * 0.6f, center = point)
                }
            }

            val peakBucket = remember(buckets) { buckets.maxByOrNull { it.amount } }
            peakBucket?.takeIf { it.amount > 0 }?.let { bucket ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = expenseColor.copy(alpha = 0.1f),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = formatCurrency(bucket.amount),
                        style = MaterialTheme.typography.labelMedium,
                        color = expenseColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (displayIndex != null && displayIndex < buckets.size) {
                val bucket = buckets[displayIndex]
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .onSizeChanged { tooltipSize = it }
                        .offset { tooltipOffset }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = bucket.dateLabel.ifEmpty { bucket.label },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                        Text(
                            text = formatCurrency(bucket.amount),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun trendAverageLabelTop(avgRatio: Float): Float {
    val containerHeight = 150f
    val topPadding = 8f
    val bottomPadding = 0f
    val labelHeight = 16f
    val labelGap = 4f
    val chartHeight = containerHeight - topPadding - bottomPadding
    val lineCenter = topPadding + chartHeight * (1f - avgRatio)
    val topLimit = topPadding + labelHeight + labelGap
    val bottomZeroLabelTop = containerHeight - bottomPadding - labelHeight
    val bottomLimit = bottomZeroLabelTop - labelHeight - labelGap
    return (lineCenter - labelHeight / 2f).coerceIn(topLimit, bottomLimit)
}

private fun formatAxisLabel(amount: Double): String {
    return when {
        amount >= 10000 -> String.format("%.0fw", amount / 10000)
        else -> String.format("%.0f", amount)
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
private fun DonutChartCard(
    slices: List<CategorySlice>,
    totalExpense: Double
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "支出占比概况",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            ExpenseShareDonut(slices = slices)
        }
    }
}

/**
  *
 */
@Composable
private fun CategoryRankingCard(
    entries: List<CategoryRankingEntry>,
    totalAmount: Double
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "支出类目排行",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (entries.isEmpty()) {
                Text(
                    text = "暂无支出数据",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@ElevatedCard
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                entries.forEach { entry ->
                    val progress = if (totalAmount > 0) (entry.amount / totalAmount).toFloat() else 0f
                    CategoryRankingItem(
                        entry = entry,
                        progress = progress
                    )
                }
            }
        }
    }
}

/**
  *
 */
@Composable
private fun CategoryRankingItem(
    entry: CategoryRankingEntry,
    progress: Float
) {
    val barColor = entry.color

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = barColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                    }
                }

                Column {
                    Text(
                        text = entry.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${formatCurrency(entry.amount)} · ${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(barColor)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0.001f, 1f))
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(barColor)
            )
        }
    }
}

/**
  *
 */
@Composable
private fun TransactionRankingCard(
    transactions: List<com.prolife.finance.model.TransactionRecord>
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "支出明细排行",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (transactions.isEmpty()) {
                Text(
                    text = "暂无支出明细",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@ElevatedCard
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                transactions.forEach { transaction ->
                    TransactionRankingItem(transaction = transaction)
                }
            }
        }
    }
}

/**
  *
 */
@Composable
private fun TransactionRankingItem(
    transaction: com.prolife.finance.model.TransactionRecord
) {
    val categoryColor = colorFromHex(transaction.colorHex)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(categoryColor)
            )

            Column {
                Text(
                    text = transaction.category,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${transaction.date}${if (transaction.note.isNotBlank()) " · ${transaction.note}" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        Text(
            text = "- ${formatCurrency(transaction.amount)}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = LedgerBloomTokens.palette.expense
        )
    }
}

/**
  *
 */
private fun formatShortAmount(amount: Double): String {
    return when {
        amount >= 10000 -> "${(amount / 10000).toInt()}万"
        amount >= 1000 -> "${(amount / 1000).toInt()}k"
        else -> "${amount.toInt()}"
    }
}

/**
  *
 */
private fun formatAmount(amount: Double): String {
    return String.format("%.2f", amount)
}

@Composable
private fun ExpenseShareDonut(slices: List<CategorySlice>) {
    val topThree = remember(slices) { slices.take(3) }
    val othersShare = remember(slices) { slices.drop(3).sumOf { it.ratio } }
    val topThreeShare = remember(topThree) { topThree.sumOf { it.ratio } }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(108.dp),
            contentAlignment = Alignment.Center
        ) {
            // Cache dp conversion and Stroke object
            val density = LocalDensity.current
            val strokeWidthPx = with(density) { 14.dp.toPx() }
            val donutStroke = remember(strokeWidthPx) {
                Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            }
            val othersColor = remember { Color.Gray.copy(alpha = 0.28f) }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val diameter = size.minDimension - strokeWidthPx
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                val arcSize = Size(diameter, diameter)
                var startAngle = -90f

                topThree.forEach { slice ->
                    val sweep = (slice.ratio * 360f).toFloat()
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = donutStroke
                    )
                    startAngle += sweep
                }

                if (othersShare > 0) {
                    drawArc(
                        color = othersColor,
                        startAngle = startAngle,
                        sweepAngle = (othersShare * 360f).toFloat(),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = donutStroke
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = formatPercent(topThreeShare), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "\u524d\u4e09", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            topThree.forEach { slice ->
                ExpenseLegendItem(name = slice.name, percent = formatPercent(slice.ratio), color = slice.color)
            }
            if (othersShare > 0) {
                ExpenseLegendItem(name = "\u5176\u4ed6", percent = formatPercent(othersShare), color = Color.Gray.copy(alpha = 0.28f))
            }
        }
    }
}

@Composable
private fun ExpenseLegendItem(name: String, percent: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
        Text(
            text = percent,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}