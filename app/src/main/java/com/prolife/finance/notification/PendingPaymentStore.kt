package com.prolife.finance.notification

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class PendingPaymentStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ledger_bloom_auto_book", Context.MODE_PRIVATE)

    fun enqueue(payment: ParsedPayment) {
        val existing = loadAll()
        if (existing.any { it.notificationKey == payment.notificationKey }) return
        saveAll(existing + payment)
    }

    fun loadAll(): List<ParsedPayment> {
        val raw = prefs.getString(KEY_PENDING_PAYMENTS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i -> fromJson(arr.getJSONObject(i)) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun remove(payment: ParsedPayment) {
        saveAll(loadAll().filterNot { it.notificationKey == payment.notificationKey })
    }

    fun removeByNotificationKey(notificationKey: String) {
        saveAll(loadAll().filterNot { it.notificationKey == notificationKey })
    }

    fun clearAll() {
        prefs.edit().remove(KEY_PENDING_PAYMENTS).apply()
    }

    private fun saveAll(payments: List<ParsedPayment>) {
        val arr = JSONArray()
        payments.forEach { arr.put(toJson(it)) }
        prefs.edit().putString(KEY_PENDING_PAYMENTS, arr.toString()).apply()
    }

    private fun toJson(p: ParsedPayment): JSONObject = JSONObject().apply {
        put(KEY_SOURCE, p.source.name)
        put(KEY_AMOUNT, p.amount)
        put(KEY_MERCHANT, p.merchant)
        put(KEY_TIMESTAMP, p.timestamp)
        put(KEY_NOTIFICATION_KEY, p.notificationKey)
    }

    private fun fromJson(json: JSONObject): ParsedPayment = ParsedPayment(
        source = PaymentSource.valueOf(json.getString(KEY_SOURCE)),
        amount = json.getString(KEY_AMOUNT),
        merchant = json.optString(KEY_MERCHANT, ""),
        timestamp = json.getLong(KEY_TIMESTAMP),
        notificationKey = json.getString(KEY_NOTIFICATION_KEY)
    )

    companion object {
        private const val KEY_PENDING_PAYMENTS = "pending_payments_json"
        private const val KEY_SOURCE = "source"
        private const val KEY_AMOUNT = "amount"
        private const val KEY_MERCHANT = "merchant"
        private const val KEY_TIMESTAMP = "timestamp"
        private const val KEY_NOTIFICATION_KEY = "notification_key"
    }
}
