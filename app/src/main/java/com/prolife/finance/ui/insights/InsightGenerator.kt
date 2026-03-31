package com.prolife.finance.ui.insights

import com.prolife.finance.model.TransactionRecord
import com.prolife.finance.model.TransactionType
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

/**
 * 智能洞察生成器
 * 优先保证统计口径准确，再给出更贴近记账场景的提醒。
 */
object InsightGenerator {

    fun generateInsights(
        currentMonthTransactions: List<TransactionRecord>,
        lastMonthTransactions: List<TransactionRecord>,
        allTransactions: List<TransactionRecord>
    ): List<Insight> {
        val insights = buildList {
            addAll(generateOnboardingInsights(currentMonthTransactions, allTransactions))
            addAll(generateMonthComparison(currentMonthTransactions, lastMonthTransactions))
            addAll(generateBalanceInsights(currentMonthTransactions))
            addAll(generateBudgetAlerts(currentMonthTransactions))
            addAll(generateCategoryFocusInsights(currentMonthTransactions))
            addAll(generateTrendInsights(allTransactions))
            addAll(generateSavingTips(currentMonthTransactions))
            addAll(generateSpecialReminders(allTransactions))
        }

        return selectTopInsights(insights)
    }

    private fun generateOnboardingInsights(
        currentMonthTransactions: List<TransactionRecord>,
        allTransactions: List<TransactionRecord>
    ): List<Insight> {
        if (allTransactions.isEmpty()) {
            return listOf(
                Insight(
                    type = InsightType.Tip,
                    title = "先记下第一笔",
                    message = "先补一笔今天的收支，智能洞察会随着记录增加变得更准确。",
                    priority = 20,
                    group = InsightGroup.ONBOARDING
                )
            )
        }

        if (currentMonthTransactions.isEmpty()) {
            return listOf(
                Insight(
                    type = InsightType.Tip,
                    title = "本月还没开始记账",
                    message = "本月还没有记录，补一笔今天的收支后，首页会给出更贴近当月的提醒。",
                    priority = 24,
                    group = InsightGroup.ONBOARDING
                )
            )
        }

        if (currentMonthTransactions.size < 5) {
            return listOf(
                Insight(
                    type = InsightType.Tip,
                    title = "本月样本还偏少",
                    message = "本月再记录 ${5 - currentMonthTransactions.size} 笔左右，洞察会更稳定。",
                    priority = 22,
                    group = InsightGroup.ONBOARDING
                )
            )
        }

        return emptyList()
    }

    private fun generateMonthComparison(
        current: List<TransactionRecord>,
        last: List<TransactionRecord>
    ): List<Insight> {
        val insights = mutableListOf<Insight>()

        val currentExpense = amountByType(current, TransactionType.EXPENSE)
        val lastExpense = amountByType(last, TransactionType.EXPENSE)
        val currentIncome = amountByType(current, TransactionType.INCOME)
        val lastIncome = amountByType(last, TransactionType.INCOME)

        if (lastExpense > 0) {
            val change = ((currentExpense - lastExpense) / lastExpense * 100).toInt()
            when {
                change <= -15 -> insights += Insight(
                    type = InsightType.Success,
                    title = "支出控制优秀",
                    message = "本月支出比上月减少了 ${-change}%，继续保持现在的节奏。",
                    priority = 90,
                    group = InsightGroup.MONTHLY_SPENDING
                )

                change >= 30 -> insights += Insight(
                    type = InsightType.Warning,
                    title = "支出增长提醒",
                    message = "本月支出比上月增加了 $change%，建议回看近期几笔大额消费。",
                    priority = 92,
                    group = InsightGroup.MONTHLY_SPENDING
                )
            }
        }

        if (lastIncome > 0) {
            val incomeChange = ((currentIncome - lastIncome) / lastIncome * 100).toInt()
            if (incomeChange >= 20) {
                insights += Insight(
                    type = InsightType.Success,
                    title = "收入明显提升",
                    message = "本月收入比上月增加了 $incomeChange%，可以顺手关注一下结余是否同步增长。",
                    priority = 84,
                    group = InsightGroup.MONTHLY_INCOME
                )
            }
        }

        return insights
    }

    private fun generateBalanceInsights(transactions: List<TransactionRecord>): List<Insight> {
        if (transactions.size < 4) return emptyList()

        val income = amountByType(transactions, TransactionType.INCOME)
        val expense = amountByType(transactions, TransactionType.EXPENSE)
        val balance = income - expense

        if (income <= 0.0) {
            return if (expense >= 1200) {
                listOf(
                    Insight(
                        type = InsightType.Warning,
                        title = "本月支出较快",
                        message = "本月已支出 ${formatMoney(expense)}，但还没有收入记录，建议确认记账是否完整。",
                        priority = 88,
                        group = InsightGroup.BALANCE
                    )
                )
            } else {
                emptyList()
            }
        }

        val savingRate = balance / income
        return when {
            balance < -300 -> listOf(
                Insight(
                    type = InsightType.Warning,
                    title = "本月已超出收入",
                    message = "当前结余为 -${formatMoney(abs(balance))}，建议优先压缩非必要支出。",
                    priority = 96,
                    group = InsightGroup.BALANCE
                )
            )

            savingRate >= 0.35 -> listOf(
                Insight(
                    type = InsightType.Success,
                    title = "结余表现不错",
                    message = "本月已保留 ${savingRate.toPercent()} 的收入为结余，继续保持。",
                    priority = 82,
                    group = InsightGroup.BALANCE
                )
            )

            else -> emptyList()
        }
    }

    private fun generateBudgetAlerts(transactions: List<TransactionRecord>): List<Insight> {
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        if (expenses.isEmpty()) return emptyList()

        val categorySpending = expenses.groupBy { it.category }
            .mapValues { (_, records) -> records.sumOf { it.amount } }

        val insights = mutableListOf<Insight>()

        val foodSpending = categorySpending["餐饮"] ?: 0.0
        if (foodSpending > 2000) {
            val overPercent = ((foodSpending - 2000) / 2000 * 100).toInt()
            insights += Insight(
                type = InsightType.Warning,
                title = "餐饮支出超预算",
                message = "本月餐饮支出 ${formatMoney(foodSpending)}，已超出参考线 $overPercent%。",
                priority = 86,
                group = InsightGroup.CATEGORY_BUDGET
            )
        }

        val shoppingSpending = categorySpending["购物"] ?: 0.0
        if (shoppingSpending > 1500) {
            insights += Insight(
                type = InsightType.Warning,
                title = "购物支出偏高",
                message = "本月购物支出已经达到 ${formatMoney(shoppingSpending)}，建议先回看是否有冲动消费。",
                priority = 80,
                group = InsightGroup.CATEGORY_BUDGET
            )
        }

        return insights
    }

    private fun generateCategoryFocusInsights(transactions: List<TransactionRecord>): List<Insight> {
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        if (expenses.size < 5) return emptyList()

        val totalExpense = expenses.sumOf { it.amount }
        if (totalExpense < 600) return emptyList()

        val topCategory = expenses.groupBy { it.category }
            .mapValues { (_, records) -> records.sumOf { it.amount } }
            .maxByOrNull { it.value }
            ?: return emptyList()

        val share = topCategory.value / totalExpense
        if (share < 0.4) return emptyList()

        return listOf(
            Insight(
                type = if (share >= 0.55) InsightType.Warning else InsightType.Info,
                title = "${topCategory.key}支出占比偏高",
                message = "本月 ${topCategory.key} 占总支出的 ${share.toPercent()}，金额 ${formatMoney(topCategory.value)}。",
                priority = if (share >= 0.55) 78 else 66,
                group = InsightGroup.CATEGORY_FOCUS
            )
        )
    }

    private fun generateTrendInsights(allTransactions: List<TransactionRecord>): List<Insight> {
        val today = LocalDate.now()
        val windowStart = today.minusDays(29)
        val recentTransactions = allTransactions
            .filter { !it.date.isBefore(windowStart) }
            .sortedByDescending { it.date }

        if (recentTransactions.size < 6) return emptyList()

        val insights = mutableListOf<Insight>()

        val categoryCount = recentTransactions.groupBy { it.category }
            .mapValues { (_, records) -> records.size }
        val topCategory = categoryCount.maxByOrNull { it.value }
        if (topCategory != null && topCategory.value >= 6) {
            insights += Insight(
                type = InsightType.Info,
                title = "近 30 天消费偏好",
                message = "最近 30 天里，${topCategory.key} 一共出现了 ${topCategory.value} 次，是最常见的支出类目。",
                priority = 60,
                group = InsightGroup.RECENT_PATTERN
            )
        }

        val largeExpenses = recentTransactions.filter {
            it.type == TransactionType.EXPENSE && it.amount > 1000
        }
        if (largeExpenses.size >= 2) {
            insights += Insight(
                type = InsightType.Info,
                title = "近期大额消费偏多",
                message = "最近 30 天已有 ${largeExpenses.size} 笔超过 ${formatMoney(1000.0)} 的支出，建议留意现金流。",
                priority = 68,
                group = InsightGroup.RECENT_PATTERN
            )
        }

        return insights
    }

    private fun generateSavingTips(transactions: List<TransactionRecord>): List<Insight> {
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        if (expenses.isEmpty()) return emptyList()

        val insights = mutableListOf<Insight>()

        val smallExpenses = expenses.filter { it.amount < 50 }
        val smallTotal = smallExpenses.sumOf { it.amount }
        if (smallExpenses.size >= 8 && smallTotal > 300) {
            insights += Insight(
                type = InsightType.Tip,
                title = "小额支出在累积",
                message = "本月已有 ${smallExpenses.size} 笔低于 ${formatMoney(50.0)} 的消费，累计 ${formatMoney(smallTotal)}。",
                priority = 52,
                group = InsightGroup.SMALL_SPENDING
            )
        }

        val subscriptionKeywords = listOf("会员", "订阅", "包月", "自动续费")
        val subscriptions = expenses.filter { record ->
            subscriptionKeywords.any { keyword ->
                record.note.contains(keyword, ignoreCase = true) || record.category.contains(keyword, ignoreCase = true)
            }
        }
        if (subscriptions.isNotEmpty()) {
            val subTotal = subscriptions.sumOf { it.amount }
            insights += Insight(
                type = InsightType.Tip,
                title = "订阅服务检查",
                message = "本月与订阅相关的支出约 ${formatMoney(subTotal)}，建议确认是否都还在使用。",
                priority = 58,
                group = InsightGroup.SUBSCRIPTION
            )
        }

        return insights
    }

    private fun generateSpecialReminders(allTransactions: List<TransactionRecord>): List<Insight> {
        val insights = mutableListOf<Insight>()

        val distinctDates = allTransactions.map { it.date }.distinct().sortedDescending()
        if (distinctDates.size >= 5) {
            var streak = 1
            var currentDate = distinctDates.first()

            for (i in 1 until distinctDates.size) {
                val previousDate = distinctDates[i]
                if (ChronoUnit.DAYS.between(previousDate, currentDate) == 1L) {
                    streak++
                    currentDate = previousDate
                } else {
                    break
                }
            }

            if (streak >= 7) {
                insights += Insight(
                    type = InsightType.Success,
                    title = "记账习惯养成中",
                    message = "你已经连续记账 $streak 天了，这会让每月洞察更稳定。",
                    priority = 76,
                    group = InsightGroup.HABIT
                )
            }
        }

        val weekendRange = currentWeekendRange(LocalDate.now())
        if (weekendRange != null) {
            val weekendExpenses = allTransactions.filter {
                it.type == TransactionType.EXPENSE &&
                    !it.date.isBefore(weekendRange.first) &&
                    !it.date.isAfter(weekendRange.second)
            }.sumOf { it.amount }

            if (weekendExpenses > 500) {
                insights += Insight(
                    type = InsightType.Tip,
                    title = "本周末花销偏高",
                    message = "这个周末已经支出 ${formatMoney(weekendExpenses)}，注意别把下周预算提前花掉。",
                    priority = 46,
                    group = InsightGroup.WEEKEND
                )
            }
        }

        return insights
    }

    private fun selectTopInsights(insights: List<Insight>): List<Insight> {
        val selected = mutableListOf<Insight>()
        val usedGroups = mutableSetOf<InsightGroup>()

        insights
            .sortedWith(compareByDescending<Insight> { it.priority }.thenBy { it.title })
            .forEach { insight ->
                if (usedGroups.add(insight.group)) {
                    selected += insight
                }
                if (selected.size == 3) return selected
            }

        return selected
    }

    private fun amountByType(transactions: List<TransactionRecord>, type: TransactionType): Double =
        transactions.filter { it.type == type }.sumOf { it.amount }

    private fun currentWeekendRange(today: LocalDate): Pair<LocalDate, LocalDate>? {
        return when (today.dayOfWeek.value) {
            6 -> today to today.plusDays(1)
            7 -> today.minusDays(1) to today
            else -> null
        }
    }

    private fun formatMoney(amount: Double): String =
        String.format(Locale.getDefault(), "¥%.0f", amount)

    private fun Double.toPercent(): String =
        String.format(Locale.getDefault(), "%.0f%%", this * 100)
}

data class Insight(
    val type: InsightType,
    val title: String,
    val message: String,
    val priority: Int,
    val group: InsightGroup
)

enum class InsightType {
    Success,
    Warning,
    Info,
    Tip
}

enum class InsightGroup {
    ONBOARDING,
    MONTHLY_SPENDING,
    MONTHLY_INCOME,
    BALANCE,
    CATEGORY_BUDGET,
    CATEGORY_FOCUS,
    RECENT_PATTERN,
    SMALL_SPENDING,
    SUBSCRIPTION,
    HABIT,
    WEEKEND
}
