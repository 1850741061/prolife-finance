package com.prolife.finance.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ShowChart
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.prolife.finance.model.StatsGranularity
import com.prolife.finance.model.TransactionRecord
import com.prolife.finance.model.TransactionType
import com.prolife.finance.ui.theme.LedgerBloomTokens
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * 闂佸憡鐟崹杈╁垝閿曞倸绀夐柕濞у嫮鏆犻柣鈩冨笒椤戝懏绻涘澶婄倞闁告挆鍛厑婵? * 闂佽　鍋撴い鏍ㄧ☉閻︻喚鈧綊娼荤粻鎴ｃ亹缁嬫鐓ラ柟瀛樼箓琚熼梺鍝勮閸庡啿锕㈤崨鏉戝嵆闁哄鍩堝Ο瀣煛娴ｅ搫顣肩€规挷绶氶弫宥囦沪閻愵剚娈伴梺?婵?14婵犮垹鐏堥弲娑㈠垂韫囨稑绠? */
@Composable
fun SwipeableTrendChart(
    buckets: List<ExpenseBucket>,
    modifier: Modifier = Modifier,
    title: String = "\u652f\u51fa\u8d8b\u52bf",
    initialWindowSize: Int = 7
) {
    var windowSize by remember { mutableIntStateOf(initialWindowSize) }

    val totalDays = buckets.size
    var startIndex by remember(totalDays, windowSize) { mutableIntStateOf(0) }
    val maxStartIndex = (totalDays - windowSize).coerceAtLeast(0)

    // 缂佺虎鍙庨崰鏇犳崲濮濈artIndex闂侀潻璐熼崝宥咃耿娓氣偓瀵偊宕奸敐鍛Ш闂佹悶鍎插娆撳船?
    LaunchedEffect(windowSize, totalDays) {
        startIndex = startIndex.coerceIn(0, maxStartIndex)
    }

    val visibleBuckets = remember(buckets, startIndex, windowSize) {
        if (buckets.isEmpty()) {
            emptyList()
        } else if (buckets.size <= windowSize) {
            // 婵犵鈧啿鈧綊鎮樻径鎰瀬闁绘鐗嗙粊锕傛⒑閹绘帞绠伴柣顭戝墯缁傚秵鎯旈敐鍥х倞闂佸憡鐟辩徊浠嬪Φ閸モ晙鐒婇煫鍥ュ劤缁€澶愭煟閳轰胶鎽犻悽顖涙尦瀵即宕滆娴犳盯鏌涜箛瀣闁?
            buckets
        } else {
            val start = startIndex.coerceIn(0, buckets.size - windowSize)
            val end = (start + windowSize).coerceAtMost(buckets.size)
            buckets.subList(start, end)
        }
    }

    val maxAmount = visibleBuckets.maxOfOrNull { it.amount }?.takeIf { it > 0 } ?: 1.0

    // 婵☆偅婢樼€氼剟宕㈠☉銏″殧鐎瑰嫭婢樼徊鍨涢悧鍫㈢煄濠㈢懓锕弫宥呯暆閸曨亞绱氶梺绋跨箰缁夋潙锕?Canvas 婵炴垶鎼╅崣鈧柣銊у枛閹?@Composable
    val chartAccentColor = LedgerBloomTokens.palette.chartAccent
    val heroColor = LedgerBloomTokens.palette.hero

    // Cache dp conversions — avoids dp.toPx() on every draw frame
    val density = LocalDensity.current
    val paddingPx = with(density) { 16.dp.toPx() }
    val lineStrokeWidthPx = with(density) { 3.dp.toPx() }
    val peakDotRadiusPx = with(density) { 6.dp.toPx() }
    val normalDotRadiusPx = with(density) { 4.dp.toPx() }

    // Reuse Path objects to avoid allocation on every draw frame
    val fillPath = remember { Path() }
    val linePath = remember { Path() }

    Column(modifier = modifier.fillMaxWidth()) {
        // 闂佸搫绉村ú顓€傛禒瀣唨闊洦鎸荤€氳尙绱掗幇顓ф當鐟滅増鐩畷姘跺炊閵娿儱绨?
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.ShowChart,
                    contentDescription = null,
                    tint = chartAccentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(7, 14).forEach { size ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (windowSize == size) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { windowSize = size }
                    ) {
                        Text(
                            text = "${size}\u5929",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (windowSize == size) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { startIndex = (startIndex - 1).coerceAtLeast(0) },
                enabled = startIndex > 0
            ) {
                Text(
                    text = "<",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (startIndex > 0) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }

            // 闂佹悶鍎插畷姗€濡撮崘顔肩闁告繂瀚崢?
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(140.dp)
            ) {
                if (visibleBuckets.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "\u6682\u65e0\u6570\u636e",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val chartHeight = canvasHeight - paddingPx * 2
                        val chartWidth = canvasWidth - paddingPx * 2

                        val points = if (visibleBuckets.size == 1) {
                            listOf(Offset(canvasWidth / 2, canvasHeight - paddingPx - (chartHeight * 0.5f)))
                        } else {
                            val step = chartWidth / (visibleBuckets.size - 1)
                            visibleBuckets.mapIndexed { index, bucket ->
                                val x = paddingPx + step * index
                                val y = paddingPx + chartHeight - (chartHeight * (bucket.amount / maxAmount)).toFloat()
                                Offset(x, y)
                            }
                        }

                        // 缂傚倷鐒﹂敋闁糕晜顨呴妴鎺楀箛椤掆偓缂嶄礁鈹戞径妯轰簻闁?
                        if (points.size > 1) {
                            fillPath.rewind()
                            fillPath.moveTo(points.first().x, canvasHeight - paddingPx)
                            points.forEach { fillPath.lineTo(it.x, it.y) }
                            fillPath.lineTo(points.last().x, canvasHeight - paddingPx)
                            fillPath.close()
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        chartAccentColor.copy(alpha = 0.3f),
                                        chartAccentColor.copy(alpha = 0.05f)
                                    ),
                                    startY = paddingPx,
                                    endY = canvasHeight - paddingPx
                                )
                            )

                            linePath.rewind()
                            linePath.moveTo(points.first().x, points.first().y)
                            points.drop(1).forEach { linePath.lineTo(it.x, it.y) }
                            drawPath(
                                path = linePath,
                                color = chartAccentColor,
                                style = Stroke(width = lineStrokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }

                        val peakAmount = visibleBuckets.maxOfOrNull { it.amount } ?: 0.0
                        points.forEachIndexed { index, point ->
                            val isPeak = visibleBuckets[index].amount == peakAmount && peakAmount > 0
                            val pointColor = if (isPeak) heroColor else chartAccentColor
                            val radius = if (isPeak) peakDotRadiusPx else normalDotRadiusPx

                            if (isPeak) {
                                drawCircle(
                                    color = pointColor.copy(alpha = 0.2f),
                                    radius = radius * 2,
                                    center = point
                                )
                            }

                            drawCircle(
                                color = Color.White,
                                radius = radius,
                                center = point
                            )
                            drawCircle(
                                color = pointColor,
                                radius = radius * 0.6f,
                                center = point
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = { startIndex = (startIndex + 1).coerceAtMost(maxStartIndex) },
                enabled = startIndex < maxStartIndex
            ) {
                Text(
                    text = ">",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (startIndex < maxStartIndex) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
        }

        // 闂佸搫鍟ㄩ崕鎻掞耿閿熺姴鍐€闁搞儮鏅╅崝?
        if (visibleBuckets.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                visibleBuckets.forEachIndexed { index, bucket ->
                    if (index == 0 || index == visibleBuckets.size - 1 ||
                        visibleBuckets[index].amount > 0 && visibleBuckets[index].amount == visibleBuckets.maxOfOrNull { it.amount }) {
                        Text(
                            text = bucket.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                }
            }
        }
    }
}

/**
 * 闁汇埄鍨介梽鍕涢埡鍛珘闁绘梹妞块崵濠囨煙椤栨碍鍣归柤鏉戯功缁絽螖閸愨晝鏆犻梺鍝勫暔閸庡崬顔忛懡銈嗗枂闁糕剝顨嗙粋?
 */
@Composable
fun CalendarExpenseViewWithDetail(
    buckets: List<ExpenseBucket>,
    granularity: StatsGranularity,
    anchorDate: LocalDate,
    allTransactions: List<TransactionRecord> = emptyList()
) {
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDetailDialog by remember { mutableStateOf(false) }

    val dateTransactions = remember(selectedDate, allTransactions) {
        selectedDate?.let { date ->
            allTransactions.filter { it.date == date }
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
            YearCalendarViewPublic(buckets = buckets, anchorDate = anchorDate)
        }
    }

    // 闁荤姴娴勯梽鍕磿韫囨搩鍤曢柛锔诲幘瀹?
    if (showDetailDialog && selectedDate != null) {
        DayDetailDialog(
            date = selectedDate!!,
            transactions = dateTransactions,
            onDismiss = { showDetailDialog = false }
        )
    }
}

private val DayOfWeekLabels = listOf("\u4e00", "\u4e8c", "\u4e09", "\u56db", "\u4e94", "\u516d", "\u65e5")

@Composable
fun WeekCalendarViewWithDetail(
    buckets: List<ExpenseBucket>,
    anchorDate: LocalDate,
    onDateClick: (LocalDate) -> Unit
) {
    val maxAmount = buckets.maxOfOrNull { it.amount }?.takeIf { it > 0 } ?: 1.0
    val startOfWeek = anchorDate.minusDays((anchorDate.dayOfWeek.value - 1).toLong())

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DayOfWeekLabels.forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            (0..6).forEach { offset ->
                val day = startOfWeek.plusDays(offset.toLong())
                val bucket = buckets.find { it.dateLabel == day.format(DayFormatter) }
                val isToday = day == LocalDate.now()
                val intensity = ((bucket?.amount ?: 0.0) / maxAmount).toFloat()

                CalendarDayCellWithClick(
                    day = day.dayOfMonth.toString(),
                    amount = bucket?.amount ?: 0.0,
                    intensity = intensity,
                    isToday = isToday,
                    hasExpense = (bucket?.amount ?: 0.0) > 0,
                    onClick = { onDateClick(day) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun MonthCalendarViewWithDetail(
    buckets: List<ExpenseBucket>,
    anchorDate: LocalDate,
    onDateClick: (LocalDate) -> Unit
) {
    val yearMonth = YearMonth.from(anchorDate)
    val firstDayOfMonth = yearMonth.atDay(1)
    val startDayOfWeek = firstDayOfMonth.dayOfWeek.value
    val maxAmount = buckets.maxOfOrNull { it.amount }?.takeIf { it > 0 } ?: 1.0

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DayOfWeekLabels.forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        val totalCells = ((startDayOfWeek - 1) + yearMonth.lengthOfMonth())
        val rows = kotlin.math.ceil(totalCells / 7.0).toInt()

        repeat(rows) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(7) { col ->
                    val cellDate = firstDayOfMonth.minusDays((startDayOfWeek - 1).toLong())
                        .plusDays((row * 7 + col).toLong())
                    val isCurrentMonth = cellDate.month == yearMonth.month
                    val dayOfMonth = cellDate.dayOfMonth
                    val bucket = buckets.find { it.dateLabel == cellDate.format(DayFormatter) }
                    val isToday = cellDate == LocalDate.now()
                    val intensity = ((bucket?.amount ?: 0.0) / maxAmount).toFloat()

                    CalendarDayCellWithClick(
                        day = dayOfMonth.toString(),
                        amount = bucket?.amount ?: 0.0,
                        intensity = intensity,
                        isToday = isToday,
                        hasExpense = (bucket?.amount ?: 0.0) > 0,
                        isCurrentMonth = isCurrentMonth,
                        onClick = { onDateClick(cellDate) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * 濡ょ姷鍋熼ˉ鎰帮綖鐎ｎ喖鐐?- 闂佺娴氶崜娆戞閹达附鍋嬮柛顐ゅ枑閹?
 */
@Composable
fun YearCalendarViewPublic(
    buckets: List<ExpenseBucket>,
    anchorDate: LocalDate
) {
    val maxAmount = buckets.maxOfOrNull { it.amount }?.takeIf { it > 0 } ?: 1.0
    val monthLabels = listOf("1\u6708", "2\u6708", "3\u6708", "4\u6708", "5\u6708", "6\u6708", "7\u6708", "8\u6708", "9\u6708", "10\u6708", "11\u6708", "12\u6708")

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(4) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(3) { col ->
                    val monthIndex = row * 3 + col
                    val bucket = buckets.getOrNull(monthIndex)
                    val intensity = ((bucket?.amount ?: 0.0) / maxAmount).toFloat()

                    MonthCell(
                        monthLabel = monthLabels[monthIndex],
                        amount = bucket?.amount ?: 0.0,
                        intensity = intensity,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCellWithClick(
    day: String,
    amount: Double,
    intensity: Float,
    isToday: Boolean,
    hasExpense: Boolean,
    modifier: Modifier = Modifier,
    isCurrentMonth: Boolean = true,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        intensity > 0.7f -> LedgerBloomTokens.palette.expense.copy(alpha = 0.3f)
        intensity > 0.4f -> LedgerBloomTokens.palette.expense.copy(alpha = 0.2f)
        intensity > 0.1f -> LedgerBloomTokens.palette.expense.copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }

    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .then(
                if (isToday) {
                    Modifier.border(
                        width = 2.dp,
                        color = LedgerBloomTokens.palette.hero,
                        shape = RoundedCornerShape(8.dp)
                    )
                } else Modifier
            )
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = day,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (isCurrentMonth) MaterialTheme.colorScheme.onSurface
                   else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (hasExpense) {
            Text(
                text = formatShortAmount(amount),
                style = MaterialTheme.typography.labelSmall,
                color = LedgerBloomTokens.palette.expense,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MonthCell(
    monthLabel: String,
    amount: Double,
    intensity: Float,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        intensity > 0.7f -> LedgerBloomTokens.palette.expense.copy(alpha = 0.3f)
        intensity > 0.4f -> LedgerBloomTokens.palette.expense.copy(alpha = 0.2f)
        intensity > 0.1f -> LedgerBloomTokens.palette.expense.copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = monthLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (amount > 0) {
                Text(
                    text = formatShortAmount(amount),
                    style = MaterialTheme.typography.labelSmall,
                    color = LedgerBloomTokens.palette.expense,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 闂佸搫绉堕崢褏妲愰敓鐘茬闁哄稁鍘煎▍銊ノ涢悧鍫㈢畱閻犳劗鍠撶划濠氬焵椤掑嫭鍎楁い鎾跺Т閸ら亶鏌℃担瑙勭凡闁烩姍鍡愪汗闁靛繒濮风涵鈧梺鎸庣☉閻楀懐绮径灞惧暫濞达絿鏌夐崢顒勬煛閸屾碍绁紒?
 */
private fun formatShortAmount(amount: Double): String {
    val absAmount = kotlin.math.abs(amount)
    return when {
        absAmount >= 10000 -> "${(absAmount / 10000).toInt()}w"
        absAmount >= 1000 -> "${(absAmount / 1000).toInt()}k"
        else -> absAmount.toInt().toString()
    }
}

/**
 * 闂佸搫鍟ㄩ崕鎻掞耿閿涘嫭瀚氶柨鏃囨閸撴壆鈧鍠栧﹢閬嶆偘?
 */
@Composable
private fun DayDetailDialog(
    date: LocalDate,
    transactions: List<TransactionRecord>,
    onDismiss: () -> Unit
) {
    val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
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
                // 闂佸搫绉村ú顓€?
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
                        color = LedgerBloomTokens.palette.expense.copy(alpha = 0.1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
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
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = LedgerBloomTokens.palette.income.copy(alpha = 0.1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
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
                    }
                }

                // 婵炲瓨鍤庨崐鏍ｅΔ鍛婵°倕瀚ㄩ埀?
                if (transactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "\u5f53\u5929\u6ca1\u6709\u8bb0\u5f55",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                text = "\u5f53\u5929\u8bb0\u5f55 (${transactions.size}\u7b14)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        items(
                            items = transactions.sortedByDescending { it.amount },
                            key = { it.id }
                        ) { record ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(colorFromHex(record.colorHex))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = record.category,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (record.note.isNotBlank()) {
                                        Text(
                                            text = " \u00b7 ${record.note}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                    }
                                }
                                Text(
                                    text = if (record.type == TransactionType.INCOME) "+${formatCurrency(record.amount)}"
                                           else "-${formatCurrency(record.amount)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (record.type == TransactionType.INCOME) LedgerBloomTokens.palette.income
                                           else LedgerBloomTokens.palette.expense
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
