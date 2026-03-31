package com.prolife.finance.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.toColorInt
import com.prolife.finance.model.DrinkCollection
import com.prolife.finance.model.DrinkRecord
import com.prolife.finance.model.DrinkSettings
import com.prolife.finance.model.DrinkType
import com.prolife.finance.model.StatsGranularity
import com.prolife.finance.ui.designsystem.LedgerDesignSystem
import com.prolife.finance.ui.theme.LedgerBloomTokens
import java.text.DecimalFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CupsFormatter = DecimalFormat("0.#")
private val DayLabelFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd")

private enum class DrinkStatsFilter(val label: String) {
    ALL("\u5168\u90e8"),
    MILKTEA("\u5976\u8336"),
    COFFEE("\u5496\u5561")
}

private data class DrinkStatsBundle(
    val totalCups: Double,
    val totalCost: Double,
    val averageCostPerCup: Double,
    val activeDays: Int,
    val favoriteBrand: String,
    val favoriteSugar: String?,
    val weeklyUsageLabel: String,
    val monthlyUsageLabel: String,
    val weeklyProgress: Float,
    val monthlyProgress: Float,
    val costBuckets: List<ExpenseBucket>,
    val periodRecords: List<DrinkRecord>,
    val recentRecords: List<DrinkRecord>
)

@Composable
fun DrinkStatsInsightCard(
    state: AppUiState,
    onAction: (AppAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var filter by rememberSaveable { mutableStateOf(DrinkStatsFilter.ALL) }
    var showCalendarView by rememberSaveable(filter, state.statsGranularity, state.statsAnchorDate) { mutableStateOf(false) }
    val analytics = remember(state.milktea, state.coffee, state.statsGranularity, state.statsAnchorDate, filter) {
        buildDrinkAnalytics(
            milktea = state.milktea,
            coffee = state.coffee,
            filter = filter,
            granularity = state.statsGranularity,
            anchorDate = state.statsAnchorDate
        )
    }
    val preferredType = if (filter == DrinkStatsFilter.COFFEE) DrinkType.COFFEE else DrinkType.MILK_TEA

    androidx.compose.material3.ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.LocalCafe,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "\u5976\u8336 / \u5496\u5561\u7edf\u8ba1",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "\u805a\u7126\u5f53\u524d\u5468\u671f\u7684\u5173\u952e\u4fe1\u606f",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DrinkStatsFilter.entries.forEach { option ->
                    val selected = option == filter
                    val scale by animateFloatAsState(
                        targetValue = if (selected) 1.04f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "drinkFilterScale"
                    )
                    Box(modifier = Modifier.scale(scale)) {
                        FilterChip(
                            selected = selected,
                            onClick = { filter = option },
                            label = { Text(option.label) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DrinkActionButton(
                    modifier = Modifier.weight(1f),
                    label = "\u9650\u989d\u8bbe\u7f6e",
                    primary = false,
                    onClick = { onAction(AppAction.OpenDrinkSettings) },
                    icon = { Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                DrinkActionButton(
                    modifier = Modifier.weight(1f),
                    label = "\u65b0\u589e\u8bb0\u5f55",
                    primary = true,
                    onClick = { onAction(AppAction.OpenCreateDrink(preferredType)) },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            if (analytics.totalCups <= 0.0 && analytics.recentRecords.isEmpty()) {
                EmptyDrinkState(filter = filter)
            } else {
                DrinkMetricsCard(analytics.totalCups, analytics.totalCost, analytics.averageCostPerCup)
                DrinkProgressCard(
                    weeklyUsage = analytics.weeklyUsageLabel,
                    monthlyUsage = analytics.monthlyUsageLabel,
                    weeklyProgress = analytics.weeklyProgress,
                    monthlyProgress = analytics.monthlyProgress
                )
                if (analytics.costBuckets.isNotEmpty()) {
                    DrinkViewSectionHeader(
                        showCalendarView = showCalendarView,
                        onShowChart = { showCalendarView = false },
                        onShowCalendar = { showCalendarView = true }
                    )
                    if (showCalendarView) {
                        DrinkCalendarViewWithDetail(
                            buckets = analytics.costBuckets,
                            granularity = state.statsGranularity,
                            anchorDate = state.statsAnchorDate,
                            records = analytics.periodRecords,
                            onAction = onAction
                        )
                    } else {
                        DrinkCostChart(buckets = analytics.costBuckets)
                    }
                }
                DrinkStatsRow(
                    activeDays = analytics.activeDays,
                    favoriteBrand = analytics.favoriteBrand,
                    sugarLabel = if (filter == DrinkStatsFilter.COFFEE) "\u996e\u54c1\u504f\u597d" else "\u7cd6\u5ea6\u504f\u597d",
                    sugarValue = analytics.favoriteSugar ?: if (filter == DrinkStatsFilter.COFFEE) typeDisplayName(DrinkType.COFFEE) else "\u6682\u65e0"
                )
                if (analytics.recentRecords.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "\u6700\u8fd1\u8bb0\u5f55",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        analytics.recentRecords.forEach { record ->
                            DrinkRecordCard(
                                record = record,
                                onClick = { onAction(AppAction.OpenEditDrink(record.id, record.drinkType)) },
                                onEdit = { onAction(AppAction.OpenEditDrink(record.id, record.drinkType)) },
                                onDelete = { onAction(AppAction.DeleteDrinkRecord(record.id, record.drinkType)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrinkActionButton(
    modifier: Modifier,
    label: String,
    primary: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "drinkActionScale"
    )
    Box(modifier = modifier.scale(scale)) {
        if (primary) {
            FilledTonalButton(onClick = onClick, interactionSource = interactionSource, modifier = Modifier.fillMaxWidth()) {
                icon()
                Spacer(modifier = Modifier.width(4.dp))
                Text(label)
            }
        } else {
            OutlinedButton(onClick = onClick, interactionSource = interactionSource, modifier = Modifier.fillMaxWidth()) {
                icon()
                Spacer(modifier = Modifier.width(4.dp))
                Text(label)
            }
        }
    }
}
@Composable
private fun EmptyDrinkState(filter: DrinkStatsFilter) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = LedgerDesignSystem.Shapes.Large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = filter.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                text = "\u5f53\u524d\u5468\u671f\u8fd8\u6ca1\u6709\u996e\u54c1\u8bb0\u5f55",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "\u70b9\u51fb\u4e0a\u65b9\u201c\u65b0\u589e\u8bb0\u5f55\u201d\u5f00\u59cb\u7edf\u8ba1",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun DrinkMetricsCard(totalCups: Double, totalCost: Double, averageCost: Double) {
    val animatedCups by animateFloatAsState(targetValue = totalCups.toFloat(), animationSpec = tween(600), label = "cups")
    val animatedCost by animateFloatAsState(targetValue = totalCost.toFloat(), animationSpec = tween(800), label = "cost")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = LedgerDesignSystem.Shapes.Large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetricItem(
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Rounded.LocalDrink, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                value = "${formatCupCount(animatedCups.toDouble())} \u676f",
                label = "\u603b\u676f\u6570",
                color = MaterialTheme.colorScheme.primary
            )
            MetricDivider()
            MetricItem(
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Rounded.AttachMoney, contentDescription = null, tint = LedgerBloomTokens.palette.expense) },
                value = formatCurrency(animatedCost.toDouble()),
                label = "\u603b\u82b1\u8d39",
                color = LedgerBloomTokens.palette.expense
            )
            MetricDivider()
            MetricItem(
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Rounded.Insights, contentDescription = null, tint = LedgerBloomTokens.palette.chartAccent) },
                value = formatCurrency(averageCost),
                label = "\u5747\u4ef7",
                color = LedgerBloomTokens.palette.chartAccent
            )
        }
    }
}

@Composable
private fun MetricItem(
    modifier: Modifier,
    icon: @Composable () -> Unit,
    value: String,
    label: String,
    color: Color
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MetricDivider() {
    Box(
        modifier = Modifier
            .height(52.dp)
            .width(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    )
}

@Composable
private fun DrinkProgressCard(weeklyUsage: String, monthlyUsage: String, weeklyProgress: Float, monthlyProgress: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ProgressItem(weeklyUsage, weeklyProgress.coerceIn(0f, 1f), if (weeklyProgress > 0.8f) LedgerBloomTokens.palette.expense else LedgerBloomTokens.palette.chartAccent)
        ProgressItem(monthlyUsage, monthlyProgress.coerceIn(0f, 1f), if (monthlyProgress > 0.8f) LedgerBloomTokens.palette.expense else MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ProgressItem(label: String, progress: Float, color: Color) {
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(800), label = "drinkProgress")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun DrinkViewSectionHeader(
    showCalendarView: Boolean,
    onShowChart: () -> Unit,
    onShowCalendar: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "\u996e\u54c1\u82b1\u8d39\u89c6\u56fe",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !showCalendarView,
                onClick = onShowChart,
                label = { Text("\u56fe\u8868") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.ShowChart,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
            FilterChip(
                selected = showCalendarView,
                onClick = onShowCalendar,
                label = { Text("\u65e5\u5386") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun DrinkCalendarViewWithDetail(
    buckets: List<ExpenseBucket>,
    granularity: StatsGranularity,
    anchorDate: LocalDate,
    records: List<DrinkRecord>,
    onAction: (AppAction) -> Unit
) {
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDetailDialog by remember { mutableStateOf(false) }

    val dateRecords = remember(selectedDate, records) {
        selectedDate?.let { date ->
            records
                .filter { it.date == date }
                .sortedWith(compareByDescending<DrinkRecord> { it.date }.thenByDescending { it.id })
        } ?: emptyList()
    }

    when (granularity) {
        StatsGranularity.WEEK -> {
            WeekCalendarViewWithDetail(
                buckets = buckets,
                anchorDate = anchorDate,
                onDateClick = { date ->
                    selectedDate = date
                    showDetailDialog = true
                }
            )
        }
        StatsGranularity.MONTH -> {
            MonthCalendarViewWithDetail(
                buckets = buckets,
                anchorDate = anchorDate,
                onDateClick = { date ->
                    selectedDate = date
                    showDetailDialog = true
                }
            )
        }
        StatsGranularity.YEAR -> {
            YearCalendarViewPublic(
                buckets = buckets,
                anchorDate = anchorDate
            )
        }
    }

    if (showDetailDialog && selectedDate != null) {
        LaunchedEffect(dateRecords.isEmpty()) {
            if (dateRecords.isEmpty()) showDetailDialog = false
        }
        DrinkDayDetailDialog(
            date = selectedDate!!,
            records = dateRecords,
            onAction = onAction,
            onDismiss = { showDetailDialog = false }
        )
    }
}

@Composable
private fun DrinkDayDetailDialog(
    date: LocalDate,
    records: List<DrinkRecord>,
    onAction: (AppAction) -> Unit,
    onDismiss: () -> Unit
) {
    val totalCups = records.sumOf { it.cups }
    val totalCost = records.sumOf { it.cost }
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy\u5e74M\u6708d\u65e5 EEEE")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = date.format(dateFormatter),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "\u5173\u95ed"
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${formatCupCount(totalCups)} \u676f",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "\u676f\u6570",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = LedgerBloomTokens.palette.expense.copy(alpha = 0.1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = formatCurrency(totalCost),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = LedgerBloomTokens.palette.expense
                            )
                            Text(
                                text = "\u82b1\u8d39",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (records.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "\u5f53\u5929\u6ca1\u6709\u996e\u54c1\u8bb0\u5f55",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                text = "\u5f53\u5929\u8bb0\u5f55 (${records.size}\u7b14)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        items(
                            items = records,
                            key = { it.id }
                        ) { record ->
                            DrinkRecordCard(
                                record = record,
                                onClick = { onAction(AppAction.OpenEditDrink(record.id, record.drinkType)) },
                                onEdit = { onAction(AppAction.OpenEditDrink(record.id, record.drinkType)) },
                                onDelete = { onAction(AppAction.DeleteDrinkRecord(record.id, record.drinkType)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrinkStatsRow(activeDays: Int, favoriteBrand: String, sugarLabel: String, sugarValue: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        DrinkStatsInfoCard(modifier = Modifier.weight(1f), label = "\u6d3b\u8dc3\u5929\u6570", value = "${activeDays}\u5929", accent = MaterialTheme.colorScheme.primary)
        DrinkStatsInfoCard(modifier = Modifier.weight(1f), label = "\u5e38\u70b9\u54c1\u724c", value = favoriteBrand.ifBlank { "\u6682\u65e0" }, accent = MaterialTheme.colorScheme.secondary)
        DrinkStatsInfoCard(modifier = Modifier.weight(1f), label = sugarLabel, value = sugarValue.ifBlank { "\u6682\u65e0" }, accent = MaterialTheme.colorScheme.tertiary)
    }
}

@Composable
private fun DrinkStatsInfoCard(modifier: Modifier, label: String, value: String, accent: Color) {
    Surface(modifier = modifier, shape = RoundedCornerShape(18.dp), color = accent.copy(alpha = 0.12f)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = accent, maxLines = 2)
        }
    }
}

@Composable
private fun DrinkRecordCard(
    record: DrinkRecord,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val accent = Color(record.drinkType.accentHex.toColorInt())
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "drinkCardScale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        color = accent.copy(alpha = 0.08f),
        shape = LedgerDesignSystem.Shapes.Large,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = LedgerDesignSystem.Shapes.Medium, color = accent.copy(alpha = 0.15f), modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(text = if (record.drinkType == DrinkType.COFFEE) "C" else "T", style = MaterialTheme.typography.titleMedium) }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = record.brand.ifBlank { typeDisplayName(record.drinkType) }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                    Surface(shape = RoundedCornerShape(4.dp), color = accent.copy(alpha = 0.12f)) {
                        Text(text = typeDisplayName(record.drinkType), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = accent)
                    }
                }
                Text(
                    text = buildString {
                        append(record.date)
                        append(" | ")
                        append(formatCupCount(record.cups))
                        append(" \u676f")
                        if (record.sugar.isNotBlank() && record.drinkType == DrinkType.MILK_TEA) {
                            append(" | ")
                            append(record.sugar)
                        }
                        if (record.notes.isNotBlank()) {
                            append(" | ")
                            append(record.notes)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formatCurrency(record.cost),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
                if (onEdit != null || onDelete != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        onEdit?.let { editAction ->
                            IconButton(onClick = editAction, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "\u7f16\u8f91",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        onDelete?.let { deleteAction ->
                            IconButton(onClick = deleteAction, modifier = Modifier.size(32.dp)) {
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

@Composable
private fun DrinkCostChart(buckets: List<ExpenseBucket>) {
    val animProgress = remember { Animatable(0f) }
    val maxAmount = remember(buckets) { buckets.maxOfOrNull { it.amount }?.takeIf { it > 0.0 } ?: 1.0 }
    val averageAmount = remember(buckets) { if (buckets.isEmpty()) 0.0 else buckets.sumOf { it.amount } / buckets.size }
    val averageRatio = if (averageAmount <= 0.0) 0f else (averageAmount / maxAmount).toFloat().coerceIn(0f, 1f)
    val averageLabelTop = averageLabelTop(averageRatio)
    val peakIndex = remember(buckets) { if (buckets.isEmpty()) -1 else buckets.indices.maxByOrNull { buckets[it].amount } ?: -1 }
    var hoverIndex by remember { mutableStateOf<Int?>(null) }
    var lockedIndex by remember { mutableStateOf<Int?>(null) }
    var tooltipSize by remember { mutableStateOf(IntSize.Zero) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(buckets) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, tween(700))
    }
    val chartAccent = LedgerBloomTokens.palette.chartAccent
    val selectedPointColor = MaterialTheme.colorScheme.primary
    val peakBadgeColor = MaterialTheme.colorScheme.primaryContainer
    val peakBadgeTextColor = MaterialTheme.colorScheme.onPrimaryContainer
    val tooltipColor = MaterialTheme.colorScheme.primary
    val tooltipTextColor = MaterialTheme.colorScheme.onPrimary
    val tooltipSecondaryTextColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(156.dp)
            .semantics { contentDescription = "drink_cost_chart" }
    ) {
        Box(modifier = Modifier.width(44.dp).height(156.dp)) {
            Text(text = chartAxisLabel(maxAmount), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.TopStart).padding(top = 8.dp))
            if (averageAmount > 0) {
                Text(text = chartAxisLabel(averageAmount), style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF8C00), modifier = Modifier.align(Alignment.TopStart).padding(top = averageLabelTop.dp))
            }
            Text(text = "0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.BottomStart))
        }
        BoxWithConstraints(modifier = Modifier.weight(1f).height(156.dp)) {
            val density = LocalDensity.current
            val chartWidthPx = with(density) { maxWidth.toPx() }
            val chartHeightPx = with(density) { maxHeight.toPx() }
            val topPaddingPx = with(density) { 8.dp.toPx() }
            val bottomPaddingPx = 0f
            val tooltipMarginPx = with(density) { 10.dp.toPx() }
            // Cache dp values used inside Canvas draw
            val selectedDotRadiusPx = with(density) { 5.dp.toPx() }
            val normalDotRadiusPx = with(density) { 3.5.dp.toPx() }
            // Reuse Path objects to avoid allocation on every draw frame
            val fillPath = remember { androidx.compose.ui.graphics.Path() }
            val linePath = remember { androidx.compose.ui.graphics.Path() }
            val displayIndex = lockedIndex ?: hoverIndex
            val selectedPoint = displayIndex?.takeIf { it in buckets.indices }?.let { index ->
                lineChartPoint(index, buckets.size, buckets[index].amount, maxAmount, chartWidthPx, chartHeightPx, topPaddingPx, bottomPaddingPx, animProgress.value)
            }
            val tooltipOffset = if (selectedPoint != null && tooltipSize != IntSize.Zero) {
                lineChartTooltipOffset(selectedPoint, tooltipSize.width, tooltipSize.height, chartWidthPx, chartHeightPx, tooltipMarginPx)
            } else {
                androidx.compose.ui.unit.IntOffset.Zero
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(buckets) {
                        detectTapGestures(
                            onTap = { offset -> if (buckets.isNotEmpty()) hoverIndex = chartIndexFromTap(offset.x, size.width.toFloat(), buckets.size) },
                            onLongPress = { offset ->
                                if (buckets.isNotEmpty()) {
                                    val index = chartIndexFromTap(offset.x, size.width.toFloat(), buckets.size)
                                    lockedIndex = if (lockedIndex == index) null else index
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        )
                    }
            ) {
                val width = size.width
                val height = size.height
                val chartHeight = height - topPaddingPx - bottomPaddingPx
                drawLine(color = Color.LightGray.copy(alpha = 0.2f), start = Offset(0f, topPaddingPx), end = Offset(width, topPaddingPx), strokeWidth = 1f)
                drawLine(color = Color.LightGray.copy(alpha = 0.2f), start = Offset(0f, height), end = Offset(width, height), strokeWidth = 1f)
                if (averageAmount > 0) {
                    val avgY = topPaddingPx + chartHeight * (1 - averageRatio)
                    drawLine(color = Color(0xFFFF8C00).copy(alpha = 0.7f), start = Offset(0f, avgY), end = Offset(width, avgY), strokeWidth = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f))
                }
                val points = buckets.mapIndexed { index, bucket ->
                    lineChartPoint(index, buckets.size, bucket.amount, maxAmount, width, height, topPaddingPx, bottomPaddingPx, animProgress.value)
                }
                if (points.size > 1) {
                    fillPath.rewind()
                    fillPath.moveTo(points.first().x, height)
                    points.forEach { fillPath.lineTo(it.x, it.y) }
                    fillPath.lineTo(points.last().x, height)
                    fillPath.close()
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            listOf(chartAccent.copy(alpha = 0.22f), Color.Transparent),
                            startY = topPaddingPx,
                            endY = height
                        )
                    )
                    linePath.rewind()
                    linePath.moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { linePath.lineTo(it.x, it.y) }
                    drawPath(path = linePath, color = chartAccent, style = Stroke(width = 3f))
                }
                points.forEachIndexed { index, point ->
                    val isSelected = index == displayIndex
                    val isPeak = index == peakIndex && buckets[index].amount > 0
                    val radius = if (isSelected || isPeak) selectedDotRadiusPx else normalDotRadiusPx
                    val pointColor = if (isSelected || isPeak) selectedPointColor else chartAccent
                    if (isSelected || isPeak) {
                        drawCircle(color = pointColor.copy(alpha = 0.2f), radius = radius * 2.2f, center = point)
                    }
                    drawCircle(color = Color.White, radius = radius, center = point)
                    drawCircle(color = pointColor, radius = radius * 0.62f, center = point)
                }
            }

            val peakBucket = remember(buckets) { buckets.maxByOrNull { it.amount }?.takeIf { it.amount > 0 } }
            peakBucket?.let { peak ->
                Surface(shape = RoundedCornerShape(8.dp), color = peakBadgeColor, modifier = Modifier.align(Alignment.TopStart)) {
                    Text(
                        text = formatCurrency(peak.amount),
                        style = MaterialTheme.typography.labelMedium,
                        color = peakBadgeTextColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (displayIndex != null && displayIndex in buckets.indices) {
                val bucket = buckets[displayIndex]
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = tooltipColor,
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
                            color = tooltipSecondaryTextColor
                        )
                        Text(
                            text = formatCurrency(bucket.amount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = tooltipTextColor
                        )
                    }
                }
            }
        }
    }
}

private fun buildDrinkAnalytics(
    milktea: DrinkCollection,
    coffee: DrinkCollection,
    filter: DrinkStatsFilter,
    granularity: StatsGranularity,
    anchorDate: LocalDate
): DrinkStatsBundle {
    val allRecords = when (filter) {
        DrinkStatsFilter.ALL -> milktea.records + coffee.records
        DrinkStatsFilter.MILKTEA -> milktea.records
        DrinkStatsFilter.COFFEE -> coffee.records
    }
    val periodRecords = filterDrinkRecordsForPeriod(allRecords, granularity, anchorDate)
    val settings = combinedDrinkSettings(filter, milktea.settings, coffee.settings)
    val totalCups = periodRecords.sumOf { it.cups }
    val totalCost = periodRecords.sumOf { it.cost }
    val favoriteBrand = periodRecords
        .groupBy { it.brand.ifBlank { typeDisplayName(it.drinkType) } }
        .maxByOrNull { (_, records) -> records.sumOf { it.cups } }
        ?.key
        ?: "\u6682\u65e0"
    val favoriteSugar = periodRecords
        .filter { it.drinkType == DrinkType.MILK_TEA && it.sugar.isNotBlank() }
        .groupBy { it.sugar }
        .maxByOrNull { (_, records) -> records.size }
        ?.key
    val today = LocalDate.now()
    val weeklyCount = periodDrinkCount(allRecords, today, PeriodScope.WEEK)
    val monthlyCount = periodDrinkCount(allRecords, today, PeriodScope.MONTH)
    val weeklyProgress = if (settings.weeklyLimit <= 0) 0f else (weeklyCount / settings.weeklyLimit).toFloat()
    val monthlyProgress = if (settings.monthlyLimit <= 0) 0f else (monthlyCount / settings.monthlyLimit).toFloat()
    return DrinkStatsBundle(
        totalCups = totalCups,
        totalCost = totalCost,
        averageCostPerCup = if (totalCups == 0.0) 0.0 else totalCost / totalCups,
        activeDays = periodRecords.map { it.date }.distinct().size,
        favoriteBrand = favoriteBrand,
        favoriteSugar = favoriteSugar,
        weeklyUsageLabel = "\u672c\u5468\u9650\u989d ${formatCupCount(weeklyCount)} / ${settings.weeklyLimit} \u676f",
        monthlyUsageLabel = "\u672c\u6708\u9650\u989d ${formatCupCount(monthlyCount)} / ${settings.monthlyLimit} \u676f",
        weeklyProgress = weeklyProgress,
        monthlyProgress = monthlyProgress,
        costBuckets = buildDrinkCostBuckets(periodRecords, granularity, anchorDate),
        periodRecords = periodRecords,
        recentRecords = periodRecords.sortedWith(compareByDescending<DrinkRecord> { it.date }.thenByDescending { it.id }).take(3)
    )
}

private fun combinedDrinkSettings(filter: DrinkStatsFilter, milktea: DrinkSettings, coffee: DrinkSettings): DrinkSettings {
    return when (filter) {
        DrinkStatsFilter.ALL -> DrinkSettings(weeklyLimit = milktea.weeklyLimit + coffee.weeklyLimit, monthlyLimit = milktea.monthlyLimit + coffee.monthlyLimit)
        DrinkStatsFilter.MILKTEA -> milktea
        DrinkStatsFilter.COFFEE -> coffee
    }
}

private fun buildDrinkCostBuckets(records: List<DrinkRecord>, granularity: StatsGranularity, anchorDate: LocalDate): List<ExpenseBucket> {
    return when (granularity) {
        StatsGranularity.WEEK -> {
            val start = startOfWeek(anchorDate)
            (0..6).map { offset ->
                val day = start.plusDays(offset.toLong())
                ExpenseBucket(label = dayOfWeekLabel(day.dayOfWeek), amount = records.filter { it.date == day }.sumOf { it.cost }, dateLabel = day.format(DayLabelFormatter))
            }
        }
        StatsGranularity.MONTH -> {
            val yearMonth = YearMonth.from(anchorDate)
            (1..yearMonth.lengthOfMonth()).map { dayOfMonth ->
                val date = yearMonth.atDay(dayOfMonth)
                ExpenseBucket(label = dayOfMonth.toString(), amount = records.filter { it.date == date }.sumOf { it.cost }, dateLabel = date.format(DayLabelFormatter))
            }
        }
        StatsGranularity.YEAR -> {
            (1..12).map { monthValue ->
                ExpenseBucket(label = monthValue.toString(), amount = records.filter { it.date.year == anchorDate.year && it.date.monthValue == monthValue }.sumOf { it.cost }, dateLabel = "${anchorDate.year}-${monthValue}")
            }
        }
    }
}

private fun filterDrinkRecordsForPeriod(records: List<DrinkRecord>, granularity: StatsGranularity, anchorDate: LocalDate): List<DrinkRecord> {
    return when (granularity) {
        StatsGranularity.WEEK -> {
            val start = startOfWeek(anchorDate)
            val end = start.plusDays(6)
            records.filter { !it.date.isBefore(start) && !it.date.isAfter(end) }
        }
        StatsGranularity.MONTH -> {
            val target = YearMonth.from(anchorDate)
            records.filter { YearMonth.from(it.date) == target }
        }
        StatsGranularity.YEAR -> records.filter { it.date.year == anchorDate.year }
    }
}

private enum class PeriodScope { WEEK, MONTH }

private fun periodDrinkCount(records: List<DrinkRecord>, anchorDate: LocalDate, scope: PeriodScope): Double {
    val filtered = when (scope) {
        PeriodScope.WEEK -> {
            val start = startOfWeek(anchorDate)
            val end = start.plusDays(6)
            records.filter { !it.date.isBefore(start) && !it.date.isAfter(end) }
        }
        PeriodScope.MONTH -> {
            val month = YearMonth.from(anchorDate)
            records.filter { YearMonth.from(it.date) == month }
        }
    }
    return filtered.sumOf { it.cups }
}

private fun startOfWeek(date: LocalDate): LocalDate = date.minusDays((date.dayOfWeek.value - 1).toLong())

private fun dayOfWeekLabel(dayOfWeek: DayOfWeek): String = when (dayOfWeek) {
    DayOfWeek.MONDAY -> "\u4e00"
    DayOfWeek.TUESDAY -> "\u4e8c"
    DayOfWeek.WEDNESDAY -> "\u4e09"
    DayOfWeek.THURSDAY -> "\u56db"
    DayOfWeek.FRIDAY -> "\u4e94"
    DayOfWeek.SATURDAY -> "\u516d"
    DayOfWeek.SUNDAY -> "\u65e5"
}

private fun typeDisplayName(type: DrinkType): String = when (type) {
    DrinkType.MILK_TEA -> "\u5976\u8336"
    DrinkType.COFFEE -> "\u5496\u5561"
}

private fun formatCupCount(value: Double): String = CupsFormatter.format(value)

private fun averageLabelTop(avgRatio: Float): Float {
    val containerHeight = 156f
    val topPadding = 8f
    val labelHeight = 16f
    val labelGap = 4f
    val chartHeight = containerHeight - topPadding
    val lineCenter = topPadding + chartHeight * (1f - avgRatio)
    val topLimit = topPadding + labelGap
    val bottomZeroLabelTop = containerHeight - labelHeight
    val bottomLimit = bottomZeroLabelTop - labelHeight - labelGap
    return (lineCenter - labelHeight / 2f).coerceIn(topLimit, bottomLimit)
}

private fun chartAxisLabel(amount: Double): String {
    return when {
        amount >= 1000 -> String.format(Locale.getDefault(), "%.0fk", amount / 1000)
        else -> String.format(Locale.getDefault(), "%.0f", amount)
    }
}
