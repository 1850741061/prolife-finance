package com.prolife.finance.data

import android.content.Context
import com.prolife.finance.model.DrinkCollection
import com.prolife.finance.model.DrinkType
import com.prolife.finance.model.FinanceSnapshot
import com.prolife.finance.model.TransactionRecord
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class FinanceCache(context: Context) {
    private val cacheFile = File(context.filesDir, "finance_cache.json")
    private val legacyTransactionCacheFile = File(context.filesDir, "transactions_cache.json")

    fun readSnapshot(): FinanceSnapshot {
        if (cacheFile.exists()) {
            val raw = cacheFile.readText()
            if (raw.isNotBlank()) {
                return parseSnapshot(raw)
            }
        }

        if (legacyTransactionCacheFile.exists()) {
            val raw = legacyTransactionCacheFile.readText()
            if (raw.isNotBlank()) {
                return FinanceSnapshot(
                    transactions = parseTransactions(JSONArray(raw))
                )
            }
        }

        return FinanceSnapshot()
    }

    fun writeSnapshot(snapshot: FinanceSnapshot) {
        cacheFile.writeText(snapshot.toJsonObject().toString())
    }

    fun clear() {
        if (cacheFile.exists()) {
            cacheFile.delete()
        }
        if (legacyTransactionCacheFile.exists()) {
            legacyTransactionCacheFile.delete()
        }
    }

    private fun parseSnapshot(raw: String): FinanceSnapshot {
        val trimmed = raw.trimStart()
        return if (trimmed.startsWith("[")) {
            FinanceSnapshot(transactions = parseTransactions(JSONArray(raw)))
        } else {
            val json = JSONObject(raw)
            FinanceSnapshot(
                transactions = parseTransactions(json.optJSONArray("transactions") ?: JSONArray()),
                milktea = DrinkCollection.fromJsonObject(DrinkType.MILK_TEA, json.optJSONObject("milktea")),
                coffee = DrinkCollection.fromJsonObject(DrinkType.COFFEE, json.optJSONObject("coffee"))
            )
        }
    }

    private fun parseTransactions(jsonArray: JSONArray): List<TransactionRecord> = buildList {
        for (index in 0 until jsonArray.length()) {
            add(TransactionRecord.fromJsonObject(jsonArray.getJSONObject(index)))
        }
    }
}
