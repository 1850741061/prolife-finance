package com.prolife.finance.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.prolife.finance.model.TransactionRecord
import com.prolife.finance.ui.designsystem.LedgerDesignSystem
import java.time.YearMonth

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DetailScreen(
    state: AppUiState,
    onAction: (AppAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMonthPicker by rememberSaveable { mutableStateOf(false) }
    val monthTransactions = remember(state.transactions, state.selectedMonth) {
        state.transactions
            .filter { YearMonth.from(it.date) == state.selectedMonth }
    }
    val summary = remember(monthTransactions) { summarize(monthTransactions) }
    val availableMonths = remember(state.transactions, state.selectedMonth) {
        (state.transactions.map { YearMonth.from(it.date) } + state.selectedMonth)
            .distinct()
            .sortedDescending()
            .take(12)
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = LedgerDesignSystem.Spacing.L,
                top = LedgerDesignSystem.Spacing.S,
                end = LedgerDesignSystem.Spacing.L,
                bottom = LedgerDesignSystem.Spacing.L
            ),
            verticalArrangement = Arrangement.spacedBy(LedgerDesignSystem.Spacing.M)
        ) {
            item {
                SectionTitle(
                    title = "\u5f53\u6708\u660e\u7ec6",
                    subtitle = "\u67e5\u770b\u5f53\u524d\u6708\u4efd\u7684\u6c47\u603b\u4e0e\u6bcf\u7b14\u6536\u652f\u3002"
                )
            }

            item {
                MonthNavigator(
                    title = state.selectedMonth.format(MonthFormatter),
                    subtitle = "\u6309\u6708\u67e5\u770b",
                    onPrevious = { onAction(AppAction.PreviousMonth) },
                    onNext = { onAction(AppAction.NextMonth) },
                    onOpenPicker = { showMonthPicker = true }
                )
            }

            if (availableMonths.size > 1) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(LedgerDesignSystem.Spacing.S)) {
                        Text(
                            text = "\u53ef\u9009\u6708\u4efd",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(availableMonths, key = { it.toString() }) { month ->
                                val selected = month == state.selectedMonth
                                AnimatedChip(
                                    selected = selected,
                                    onClick = { onAction(AppAction.SelectMonth(month)) },
                                    label = { Text(month.format(MonthFormatter)) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                SummaryHeroCard(
                    title = "\u6708\u5ea6\u6982\u89c8",
                    subtitle = state.selectedMonth.format(MonthFormatter),
                    summary = summary
                )
            }

            item {
                SectionTitle(
                    title = "\u672c\u6708\u660e\u7ec6",
                    subtitle = if (monthTransactions.isEmpty()) {
                        "\u672c\u6708\u8fd8\u6ca1\u6709\u8bb0\u5f55\uff0c\u5148\u8bb0\u4e00\u7b14\u5f00\u59cb\u7edf\u8ba1\u3002"
                    } else {
                        "${monthTransactions.size} \u7b14\u8bb0\u5f55\uff0c\u70b9\u51fb\u5361\u7247\u53ef\u7f16\u8f91\uff0c\u53f3\u4fa7\u53ef\u76f4\u63a5\u64cd\u4f5c\u3002"
                    }
                )
            }

            if (monthTransactions.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "\u672c\u6708\u8fd8\u6ca1\u6709\u8bb0\u5f55",
                        subtitle = "\u6dfb\u52a0\u4e00\u7b14\u6536\u5165\u6216\u652f\u51fa\u5f00\u59cb\u8bb0\u8d26\u3002",
                        actionLabel = "\u8bb0\u4e00\u7b14",
                        onAction = { onAction(AppAction.OpenCreateExpense) }
                    )
                }
            } else {
                items(
                    items = monthTransactions,
                    key = { it.id }
                ) { transaction ->
                    TransactionCard(
                        transaction = transaction,
                        onClick = { onAction(AppAction.OpenEditTransaction(transaction.id)) },
                        onEdit = { onAction(AppAction.OpenEditTransaction(transaction.id)) },
                        onDelete = { onAction(AppAction.DeleteTransaction(transaction.id)) }
                    )
                }
            }
        }

        if (showMonthPicker) {
            DetailMonthPickerDialog(
                initialMonth = state.selectedMonth,
                onDismiss = { showMonthPicker = false },
                onMonthSelected = { month ->
                    onAction(AppAction.SelectMonth(month))
                    showMonthPicker = false
                }
            )
        }
    }
}

@Composable
private fun AnimatedChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit
) {
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
            onClick = onClick,
            label = label
        )
    }
}

@Composable
private fun DetailMonthPickerDialog(
    initialMonth: YearMonth,
    onDismiss: () -> Unit,
    onMonthSelected: (YearMonth) -> Unit
) {
    var displayYear by remember(initialMonth) { mutableIntStateOf(initialMonth.year) }

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
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "\u9009\u62e9\u65e5\u671f",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "\u6309\u6708\u5207\u6362\u660e\u7ec6\u5217\u8868",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { displayYear -= 1 }) {
                            Icon(
                                imageVector = Icons.Rounded.ChevronLeft,
                                contentDescription = "Previous year"
                            )
                        }
                        Text(
                            text = "${displayYear}\u5e74",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { displayYear += 1 }) {
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = "Next year"
                            )
                        }
                    }

                    DetailMonthGrid(
                        displayYear = displayYear,
                        selectedMonth = initialMonth,
                        onMonthSelected = onMonthSelected
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailMonthGrid(
    displayYear: Int,
    selectedMonth: YearMonth,
    onMonthSelected: (YearMonth) -> Unit
) {
    val monthRows = (1..12).map { YearMonth.of(displayYear, it) }.chunked(3)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        monthRows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                rowItems.forEach { month ->
                    val selected = month == selectedMonth
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(78.dp)
                            .clickable { onMonthSelected(month) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        tonalElevation = if (selected) 4.dp else 0.dp,
                        shadowElevation = if (selected) 4.dp else 0.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = month.format(MonthFormatter),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}