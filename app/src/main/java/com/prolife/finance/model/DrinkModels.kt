package com.prolife.finance.model

import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.time.LocalDate

private const val DEFAULT_MILKTEA_WEEKLY_LIMIT = 2
private const val DEFAULT_MILKTEA_MONTHLY_LIMIT = 8
private const val DEFAULT_COFFEE_WEEKLY_LIMIT = 3
private const val DEFAULT_COFFEE_MONTHLY_LIMIT = 12

enum class DrinkType(
    val wireValue: String,
    val label: String,
    val categoryName: String,
    val accentHex: String
) {
    MILK_TEA("milktea", "奶茶", "奶茶", "#EC4899"),
    COFFEE("coffee", "咖啡", "咖啡", "#8B6914");

    companion object {
        fun from(raw: String?): DrinkType =
            entries.firstOrNull { it.wireValue.equals(raw, ignoreCase = true) }
                ?: MILK_TEA
    }
}

data class DrinkSettings(
    val weeklyLimit: Int,
    val monthlyLimit: Int
) {
    fun toJsonObject(): JSONObject = JSONObject()
        .put("weeklyLimit", weeklyLimit)
        .put("monthlyLimit", monthlyLimit)

    companion object {
        fun defaultFor(type: DrinkType): DrinkSettings = when (type) {
            DrinkType.MILK_TEA -> DrinkSettings(
                weeklyLimit = DEFAULT_MILKTEA_WEEKLY_LIMIT,
                monthlyLimit = DEFAULT_MILKTEA_MONTHLY_LIMIT
            )
            DrinkType.COFFEE -> DrinkSettings(
                weeklyLimit = DEFAULT_COFFEE_WEEKLY_LIMIT,
                monthlyLimit = DEFAULT_COFFEE_MONTHLY_LIMIT
            )
        }

        fun fromJsonObject(type: DrinkType, json: JSONObject?): DrinkSettings {
            val defaults = defaultFor(type)
            return if (json == null) defaults else DrinkSettings(
                weeklyLimit = json.optInt("weeklyLimit", defaults.weeklyLimit),
                monthlyLimit = json.optInt("monthlyLimit", defaults.monthlyLimit)
            )
        }
    }
}

data class DrinkRecord(
    val id: String,
    val date: LocalDate,
    val cups: Double,
    val cost: Double,
    val sugar: String,
    val brand: String,
    val notes: String,
    val drinkType: DrinkType
) {
    fun normalized(): DrinkRecord {
        val normalizedId = normalizeDrinkRecordId(id, date)
        return if (normalizedId == id) this else copy(id = normalizedId)
    }

    fun toJsonObject(): JSONObject = JSONObject().apply {
        val normalizedId = normalizeDrinkRecordId(id, date)
        put("id", normalizedId.toLongOrNull() ?: normalizedId)
        put("date", date.toString())
        put("amount", BigDecimal.valueOf(cups).stripTrailingZeros().toPlainString())
        put("cost", BigDecimal.valueOf(cost).stripTrailingZeros().toPlainString())
        put("sugar", sugar)
        put("brand", brand)
        put("notes", notes)
        put("drinkType", drinkType.wireValue)
    }

    companion object {
        fun fromJsonObject(type: DrinkType, json: JSONObject): DrinkRecord {
            val date = runCatching { LocalDate.parse(json.optString("date")) }.getOrElse { LocalDate.now() }
            val rawAmount = json.opt("amount")
            val cups = when (rawAmount) {
                is Number -> rawAmount.toDouble()
                is String -> rawAmount.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }
            val rawCost = json.opt("cost")
            val cost = when (rawCost) {
                is Number -> rawCost.toDouble()
                is String -> rawCost.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }
            val drinkType = DrinkType.from(json.optString("drinkType").ifBlank { type.wireValue })
            return DrinkRecord(
                id = json.opt("id")?.toString().orEmpty().ifBlank { date.toEpochDay().toString() },
                date = date,
                cups = cups,
                cost = cost,
                sugar = json.optString("sugar"),
                brand = json.optString("brand"),
                notes = json.optString("notes"),
                drinkType = drinkType
            ).normalized()
        }
    }
}

data class DrinkCollection(
    val type: DrinkType,
    val records: List<DrinkRecord>,
    val settings: DrinkSettings
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put(
            "records",
            JSONArray().apply {
                records.forEach { put(it.normalized().toJsonObject()) }
            }
        )
        put("settings", settings.toJsonObject())
    }

    companion object {
        fun defaultFor(type: DrinkType): DrinkCollection = DrinkCollection(
            type = type,
            records = emptyList(),
            settings = DrinkSettings.defaultFor(type)
        )

        fun fromJsonObject(type: DrinkType, json: JSONObject?): DrinkCollection {
            val recordsJson = json?.optJSONArray("records") ?: JSONArray()
            val records = buildList {
                for (index in 0 until recordsJson.length()) {
                    add(DrinkRecord.fromJsonObject(type, recordsJson.getJSONObject(index)))
                }
            }
            return DrinkCollection(
                type = type,
                records = records,
                settings = DrinkSettings.fromJsonObject(type, json?.optJSONObject("settings"))
            )
        }
    }
}

data class FinanceSnapshot(
    val transactions: List<TransactionRecord> = emptyList(),
    val milktea: DrinkCollection = DrinkCollection.defaultFor(DrinkType.MILK_TEA),
    val coffee: DrinkCollection = DrinkCollection.defaultFor(DrinkType.COFFEE)
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put(
            "transactions",
            JSONArray().apply {
                transactions.forEach { put(it.toJsonObject()) }
            }
        )
        put("milktea", milktea.toJsonObject())
        put("coffee", coffee.toJsonObject())
    }
}

fun mergeDrinkCollections(local: DrinkCollection, remote: DrinkCollection): DrinkCollection {
    val mergedRecords = linkedMapOf<String, DrinkRecord>()
    (local.records + remote.records).forEach { record ->
        val normalized = record.normalized()
        val existing = mergedRecords[normalized.id]
        if (existing == null || normalized.completenessScore() >= existing.completenessScore()) {
            mergedRecords[normalized.id] = normalized
        }
    }

    val defaultSettings = DrinkSettings.defaultFor(local.type)
    val mergedSettings = when {
        remote.settings != defaultSettings -> remote.settings
        else -> local.settings
    }

    return DrinkCollection(
        type = local.type,
        records = mergedRecords.values.sortedWith(
            compareByDescending<DrinkRecord> { it.date }.thenByDescending { it.id }
        ),
        settings = mergedSettings
    )
}

fun mergeFinanceSnapshots(local: FinanceSnapshot, remote: FinanceSnapshot): FinanceSnapshot {
    return FinanceSnapshot(
        transactions = mergeTransactions(local.transactions, remote.transactions),
        milktea = mergeDrinkCollections(local.milktea, remote.milktea),
        coffee = mergeDrinkCollections(local.coffee, remote.coffee)
    )
}

private fun DrinkRecord.completenessScore(): Int {
    var score = 0
    if (brand.isNotBlank()) score += 2
    if (notes.isNotBlank()) score += 1
    if (sugar.isNotBlank()) score += 1
    if (cups > 0) score += 1
    if (cost > 0) score += 1
    return score
}

fun normalizeDrinkRecordId(rawId: String, date: LocalDate): String {
    val trimmed = rawId.trim()
    if (trimmed.isEmpty()) return date.toEpochDay().toString()
    if (trimmed.all(Char::isDigit)) return trimmed

    val digits = trimmed.filter(Char::isDigit)
    if (digits.length >= 8) {
        return digits.takeLast(17)
    }

    val suffix = ((trimmed.hashCode().toLong() and 0x7FFFFFFF) % 10_000)
        .toString()
        .padStart(4, '0')
    return "${date.toEpochDay()}$suffix"
}
