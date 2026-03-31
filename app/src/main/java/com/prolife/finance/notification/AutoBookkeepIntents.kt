package com.prolife.finance.notification

import android.content.Intent

const val EXTRA_PAYMENT_SOURCE = "extra_payment_source"
const val EXTRA_PAYMENT_AMOUNT = "extra_payment_amount"
const val EXTRA_PAYMENT_MERCHANT = "extra_payment_merchant"
const val EXTRA_PAYMENT_TIMESTAMP = "extra_payment_timestamp"
const val EXTRA_PAYMENT_NOTIFICATION_KEY = "extra_payment_notification_key"

fun Intent.putParsedPayment(payment: ParsedPayment): Intent = apply {
    putExtra(EXTRA_PAYMENT_SOURCE, payment.source.name)
    putExtra(EXTRA_PAYMENT_AMOUNT, payment.amount)
    putExtra(EXTRA_PAYMENT_MERCHANT, payment.merchant)
    putExtra(EXTRA_PAYMENT_TIMESTAMP, payment.timestamp)
    putExtra(EXTRA_PAYMENT_NOTIFICATION_KEY, payment.notificationKey)
}

fun Intent.toParsedPaymentOrNull(): ParsedPayment? {
    val sourceName = getStringExtra(EXTRA_PAYMENT_SOURCE) ?: return null
    val amount = getStringExtra(EXTRA_PAYMENT_AMOUNT) ?: return null
    val merchant = getStringExtra(EXTRA_PAYMENT_MERCHANT).orEmpty()
    val timestamp = getLongExtra(EXTRA_PAYMENT_TIMESTAMP, -1L)
    val notificationKey = getStringExtra(EXTRA_PAYMENT_NOTIFICATION_KEY) ?: return null
    if (timestamp <= 0L) return null

    val source = runCatching { PaymentSource.valueOf(sourceName) }.getOrNull() ?: return null
    return ParsedPayment(
        source = source,
        amount = amount,
        merchant = merchant,
        timestamp = timestamp,
        notificationKey = notificationKey
    )
}
