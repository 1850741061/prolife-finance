package com.prolife.finance.model

import android.content.Context
import org.json.JSONArray

data class FinanceCategory(
    val name: String,
    val colorHex: String,
    val iconName: String,
    val type: TransactionType
)

val DefaultExpenseCategories = listOf(
    FinanceCategory("餐饮", "#FF9F43", "utensils", TransactionType.EXPENSE),
    FinanceCategory("交通", "#3B82F6", "car", TransactionType.EXPENSE),
    FinanceCategory("购物", "#EC4899", "bag", TransactionType.EXPENSE),
    FinanceCategory("娱乐", "#8B5CF6", "gamepad", TransactionType.EXPENSE),
    FinanceCategory("居住", "#10B981", "home", TransactionType.EXPENSE),
    FinanceCategory("医疗", "#EF4444", "heart", TransactionType.EXPENSE),
    FinanceCategory("教育", "#F59E0B", "school", TransactionType.EXPENSE),
    FinanceCategory("旅行", "#14B8A6", "plane", TransactionType.EXPENSE),
    FinanceCategory("人情", "#EAB308", "gift", TransactionType.EXPENSE),
    FinanceCategory("数码", "#6366F1", "phone", TransactionType.EXPENSE),
    FinanceCategory("奶茶", "#EC4899", "mug-hot", TransactionType.EXPENSE),
    FinanceCategory("咖啡", "#8B6914", "coffee", TransactionType.EXPENSE),
    FinanceCategory("其他", "#6B7280", "more", TransactionType.EXPENSE)
)

val DefaultIncomeCategories = listOf(
    FinanceCategory("工资", "#10B981", "salary", TransactionType.INCOME),
    FinanceCategory("奖金", "#F59E0B", "award", TransactionType.INCOME),
    FinanceCategory("兼职", "#8B5CF6", "briefcase", TransactionType.INCOME),
    FinanceCategory("投资收益", "#22C55E", "chart", TransactionType.INCOME),
    FinanceCategory("礼金", "#EC4899", "gift", TransactionType.INCOME),
    FinanceCategory("其他收入", "#6B7280", "more", TransactionType.INCOME)
)

/** 可选图标列表，供用户自定义分类时选择 */
val CategoryIconOptions = listOf(
    "utensils", "car", "bag", "gamepad", "home", "heart", "school", "plane",
    "gift", "phone", "mug-hot", "coffee", "salary", "award", "briefcase",
    "chart", "more", "star", "bookmark", "music", "pets", "fitness",
    "work", "restaurant", "local-cafe", "local-drink", "shopping-bag",
    "directions-car", "flight", "smartphone", "sports-esports", "redeem",
    "payments", "business-center", "emoji-events", "school"
)

/** 可选颜色列表 */
val CategoryColorOptions = listOf(
    "#FF9F43", "#3B82F6", "#EC4899", "#8B5CF6", "#10B981", "#EF4444",
    "#F59E0B", "#14B8A6", "#EAB308", "#6366F1", "#6B7280", "#22C55E",
    "#F97316", "#06B6D4", "#A855F7", "#E11D48", "#84CC16", "#0EA5E9"
)

/** 从 SharedPreferences 读取分类，首次使用默认值初始化 */
fun categoriesFor(type: TransactionType, context: Context): List<FinanceCategory> {
    val prefs = context.getSharedPreferences("ledger_bloom_prefs", Context.MODE_PRIVATE)
    val key = if (type == TransactionType.EXPENSE) "expense_categories" else "income_categories"
    val defaults = if (type == TransactionType.EXPENSE) DefaultExpenseCategories else DefaultIncomeCategories

    val json = prefs.getString(key, null)
    if (json != null) {
        return parseCategories(json, type)
    }
    // 首次：写入默认值
    prefs.edit().putString(key, serializeCategories(defaults)).apply()
    return defaults
}

/** 保存分类列表 */
fun saveCategories(context: Context, type: TransactionType, categories: List<FinanceCategory>) {
    val prefs = context.getSharedPreferences("ledger_bloom_prefs", Context.MODE_PRIVATE)
    val key = if (type == TransactionType.EXPENSE) "expense_categories" else "income_categories"
    prefs.edit().putString(key, serializeCategories(categories)).apply()
}

private fun serializeCategories(categories: List<FinanceCategory>): String {
    val arr = JSONArray()
    for (c in categories) {
        val obj = org.json.JSONObject().apply {
            put("name", c.name)
            put("colorHex", c.colorHex)
            put("iconName", c.iconName)
        }
        arr.put(obj)
    }
    return arr.toString()
}

private fun parseCategories(json: String, type: TransactionType): List<FinanceCategory> {
    val arr = JSONArray(json)
    val result = mutableListOf<FinanceCategory>()
    for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        result.add(
            FinanceCategory(
                name = obj.getString("name"),
                colorHex = obj.getString("colorHex"),
                iconName = obj.getString("iconName"),
                type = type
            )
        )
    }
    return result
}

// 保持旧的无参签名兼容
fun categoriesFor(type: TransactionType): List<FinanceCategory> =
    if (type == TransactionType.EXPENSE) DefaultExpenseCategories else DefaultIncomeCategories
