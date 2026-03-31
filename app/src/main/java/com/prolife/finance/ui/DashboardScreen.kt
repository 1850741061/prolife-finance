package com.prolife.finance.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prolife.finance.model.TransactionRecord
import com.prolife.finance.model.TransactionType
import com.prolife.finance.ui.designsystem.*
import com.prolife.finance.ui.insights.InsightGenerator
import com.prolife.finance.ui.insights.InsightType
import com.prolife.finance.ui.theme.LedgerBloomTokens
import java.time.LocalDate
import java.time.YearMonth
import java.time.LocalTime

/**
 * 婵☆偓绲鹃悧鐘诲Υ?Dashboard
 * 缂傚倷鑳堕崢褔骞冩惔锝勬勃闁哄洨濮版禒娑㈡煕韫囨柨鈻曢柡渚囧幘閹峰綊濮€閻樺樊娼虫繛锝呮礌閸撴繃瀵?
 */
@Composable
fun DashboardScreen(
    state: AppUiState,
    onAction: (AppAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val currentMonth = YearMonth.now()
    val transactions = state.transactions

    // 合并过滤，避免多次遍历
    val (monthTransactions, todayTransactions, lastMonthTransactions) = remember(transactions, currentMonth, today) {
        val month = transactions.filter { YearMonth.from(it.date) == currentMonth }
        val todayList = transactions.filter { it.date == today }
        val lastMonth = transactions.filter { YearMonth.from(it.date) == currentMonth.minusMonths(1) }
        Triple(month, todayList, lastMonth)
    }
    
    val summary = remember(monthTransactions) { summarize(monthTransactions) }
    val todayExpense = remember(todayTransactions) {
        todayTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }
    
    // 闂佹眹鍨婚崰鎰板垂濮橆優娲倷閼碱剚顎?
    val insights = remember(monthTransactions, lastMonthTransactions, transactions) {
        InsightGenerator.generateInsights(monthTransactions, lastMonthTransactions, transactions)
    }
    
    // 闂佸搫鐗冮崑鎾诲级閳哄倸鐏ｆ慨姗堢畵瀵?
    val recentTransactions = remember(transactions) {
        transactions.take(5)
    }
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = LedgerDesignSystem.ContentPadding.ScreenHorizontal,
            top = LedgerDesignSystem.Spacing.S,
            end = LedgerDesignSystem.ContentPadding.ScreenHorizontal,
            bottom = LedgerDesignSystem.Spacing.L
        ),
        verticalArrangement = Arrangement.spacedBy(LedgerDesignSystem.Spacing.M)
    ) {
        // 濠电偛妫庨崹鑲╂崲鐎ｎ剚瀚?
        item {
            WelcomeHeader(date = today)
        }

        // Auto bookkeep pending
        if (state.autoBookkeepEnabled && state.pendingPayments.isNotEmpty()) {
            item {
                AutoBookkeepBanner(
                    payments = state.pendingPayments,
                    onBook = { onAction(AppAction.OpenEditorFromPayment(it)) },
                    onDismiss = { onAction(AppAction.DismissPendingPayment(it)) },
                    onDismissAll = { onAction(AppAction.DismissAllPendingPayments) }
                )
            }
        }
        
        // 闂佺绻戞繛濠囧极椤撱垹绠伴柛銉ｅ妿閸ㄥジ鏌涘Δ瀣？濠?
        item {
            KeyMetricsCard(
                balance = summary.balance,
                todayExpense = todayExpense,
                monthExpense = summary.expense,
                monthIncome = summary.income
            )
        }
        
        // 闂佸搫鎳樼紓姘跺礂濡ソ娲倷閼碱剚顎?
        if (insights.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "\u667a\u80fd\u6d1e\u5bdf",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        insights.forEach { insight ->
                            InsightCard(
                                type = when (insight.type) {
                                    InsightType.Success -> com.prolife.finance.ui.designsystem.InsightType.Success
                                    InsightType.Warning -> com.prolife.finance.ui.designsystem.InsightType.Warning
                                    InsightType.Info -> com.prolife.finance.ui.designsystem.InsightType.Info
                                    InsightType.Tip -> com.prolife.finance.ui.designsystem.InsightType.Tip
                                },
                                title = insight.title,
                                message = insight.message
                            )
                        }
                    }
            }
        }
        
        // 闂婎偄娴傞崑鍛暤鎼淬劌绠肩€广儱瀚粙?
        item {
            QuickActions(onAction = onAction)
        }
        
        // 闂佸搫鐗冮崑鎾诲级閳哄倸鐏ｆ慨姗堢畵瀵?
        item {
            RecentTransactionsCard(
                transactions = recentTransactions,
                onViewAll = { onAction(AppAction.SwitchTab(BottomDestination.DETAIL)) }
            )
        }
    }
}

@Composable
private fun WelcomeHeader(date: LocalDate) {
    val hour = LocalTime.now().hour
    val greeting = when (hour) {
        in 5..11 -> "\u65e9\u4e0a\u597d"
        in 12..17 -> "\u4e0b\u5348\u597d"
        in 18..22 -> "\u665a\u4e0a\u597d"
        else -> "\u591c\u6df1\u4e86"
    }
    
    Column {
        Text(
            text = "$greeting \uD83D\uDC4B",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "\u4eca\u5929\u662f ${date.monthValue}\u6708${date.dayOfMonth}\u65e5 ${getWeekday(date)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun KeyMetricsCard(
    balance: Double,
    todayExpense: Double,
    monthExpense: Double,
    monthIncome: Double
) {
    val balanceColor = if (balance >= 0) {
        LedgerBloomTokens.palette.income
    } else {
        LedgerBloomTokens.palette.expense
    }
    val heroContainerColor = LedgerBloomTokens.palette.hero
    val heroBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = LedgerDesignSystem.Shapes.XLarge,
        colors = CardDefaults.cardColors(
            containerColor = heroContainerColor
        ),
        border = BorderStroke(1.dp, heroBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = formatCurrency(balance),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = balanceColor
            )
            Text(
                text = "\u672c\u6708\u7ed3\u4f59",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (todayExpense > 0) {
                    LedgerBloomTokens.palette.expense.copy(alpha = 0.1f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Today,
                            contentDescription = null,
                            tint = if (todayExpense > 0) LedgerBloomTokens.palette.expense else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "\u4eca\u65e5\u652f\u51fa",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        text = formatCurrency(todayExpense),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (todayExpense > 0) LedgerBloomTokens.palette.expense else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(
                    label = "\u672c\u6708\u6536\u5165",
                    value = formatCurrency(monthIncome),
                    color = LedgerBloomTokens.palette.income,
                    icon = Icons.Rounded.ArrowDownward
                )
                MetricItem(
                    label = "\u672c\u6708\u652f\u51fa",
                    value = formatCurrency(monthExpense),
                    color = LedgerBloomTokens.palette.expense,
                    icon = Icons.Rounded.ArrowUpward
                )
            }
        }
    }
}


@Composable
private fun MetricItem(
    label: String,
    value: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuickActions(onAction: (AppAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "\u5feb\u6377\u8bb0\u8d26",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                icon = Icons.Rounded.Add,
                label = "\u8bb0\u652f\u51fa",
                color = LedgerBloomTokens.palette.expense,
                modifier = Modifier.weight(1f),
                onClick = { onAction(AppAction.OpenCreateExpense) }
            )
            QuickActionButton(
                icon = Icons.Rounded.Add,
                label = "\u8bb0\u6536\u5165",
                color = LedgerBloomTokens.palette.income,
                modifier = Modifier.weight(1f),
                onClick = { onAction(AppAction.OpenCreateIncome) }
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "buttonScale"
    )
    
    Surface(
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        val clickableModifier = Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
        Box(
            modifier = clickableModifier.padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = color
                )
            }
        }
    }
}

@Composable
private fun RecentTransactionsCard(
    transactions: List<TransactionRecord>,
    onViewAll: () -> Unit
) {
    HierarchicalCard(
        importance = CardImportance.Normal,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "\u6700\u8fd1\u4ea4\u6613",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onViewAll) {
                    Text("\u67e5\u770b\u5168\u90e8")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\u6682\u65e0\u4ea4\u6613\u8bb0\u5f55",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    transactions.forEach { transaction ->
                        RecentTransactionItem(transaction = transaction)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentTransactionItem(transaction: TransactionRecord) {
    val isExpense = transaction.type == TransactionType.EXPENSE
    val amountColor = if (isExpense) {
        LedgerBloomTokens.palette.expense
    } else {
        LedgerBloomTokens.palette.income
    }
    val sign = if (isExpense) "-" else "+"
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 缂備緡鍋夐褔宕洪崱娑樼倞闁绘劦鍓涢崹?
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colorFromHex(transaction.colorHex).copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // 缂備緡鍋夐褔宕洪崱娑樼倞闁绘劦鍓涢崹濂告煕濡ゅ啫小缂?
                    Box(
                        modifier = Modifier.size(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = transaction.category.take(1),
                            style = MaterialTheme.typography.labelSmall,
                            color = colorFromHex(transaction.colorHex)
                        )
                    }
                }
            }
            
            Column {
                Text(
                    text = transaction.category,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (transaction.note.isNotBlank()) {
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
        
        Text(
            text = "$sign${formatCurrency(transaction.amount)}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
    }
}

private fun getWeekday(date: LocalDate): String {
    return when (date.dayOfWeek.value) {
        1 -> "\u661f\u671f\u4e00"
        2 -> "\u661f\u671f\u4e8c"
        3 -> "\u661f\u671f\u4e09"
        4 -> "\u661f\u671f\u56db"
        5 -> "\u661f\u671f\u4e94"
        6 -> "\u661f\u671f\u516d"
        7 -> "\u661f\u671f\u65e5"
        else -> ""
    }
}
