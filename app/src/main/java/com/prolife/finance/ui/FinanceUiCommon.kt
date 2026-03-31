package com.prolife.finance.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prolife.finance.model.StatsGranularity
import com.prolife.finance.model.TransactionRecord
import com.prolife.finance.model.TransactionType
import com.prolife.finance.ui.theme.LedgerBloomTokens
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

val MonthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
val DayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd")
private val CurrencyFormatter = DecimalFormat("#,##0.00")
private val PercentFormatter = DecimalFormat("0")

data class MoneySummary(
    val income: Double,
    val expense: Double
) {
    val balance: Double
        get() = income - expense
}

data class ExpenseBucket(
    val label: String,
    val amount: Double,
    val dateLabel: String = ""
)

data class CategorySlice(
    val name: String,
    val amount: Double,
    val ratio: Double,
    val color: Color
)

data class CategoryRankingEntry(
    val name: String,
    val amount: Double,
    val color: Color
)
data class InsightMetric(
    val label: String,
    val value: String,
    val accent: Color
)

data class AnalyticsBundle(
    val summaryTitle: String,
    val periodLabel: String,
    val summary: MoneySummary,
    val previousPeriodBalance: Double,
    val trendBuckets: List<ExpenseBucket>,
    val highlightBucket: ExpenseBucket?,
    val activeExpenseDays: Int,
    val averageExpensePerBucket: Double,
    val categorySlices: List<CategorySlice>,
    val topThreeExpenseShare: Double,
    val categoryRanking: List<CategoryRankingEntry>,
    val leadingCategory: CategoryRankingEntry?,
    val topExpenseTransactions: List<TransactionRecord>
)

fun summarize(transactions: List<TransactionRecord>): MoneySummary {
    var income = 0.0
    var expense = 0.0
    for (t in transactions) {
        when (t.type) {
            TransactionType.INCOME -> income += t.amount
            TransactionType.EXPENSE -> expense += t.amount
        }
    }
    return MoneySummary(income = income, expense = expense)
}

fun formatCurrency(value: Double): String = "\u00A5${CurrencyFormatter.format(value)}"

fun formatPercent(value: Double): String = "${PercentFormatter.format(value * 100)}%"

fun formatStatsLabel(granularity: StatsGranularity, anchorDate: LocalDate): String {
    return when (granularity) {
        StatsGranularity.WEEK -> {
            val start = anchorDate.minusDays((anchorDate.dayOfWeek.value - 1).toLong())
            val end = start.plusDays(6)
            "${start.format(DayFormatter)} - ${end.format(DayFormatter)}"
        }
        StatsGranularity.MONTH -> YearMonth.from(anchorDate).format(MonthFormatter)
        StatsGranularity.YEAR -> "${anchorDate.year}"
    }
}

private val colorCache = java.util.concurrent.ConcurrentHashMap<String, Color>()

fun colorFromHex(hex: String): Color {
    return colorCache.getOrPut(hex) {
        try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (_: IllegalArgumentException) {
            Color(0xFF94A3B8)
        }
    }
}

fun buildAnalytics(
    transactions: List<TransactionRecord>,
    granularity: StatsGranularity,
    anchorDate: LocalDate
): AnalyticsBundle {
    val filtered = filterTransactionsForPeriod(transactions, granularity, anchorDate)
    val summary = summarize(filtered)
    val expenses = filtered.filter { it.type == TransactionType.EXPENSE }
    val buckets = buildExpenseBuckets(expenses, granularity, anchorDate)
    val peak = buckets.maxByOrNull { it.amount }
    val activeExpenseDays = buckets.count { it.amount > 0.0 }
    val averageDivisor = averageBucketCount(granularity, anchorDate)
    val averageExpensePerBucket = if (averageDivisor == 0) 0.0 else summary.expense / averageDivisor

    val previousPeriodBalance = calculatePreviousPeriodBalance(transactions, granularity, anchorDate)

    val categoryTotals = expenses.groupBy { it.category }
        .map { (category, records) ->
            CategoryRankingEntry(
                name = category,
                amount = records.sumOf { it.amount },
                color = colorFromHex(records.firstOrNull()?.colorHex ?: "#6B7280")
            )
        }
        .sortedByDescending { it.amount }

    val topThree = categoryTotals.take(3)
    val othersTotal = categoryTotals.drop(3).sumOf { it.amount }
    val sliceBase = topThree.sumOf { it.amount } + othersTotal
    val slices = buildList {
        topThree.forEach { entry ->
            add(
                CategorySlice(
                    name = entry.name,
                    amount = entry.amount,
                    ratio = if (sliceBase == 0.0) 0.0 else entry.amount / sliceBase,
                    color = entry.color
                )
            )
        }
        if (othersTotal > 0.0) {
            add(
                CategorySlice(
                    name = "Other",
                    amount = othersTotal,
                    ratio = if (sliceBase == 0.0) 0.0 else othersTotal / sliceBase,
                    color = Color(0xFF94A3B8)
                )
            )
        }
    }

    return AnalyticsBundle(
        summaryTitle = when (granularity) {
            StatsGranularity.WEEK -> "\u672c\u5468\u7edf\u8ba1"
            StatsGranularity.MONTH -> "\u672c\u6708\u7edf\u8ba1"
            StatsGranularity.YEAR -> "\u672c\u5e74\u7edf\u8ba1"
        },
        periodLabel = formatStatsLabel(granularity, anchorDate),
        summary = summary,
        previousPeriodBalance = previousPeriodBalance,
        trendBuckets = buckets,
        highlightBucket = peak,
        activeExpenseDays = activeExpenseDays,
        averageExpensePerBucket = averageExpensePerBucket,
        categorySlices = slices,
        topThreeExpenseShare = if (summary.expense == 0.0) 0.0 else topThree.sumOf { it.amount } / summary.expense,
        categoryRanking = categoryTotals,
        leadingCategory = categoryTotals.firstOrNull(),
        topExpenseTransactions = expenses.sortedByDescending { it.amount }.take(5)
    )
}

private fun averageBucketCount(
    granularity: StatsGranularity,
    anchorDate: LocalDate
): Int {
    val today = LocalDate.now()
    return when (granularity) {
        StatsGranularity.WEEK -> {
            val start = anchorDate.minusDays((anchorDate.dayOfWeek.value - 1).toLong())
            val end = start.plusDays(6)
            if (!today.isBefore(start) && !today.isAfter(end)) {
                (today.toEpochDay() - start.toEpochDay() + 1).toInt()
            } else {
                7
            }
        }
        StatsGranularity.MONTH -> {
            val month = YearMonth.from(anchorDate)
            if (YearMonth.from(today) == month) today.dayOfMonth else month.lengthOfMonth()
        }
        StatsGranularity.YEAR -> if (today.year == anchorDate.year) today.monthValue else 12
    }
}


/**
  *
  *
 */
private fun calculatePreviousPeriodBalance(
    transactions: List<TransactionRecord>,
    granularity: StatsGranularity,
    anchorDate: LocalDate
): Double {
    val previousAnchorDate = when (granularity) {
        StatsGranularity.WEEK -> anchorDate.minusWeeks(1)
        StatsGranularity.MONTH -> anchorDate.minusMonths(1)
        StatsGranularity.YEAR -> anchorDate.minusYears(1)
    }
    val previousTransactions = filterTransactionsForPeriod(transactions, granularity, previousAnchorDate)
    return summarize(previousTransactions).balance
}

private fun filterTransactionsForPeriod(
    transactions: List<TransactionRecord>,
    granularity: StatsGranularity,
    anchorDate: LocalDate
): List<TransactionRecord> {
    return when (granularity) {
        StatsGranularity.WEEK -> {
            val start = anchorDate.minusDays((anchorDate.dayOfWeek.value - 1).toLong())
            val end = start.plusDays(6)
            transactions.filter { !it.date.isBefore(start) && !it.date.isAfter(end) }
        }
        StatsGranularity.MONTH -> {
            val target = YearMonth.from(anchorDate)
            transactions.filter { YearMonth.from(it.date) == target }
        }
        StatsGranularity.YEAR -> transactions.filter { it.date.year == anchorDate.year }
    }
}

private fun buildExpenseBuckets(
    expenses: List<TransactionRecord>,
    granularity: StatsGranularity,
    anchorDate: LocalDate
): List<ExpenseBucket> {
    return when (granularity) {
        StatsGranularity.WEEK -> {
            val start = anchorDate.minusDays((anchorDate.dayOfWeek.value - 1).toLong())
            val grouped = expenses.groupBy { it.date }
            (0..6).map { offset ->
                val day = start.plusDays(offset.toLong())
                ExpenseBucket(
                    label = day.format(DateTimeFormatter.ofPattern("E")),
                    amount = grouped[day].orEmpty().sumOf { it.amount },
                    dateLabel = day.format(DayFormatter)
                )
            }
        }
        StatsGranularity.MONTH -> {
            val month = YearMonth.from(anchorDate)
            val grouped = expenses.groupBy { it.date }
            (1..month.lengthOfMonth()).map { dayOfMonth ->
                val date = month.atDay(dayOfMonth)
                ExpenseBucket(
                    label = "${dayOfMonth}\u65e5",
                    amount = grouped[date].orEmpty().sumOf { it.amount },
                    dateLabel = date.format(DayFormatter)
                )
            }
        }
        StatsGranularity.YEAR -> {
            val grouped = expenses.groupBy { it.date.monthValue }
            (1..12).map { month ->
                ExpenseBucket(
                    label = "${month}\u6708",
                    amount = grouped[month].orEmpty().filter { it.date.year == anchorDate.year }.sumOf { it.amount },
                    dateLabel = YearMonth.of(anchorDate.year, month).format(MonthFormatter)
                )
            }
        }
    }
}
@Composable
fun MonthNavigator(
    title: String,
    subtitle: String = "Selected Period",
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenPicker: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)) {
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous")
                }
            }
            Column(
                modifier = if (onOpenPicker != null) {
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = onOpenPicker)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                } else {
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (onOpenPicker != null) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Open period picker",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)) {
                IconButton(onClick = onNext) {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = "Next")
                }
            }
        }
    }
}

@Composable
fun SummaryHeroCard(title: String, subtitle: String, summary: MoneySummary) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = LedgerBloomTokens.palette.hero
                ) {
                    Text(
                        text = subtitle,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HeroMetric(
                    label = "\u7ed3\u4f59",
                    value = formatCurrency(summary.balance),
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.weight(1f)
                )
                HeroMetric(
                    label = "\u652f\u51fa",
                    value = formatCurrency(summary.expense),
                    valueColor = LedgerBloomTokens.palette.expense,
                    containerColor = LedgerBloomTokens.palette.expense.copy(alpha = 0.08f),
                    modifier = Modifier.weight(1f)
                )
                HeroMetric(
                    label = "\u6536\u5165",
                    value = formatCurrency(summary.income),
                    valueColor = LedgerBloomTokens.palette.income,
                    containerColor = LedgerBloomTokens.palette.income.copy(alpha = 0.08f),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}


@Composable
private fun HeroMetric(
    label: String,
    value: String,
    valueColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable
fun TransactionCard(
    transaction: TransactionRecord,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val amountColor = if (transaction.type == TransactionType.INCOME) LedgerBloomTokens.palette.income else LedgerBloomTokens.palette.expense
    val badgeColor = colorFromHex(transaction.colorHex)
    val amountLabel = if (transaction.type == TransactionType.INCOME) "+${formatCurrency(transaction.amount)}" else "-${formatCurrency(transaction.amount)}"

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)),
        elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(badgeColor)
                        )
                        Text(transaction.category, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = amountColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = if (transaction.type == TransactionType.INCOME) "\u6536\u5165" else "\u652f\u51fa",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                color = amountColor,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (transaction.note.isNotBlank()) {
                        Text(
                            text = transaction.note,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = transaction.date.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = amountLabel,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = amountColor
                    )
                    if (onEdit != null || onDelete != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            onEdit?.let { editAction ->
                                IconButton(onClick = editAction, modifier = Modifier.size(36.dp)) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "\u7f16\u8f91",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            onDelete?.let { deleteAction ->
                                IconButton(onClick = deleteAction, modifier = Modifier.size(36.dp)) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = "\u5220\u9664",
                                        tint = MaterialTheme.colorScheme.onSurface
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



@Composable
fun EmptyStateCard(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!actionLabel.isNullOrBlank() && onAction != null) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.clickable(onClick = onAction)
                ) {
                    Text(
                        text = actionLabel,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
