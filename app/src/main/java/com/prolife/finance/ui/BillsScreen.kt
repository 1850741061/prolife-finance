package com.prolife.finance.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prolife.finance.model.TransactionRecord
import com.prolife.finance.model.TransactionType
import com.prolife.finance.ui.animations.*
import com.prolife.finance.ui.designsystem.AnimatedEntrance
import com.prolife.finance.ui.theme.LedgerBloomTokens
import java.time.Year
import java.time.YearMonth

private enum class BillShortcutMode(val label: String) {
    MONTH("\u6309\u6708\u67e5\u770b"),
    YEAR("\u6309\u5e74\u67e5\u770b")
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun BillsScreen(
    transactions: List<TransactionRecord>,
    onAction: (AppAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val groupedMonths = remember(transactions) {
        transactions.groupBy { YearMonth.from(it.date) }.toSortedMap(compareByDescending { it })
    }
    val groupedYears = remember(transactions) {
        transactions.groupBy { it.date.year }.toSortedMap(compareByDescending { it })
    }
    val allSummary = remember(transactions) { summarize(transactions) }
    val yearOptions = remember(groupedYears) { groupedYears.keys.toList() }
    var shortcutMode by remember { mutableStateOf(BillShortcutMode.MONTH) }
    var selectedYear by remember(yearOptions) {
        mutableStateOf(yearOptions.firstOrNull() ?: Year.now().value)
    }
    val activeYear = selectedYear.takeIf { yearOptions.contains(it) } ?: yearOptions.firstOrNull() ?: Year.now().value
    val filteredMonthEntries = remember(groupedMonths, activeYear) {
        groupedMonths.entries
            .filter { it.key.year == activeYear }
            .map { entry ->
                Triple(entry.key, entry.value, summarize(entry.value))
            }
    }
    val recentMonthEntries = remember(groupedMonths) {
        groupedMonths.entries.take(3).map { entry -> Triple(entry.key, entry.value, summarize(entry.value)) }
    }
    val yearAnalyticsList = remember(groupedYears) {
        groupedYears.entries.toList().map { (year, yearTransactions) ->
            Triple(year, yearTransactions, summarize(yearTransactions))
        }
    }

    var expandedMonth by remember { mutableStateOf<YearMonth?>(null) }
    var expandedYear by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            SectionTitle(
                title = "\u8d26\u5355\u6982\u89c8",
                subtitle = "\u6309\u6708\u6216\u6309\u5e74\u67e5\u770b\u4f60\u7684\u6536\u652f\u8bb0\u5f55\u4e0e\u8d8b\u52bf"
            )
        }
        if (transactions.isNotEmpty()) {
            item {
                SummaryHeroCard(
                    title = "\u603b\u89c8",
                    subtitle = "${transactions.size} \u7b14\u8bb0\u5f55 \u00b7 ${groupedMonths.size} \u4e2a\u8d26\u5355\u6708",
                    summary = allSummary
                )
            }
            item {
                ElevatedCard {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                            Text(
                                text = "\u67e5\u770b\u65b9\u5f0f",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(BillShortcutMode.entries) { mode ->
                                    val selected = shortcutMode == mode
                                    val scale by animateFloatAsState(
                                        targetValue = if (selected) 1.05f else 1f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        ),
                                        label = "chipScale"
                                    )
                                    Box(modifier = Modifier.scale(scale)) {
                                        FilterChip(
                                            selected = selected,
                                            onClick = {
                                                shortcutMode = mode
                                                expandedMonth = null
                                                expandedYear = null
                                            },
                                            label = { Text(mode.label) }
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "\u5df2\u8986\u76d6 ${groupedYears.size} \u4e2a\u5e74\u4efd\uff0c${groupedMonths.size} \u4e2a\u8bb0\u8d26\u6708\u4efd\u3002",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            if (recentMonthEntries.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "\u6700\u8fd1\u8d26\u5355",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                recentMonthEntries.forEachIndexed { _, (month, monthTransactions, summary) ->
                    val isExpanded = expandedMonth == month
                    item(key = "recent_$month") {
                        Column {
                            ExpandableBillCard(
                                title = month.format(MonthFormatter),
                                subtitle = "${monthTransactions.size} \u7b14 \u00b7 \u6536\u5165 ${formatCurrency(summary.income)} \u00b7 \u652f\u51fa ${formatCurrency(summary.expense)}",
                                accent = colorFromHex(monthTransactions.firstOrNull()?.colorHex ?: "#6B7280"),
                                trailing = formatCurrency(summary.balance),
                                isExpanded = isExpanded,
                                onClick = {
                                    expandedMonth = if (isExpanded) null else month
                                    expandedYear = null
                                }
                            )
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically(tween(250)) + fadeIn(tween(200)),
                                exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
                            ) {
                                BillStatsPanel(
                                    transactions = monthTransactions,
                                    title = month.format(MonthFormatter)
                                )
                            }
                        }
                    }
                }
            }
            if (shortcutMode == BillShortcutMode.MONTH && yearOptions.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "\u9009\u62e9\u5e74\u4efd",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(yearOptions) { year ->
                                    val selected = activeYear == year
                                    val scale by animateFloatAsState(
                                        targetValue = if (selected) 1.05f else 1f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        ),
                                        label = "yearChipScale"
                                    )
                                    Box(modifier = Modifier.scale(scale)) {
                                        FilterChip(
                                            selected = selected,
                                            onClick = {
                                                selectedYear = year
                                                expandedMonth = null
                                                expandedYear = null
                                            },
                                            label = { Text("${year}\u5e74") }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            if (shortcutMode == BillShortcutMode.YEAR) {
                item {
                    Text(
                        text = "\u5e74\u5ea6\u603b\u89c8",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                yearAnalyticsList.forEachIndexed { _, (year, yearTransactions, summary) ->
                    val isExpanded = expandedYear == year
                    item(key = "year_$year") {
                        Column {
                            ExpandableBillCard(
                                title = "${year}\u5e74\u603b\u89c8",
                                subtitle = "${yearTransactions.size} \u7b14 \u00b7 \u6536\u5165 ${formatCurrency(summary.income)} \u00b7 \u652f\u51fa ${formatCurrency(summary.expense)}",
                                accent = if (summary.balance >= 0) {
                                    LedgerBloomTokens.palette.income
                                } else {
                                    LedgerBloomTokens.palette.expense
                                },
                                trailing = formatCurrency(summary.balance),
                                isExpanded = isExpanded,
                                onClick = {
                                    expandedYear = if (isExpanded) null else year
                                    expandedMonth = null
                                }
                            )
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically(tween(250)) + fadeIn(tween(200)),
                                exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
                            ) {
                                BillStatsPanel(
                                    transactions = yearTransactions,
                                    title = "${year}\u5e74\u7edf\u8ba1",
                                    isYearOverview = true
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    Text(
                        text = "${activeYear}\u5e74\u8d26\u5355",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (filteredMonthEntries.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "\u8fd9\u4e00\u5e74\u8fd8\u6ca1\u6709\u8d26\u5355",
                            subtitle = "\u5207\u6362\u5230\u5176\u4ed6\u5e74\u4efd\uff0c\u6216\u5148\u65b0\u589e\u51e0\u7b14\u8bb0\u5f55\u3002"
                        )
                    }
                } else {
                    filteredMonthEntries.forEachIndexed { _, (month, monthTransactions, summary) ->
                        val isExpanded = expandedMonth == month
                        item(key = "filtered_$month") {
                            Column {
                                ExpandableBillCard(
                                    title = month.format(MonthFormatter),
                                    subtitle = "${monthTransactions.size} \u7b14 \u00b7 \u6536\u5165 ${formatCurrency(summary.income)} \u00b7 \u652f\u51fa ${formatCurrency(summary.expense)}",
                                    accent = colorFromHex(monthTransactions.firstOrNull()?.colorHex ?: "#6B7280"),
                                    trailing = formatCurrency(summary.balance),
                                    isExpanded = isExpanded,
                                    onClick = {
                                        expandedMonth = if (isExpanded) null else month
                                        expandedYear = null
                                    }
                                )
                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = expandVertically(tween(250)) + fadeIn(tween(200)),
                                    exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
                                ) {
                                    BillStatsPanel(
                                        transactions = monthTransactions,
                                        title = month.format(MonthFormatter)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            item {
                EmptyStateCard(
                    title = "\u8fd8\u6ca1\u6709\u8d26\u5355\u8bb0\u5f55",
                    subtitle = "\u5148\u8bb0\u4e00\u7b14\uff0c\u8d26\u5355\u9875\u4f1a\u81ea\u52a8\u6309\u6708\u548c\u6309\u5e74\u805a\u5408\u3002"
                )
            }
        }
    }
}

@Composable
private fun ExpandableBillCard(
    title: String,
    subtitle: String,
    accent: Color,
    trailing: String,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(300),
        label = "arrowRotation"
    )

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = accent.copy(alpha = if (isExpanded) 0.2f else 0.12f)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(accent)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isExpanded) accent else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        trailing,
                        fontWeight = FontWeight.ExtraBold,
                        color = accent,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "\u6536\u8d77" else "\u5c55\u5f00",
                    modifier = Modifier.rotate(rotation),
                    tint = if (isExpanded) accent else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BillStatsPanel(
    transactions: List<TransactionRecord>,
    title: String,
    isYearOverview: Boolean = false
) {
    val analytics = remember(transactions, isYearOverview) {
        buildBillAnalytics(transactions, isYearOverview)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 闂佸搫绉村ú顓€?
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "${transactions.size} \u7b14",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // 缂傚倷鐒﹂幐椋庣礊閹达箑纭€闁挎稑瀚。?
            SummaryMiniCard(
                balance = analytics.summary.balance,
                income = analytics.summary.income,
                expense = analytics.summary.expense
            )
            
            if (analytics.trendBuckets.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "\u652f\u51fa\u8d8b\u52bf",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    SimpleTrendChart(buckets = analytics.trendBuckets)
                }
            }
            
            if (analytics.categorySlices.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "\u652f\u51fa\u5360\u6bd4 \u00b7 \u524d\u4e09\u5408\u8ba1 ${formatPercent(analytics.topThreeShare)}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    SimpleDonutChart(slices = analytics.categorySlices)
                }
            }
            
            // 缂備緡鍋夊畷闈浢烘导鏉戠闁圭儤娲﹂弨?
            if (analytics.categoryRanking.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "\u5206\u7c7b\u6392\u884c",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    SimpleRankingBars(
                        entries = analytics.categoryRanking.take(5),
                        totalAmount = analytics.summary.expense
                    )
                }
            }
            
            // 闂佸搫瀚ù椋庡垝韫囨稑绠抽柟鐑樻处閺€?
            if (analytics.topExpenseTransactions.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "\u652f\u51fa\u660e\u7ec6\u6392\u884c",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    analytics.topExpenseTransactions.take(3).forEachIndexed { index, record ->
                        SimpleRankItem(
                            rank = index + 1,
                            title = record.category,
                            subtitle = record.date.toString(),
                            amount = formatCurrency(record.amount),
                            color = colorFromHex(record.colorHex)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryMiniCard(
    balance: Double,
    income: Double,
    expense: Double
) {
    val balanceColor = if (balance >= 0) LedgerBloomTokens.palette.income else LedgerBloomTokens.palette.expense
    
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = formatCurrency(balance),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = balanceColor
            )
            Text(
                text = "\u7ed3\u4f59",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatCurrency(income),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LedgerBloomTokens.palette.income
                    )
                    Text(
                        text = "\u6536\u5165",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatCurrency(expense),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LedgerBloomTokens.palette.expense
                    )
                    Text(
                        text = "\u652f\u51fa",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SimpleTrendChart(buckets: List<ExpenseBucket>) {
    if (buckets.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "\u6682\u65e0\u6570\u636e",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val maxAmount = remember(buckets) { buckets.maxOfOrNull { it.amount }?.takeIf { it > 0 } ?: 1.0 }
    val accent = LedgerBloomTokens.palette.chartAccent
    val peakIndex = remember(buckets) { buckets.indices.maxByOrNull { buckets[it].amount } ?: -1 }
    val peakBucket = remember(buckets, peakIndex) { buckets.getOrNull(peakIndex)?.takeIf { it.amount > 0 } }
    var selectedIndex by remember(buckets) { mutableStateOf<Int?>(null) }
    val selectedBucket = selectedIndex?.let { buckets.getOrNull(it) }
    val labelIndexes = remember(buckets) { billTrendLabelIndexes(buckets.size) }
    val barAreaHeightDp = if (buckets.size > 12) 52.dp else 60.dp
    val barWidthDp = if (buckets.size > 12) 5.dp else 8.dp
    val spacingDp = if (buckets.size > 12) 2.dp else 4.dp
    val cornerRadiusDp = if (buckets.size > 12) 2.dp else 4.dp
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    // Cache dp conversions — avoids dp.toPx() on every draw frame
    val density = LocalDensity.current
    val barWidthPx = with(density) { barWidthDp.toPx() }
    val cornerRadiusPx = with(density) { cornerRadiusDp.toPx() }
    val topPaddingPx = with(density) { 14.dp.toPx() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        peakBucket?.let { bucket ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "\u5cf0\u503c\u652f\u51fa",
                    style = MaterialTheme.typography.labelMedium,
                    color = surfaceVariantColor
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = accent.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "${bucket.label} \u00b7 ${formatCurrency(bucket.amount)}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = accent
                    )
                }
            }
        }

        selectedBucket?.let { bucket ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "\u5df2\u9009 ${bucket.label} \u00b7 ${formatCurrency(bucket.amount)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = primaryColor
                )
            }
        }

        // Single Canvas for all bars — avoids N composable nodes
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(barAreaHeightDp + 14.dp)
                .pointerInput(buckets) {
                    detectTapGestures { offset ->
                        if (buckets.isEmpty()) return@detectTapGestures
                        val itemWidth = size.width / buckets.size
                        val index = (offset.x / itemWidth).toInt().coerceIn(0, buckets.size - 1)
                        selectedIndex = if (selectedIndex == index) null else index
                    }
                }
        ) {
            val chartHeightPx = size.height - topPaddingPx
            val slotWidth = size.width / buckets.size

            buckets.forEachIndexed { index, bucket ->
                val heightRatio = if (maxAmount > 0) {
                    (bucket.amount / maxAmount).toFloat().coerceIn(0f, 1f)
                } else {
                    0f
                }
                val barHeight = chartHeightPx * heightRatio
                val x = slotWidth * index + (slotWidth - barWidthPx) / 2f
                val y = size.height - barHeight

                val color = when {
                    index == selectedIndex -> primaryColor
                    index == peakIndex -> accent
                    else -> accent.copy(alpha = if (bucket.amount > 0) 0.5f else 0.16f)
                }

                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(barWidthPx, barHeight),
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacingDp)
        ) {
            buckets.forEachIndexed { index, bucket ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (labelIndexes.contains(index)) {
                        Text(
                            text = bucket.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
@Composable
private fun SimpleDonutChart(slices: List<CategorySlice>) {
    val topThree = remember(slices) { slices.take(3) }
    val othersShare = remember(slices) { slices.drop(3).sumOf { it.ratio } }
    val topThreeShare = remember(topThree) { topThree.sumOf { it.ratio } }

    // Cache dp conversions and drawing objects
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { 12.dp.toPx() }
    val donutStroke = remember(strokeWidthPx) { Stroke(width = strokeWidthPx, cap = StrokeCap.Round) }
    val othersColor = remember { Color.Gray.copy(alpha = 0.3f) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(92.dp),
            contentAlignment = Alignment.Center
        ) {
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
                Text(
                    text = formatPercent(topThreeShare),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "\u524d\u4e09",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            topThree.forEach { slice ->
                SimpleDonutLegendItem(
                    name = slice.name,
                    percent = formatPercent(slice.ratio),
                    color = slice.color
                )
            }
            if (othersShare > 0) {
                SimpleDonutLegendItem(
                    name = "\u5176\u4ed6",
                    percent = formatPercent(othersShare),
                    color = Color.Gray.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun SimpleDonutLegendItem(
    name: String,
    percent: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = percent,
            modifier = Modifier.width(44.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            maxLines = 1
        )
    }
}
@Composable
private fun SimpleRankingBars(
    entries: List<CategoryRankingEntry>,
    totalAmount: Double
) {
    val maxValue = entries.maxOfOrNull { it.amount }?.takeIf { it > 0 } ?: 1.0
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        entries.forEachIndexed { index, entry ->
            val share = if (totalAmount == 0.0) 0.0 else entry.amount / totalAmount
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(16.dp)
                        )
                        Text(
                            text = entry.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "${formatCurrency(entry.amount)} \u00b7 ${formatPercent(share)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LinearProgressIndicator(
                    progress = { (entry.amount / maxValue).toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = entry.color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SimpleRankItem(
    rank: Int,
    title: String,
    subtitle: String,
    amount: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = rank.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = amount,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }
}

data class BillAnalytics(
    val summary: MoneySummary,
    val trendBuckets: List<ExpenseBucket>,
    val categorySlices: List<CategorySlice>,
    val categoryRanking: List<CategoryRankingEntry>,
    val topExpenseTransactions: List<TransactionRecord>,
    val topThreeShare: Double
)

private fun buildBillAnalytics(transactions: List<TransactionRecord>, isYearOverview: Boolean): BillAnalytics {
    val summary = summarize(transactions)
    val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
    val transactionDates = transactions.map { it.date }.sorted()
    val transactionMonths = transactionDates.map { YearMonth.from(it) }.distinct()

    val trendBuckets = when {
        expenses.isEmpty() -> emptyList()
        isYearOverview -> {
            val year = transactions.firstOrNull()?.date?.year ?: expenses.first().date.year
            buildMonthlyTrendBuckets(
                expenses = expenses,
                startMonth = YearMonth.of(year, 1),
                endMonth = YearMonth.of(year, 12)
            )
        }
        transactionMonths.size == 1 -> {
            buildDailyTrendBuckets(
                expenses = expenses,
                month = transactionMonths.first()
            )
        }
        else -> {
            buildMonthlyTrendBuckets(
                expenses = expenses,
                startMonth = transactionMonths.first(),
                endMonth = transactionMonths.last()
            )
        }
    }

    val categoryTotals = expenses.groupBy { it.category }
        .map { (category, records) ->
            CategoryRankingEntry(
                name = category,
                amount = records.sumOf { it.amount },
                color = colorFromHex(records.firstOrNull()?.colorHex ?: "#6B7280")
            )
        }
        .sortedByDescending { it.amount }

    val totalExpense = summary.expense
    val topThree = categoryTotals.take(3)
    val othersTotal = categoryTotals.drop(3).sumOf { it.amount }

    val slices = buildList {
        topThree.forEach { entry ->
            add(
                CategorySlice(
                    name = entry.name,
                    amount = entry.amount,
                    ratio = if (totalExpense == 0.0) 0.0 else entry.amount / totalExpense,
                    color = entry.color
                )
            )
        }
        if (othersTotal > 0.0) {
            add(
                CategorySlice(
                    name = "\u5176\u4ed6",
                    amount = othersTotal,
                    ratio = if (totalExpense == 0.0) 0.0 else othersTotal / totalExpense,
                    color = Color.Gray.copy(alpha = 0.5f)
                )
            )
        }
    }

    return BillAnalytics(
        summary = summary,
        trendBuckets = trendBuckets,
        categorySlices = slices,
        categoryRanking = categoryTotals,
        topExpenseTransactions = expenses.sortedByDescending { it.amount },
        topThreeShare = if (totalExpense == 0.0) 0.0 else topThree.sumOf { it.amount } / totalExpense
    )
}

private fun buildDailyTrendBuckets(
    expenses: List<TransactionRecord>,
    month: YearMonth
): List<ExpenseBucket> {
    val groupedByDate = expenses.groupBy { it.date }
    return (1..month.lengthOfMonth()).map { dayOfMonth ->
        val date = month.atDay(dayOfMonth)
        ExpenseBucket(
            label = dayOfMonth.toString(),
            amount = groupedByDate[date].orEmpty().sumOf { it.amount }
        )
    }
}

private fun buildMonthlyTrendBuckets(
    expenses: List<TransactionRecord>,
    startMonth: YearMonth,
    endMonth: YearMonth
): List<ExpenseBucket> {
    val groupedByMonth = expenses.groupBy { YearMonth.from(it.date) }
    val buckets = mutableListOf<ExpenseBucket>()
    var cursor = startMonth
    while (!cursor.isAfter(endMonth)) {
        val records = groupedByMonth[cursor].orEmpty()
        buckets += ExpenseBucket(
            label = "${cursor.monthValue}\u6708",
            amount = records.sumOf { it.amount }
        )
        cursor = cursor.plusMonths(1)
    }
    return buckets
}

private fun billTrendLabelIndexes(count: Int): Set<Int> {
    return when {
        count <= 6 -> (0 until count).toSet()
        count <= 12 -> (0 until count).toSet()
        else -> listOf(0, count / 4, count / 2, count * 3 / 4, count - 1)
            .distinct()
            .filter { it in 0 until count }
            .toSet()
    }
}
private fun formatShortCurrency(value: Double): String {
    return when {
        value >= 10000 -> "${(value / 10000).toInt()}w"
        value >= 1000 -> "${(value / 1000).toInt()}k"
        else -> value.toInt().toString()
    }
}
