package com.prolife.finance.model

import org.json.JSONObject
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

enum class TransactionType(val wireValue: String) {
    EXPENSE("expense"),
    INCOME("income");

    companion object {
        fun from(raw: String?): TransactionType =
            if (raw.equals(INCOME.wireValue, ignoreCase = true)) INCOME else EXPENSE
    }
}

data class TransactionRecord(
    val id: String,
    val type: TransactionType,
    val date: LocalDate,
    val category: String,
    val amount: Double,
    val note: String,
    val colorHex: String,
    val updatedAt: Instant,
    val createdAt: Instant? = null,
    val milkteaRecordId: String? = null,
    val extraFieldsJson: String? = null
) {
    fun toJsonObject(): JSONObject {
        val base = parseExtraFields(extraFieldsJson)
        val normalizedId = normalizeLegacyTransactionId(id, updatedAt, milkteaRecordId)
        base.put("id", normalizedId.toLongOrNull() ?: normalizedId)
        base.put("type", type.wireValue)
        base.put("date", date.toString())
        base.put("category", category)
        base.put("amount", BigDecimal.valueOf(amount).stripTrailingZeros().toPlainString())
        base.put("note", note)
        base.put("catColor", colorHex)
        createdAt?.let { base.put("createdAt", it.toString()) }
        milkteaRecordId?.let { base.put("milkteaRecordId", it.toLongOrNull() ?: it) }
        base.put("updatedAt", updatedAt.toString())
        return base
    }

    companion object {
        private val KnownKeys = setOf(
            "id",
            "type",
            "date",
            "category",
            "amount",
            "note",
            "catColor",
            "updatedAt",
            "createdAt",
            "milkteaRecordId"
        )

        fun fromJsonObject(json: JSONObject): TransactionRecord {
            val rawDate = json.optString("date").ifBlank { LocalDate.now().toString() }
            val parsedDate = runCatching { LocalDate.parse(rawDate) }.getOrElse { LocalDate.now() }
            val createdAt = json.optString("createdAt")
                .takeIf { it.isNotBlank() }
                ?.let { runCatching { Instant.parse(it) }.getOrNull() }
            val parsedUpdatedAt = json.optString("updatedAt")
                .takeIf { it.isNotBlank() }
                ?.let { runCatching { Instant.parse(it) }.getOrNull() }
                ?: createdAt
                ?: Instant.now()

            val rawAmount = json.opt("amount")
            val amount = when (rawAmount) {
                is Number -> rawAmount.toDouble()
                is String -> rawAmount.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }

            val detectedType = TransactionType.from(json.optString("type"))
            val category = json.optString("category", "其他")
            val fallbackColor = categoriesFor(detectedType)
                .firstOrNull { it.name == category }
                ?.colorHex
                ?: "#6B7280"
            val linkedDrinkId = json.opt("milkteaRecordId")
                ?.takeUnless { it == JSONObject.NULL }
                ?.toString()
                ?.trim()
                ?.ifBlank { null }
            val extraFields = JSONObject()
            val iterator = json.keys()
            while (iterator.hasNext()) {
                val key = iterator.next()
                if (key !in KnownKeys) {
                    extraFields.put(key, json.get(key))
                }
            }
            val extraFieldsJson = extraFields.takeIf { it.length() > 0 }?.toString()

            return TransactionRecord(
                id = json.opt("id")?.toString().orEmpty().ifBlank { System.currentTimeMillis().toString() },
                type = detectedType,
                date = parsedDate,
                category = category,
                amount = amount,
                note = json.optString("note"),
                colorHex = json.optString("catColor").ifBlank { fallbackColor },
                updatedAt = parsedUpdatedAt,
                createdAt = createdAt,
                milkteaRecordId = linkedDrinkId,
                extraFieldsJson = extraFieldsJson
            )
        }
    }
}

private val numericTransactionId = Regex("^\\d+$")
private val prefixedTimestampId = Regex("^tx_(\\d+)$", RegexOption.IGNORE_CASE)
private val prefixedDrinkId = Regex("^mt_(\\d+)$", RegexOption.IGNORE_CASE)

fun normalizeLegacyTransactionId(rawId: String, updatedAt: Instant, milkteaRecordId: String? = null): String {
    milkteaRecordId?.let { linkedId ->
        return "mt_${normalizeLegacyIdValue(linkedId.trim(), updatedAt)}"
    }

    val trimmed = rawId.trim()
    if (trimmed.isEmpty()) return updatedAt.toEpochMilli().toString()

    prefixedDrinkId.matchEntire(trimmed)?.let { match ->
        return "mt_${normalizeLegacyIdValue(match.groupValues[1], updatedAt)}"
    }

    prefixedTimestampId.matchEntire(trimmed)?.let { match ->
        return normalizeLegacyIdValue(match.groupValues[1], updatedAt)
    }

    return normalizeLegacyIdValue(trimmed, updatedAt)
}

private fun normalizeLegacyIdValue(rawValue: String, updatedAt: Instant): String {
    val trimmed = rawValue.trim()
    if (trimmed.isEmpty()) return updatedAt.toEpochMilli().toString()

    if (numericTransactionId.matches(trimmed)) {
        return trimmed
    }

    val digits = trimmed.filter(Char::isDigit)
    if (digits.length >= 8) {
        return digits.takeLast(17)
    }

    val suffix = ((trimmed.hashCode().toLong() and 0x7FFFFFFF) % 10_000).toString().padStart(4, '0')
    return "${updatedAt.toEpochMilli()}$suffix"
}

fun TransactionRecord.normalizedForLegacyDesktop(): TransactionRecord {
    val normalizedId = normalizeLegacyTransactionId(id, updatedAt, milkteaRecordId)
    return if (normalizedId == id) this else copy(id = normalizedId)
}

fun List<TransactionRecord>.normalizedForLegacyDesktop(): List<TransactionRecord> =
    map(TransactionRecord::normalizedForLegacyDesktop)

fun mergeTransactions(local: List<TransactionRecord>, remote: List<TransactionRecord>): List<TransactionRecord> {
    val merged = linkedMapOf<String, TransactionRecord>()
    (local + remote).forEach { record ->
        val normalizedRecord = record.normalizedForLegacyDesktop()
        val existing = merged[normalizedRecord.id]
        if (existing == null || normalizedRecord.updatedAt.isAfter(existing.updatedAt)) {
            merged[normalizedRecord.id] = normalizedRecord
        }
    }
    return merged.values.sortedWith(compareByDescending<TransactionRecord> { it.date }.thenByDescending { it.updatedAt })
}

private fun parseExtraFields(raw: String?): JSONObject {
    if (raw.isNullOrBlank()) return JSONObject()
    return runCatching { JSONObject(raw) }.getOrElse { JSONObject() }
}
