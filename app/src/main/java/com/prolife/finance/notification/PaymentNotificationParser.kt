package com.prolife.finance.notification

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

enum class PaymentSource(val label: String, val packageName: String) {
    WECHAT("微信", "com.tencent.mm"),
    ALIPAY("支付宝", "com.eg.android.AlipayGphone")
}

data class ParsedPayment(
    val source: PaymentSource,
    val amount: String,
    val merchant: String,
    val timestamp: Long,
    val notificationKey: String
)

object PaymentNotificationParser {

    val TARGET_PACKAGES: Set<String> = PaymentSource.entries.map { it.packageName }.toSet()

    private val WECHAT_TRIGGER_KEYWORDS = listOf(
        "微信支付",
        "微信支付凭证",
        "支付凭证",
        "付款成功",
        "交易提醒"
    )
    private val WECHAT_EXCLUDE_KEYWORDS = listOf(
        "收款到账",
        "转账收款",
        "二维码收款",
        "已存入零钱",
        "退款成功",
        "退款到账"
    )
    private val WECHAT_AMOUNT_PATTERNS = listOf(
        Regex("""[¥￥]\s*(\d+(?:\.\d{1,2})?)"""),
        Regex("""一笔\s*(\d+(?:\.\d{1,2})?)\s*元(?:的)?支出"""),
        Regex("""(?:支出|消费|付款|支付)\s*(\d+(?:\.\d{1,2})?)\s*元""")
    )
    private val WECHAT_MERCHANT_PATTERNS = listOf(
        Regex("""你在(.+?)(?:消费|支付|付款|向)"""),
        Regex("""向(.+?)(?:付款|支付)"""),
        Regex("""收款方[:：]\s*([^\s]+(?:\s*[^\s，。]+)*)"""),
        Regex("""商户[:：]\s*([^\s]+(?:\s*[^\s，。]+)*)""")
    )

    private val ALIPAY_TRIGGER_KEYWORDS = listOf(
        "交易提醒",
        "账单提醒",
        "支付成功",
        "付款成功",
        "支付通知",
        "扣款提醒",
        "消费提醒",
        "付款结果",
        "扣款成功",
        "支付凭证"
    )
    private val ALIPAY_EXCLUDE_KEYWORDS = listOf(
        "成功收款",
        "收款到账",
        "到账提醒",
        "退款",
        "退款成功",
        "已收钱",
        "免费提现",
        // 优惠券/红包/营销类
        "优惠券",
        "红包",
        "过期",
        "即将过期",
        "到期",
        "领取",
        "活动",
        "奖励",
        "返现",
        "余额",
        "收益",
        "理财",
        "充值",
        "福利",
        "赠券",
        "抵扣券",
        "现金券",
        "体验金",
        "提现",
        "转入",
        "转出",
        "打赏",
        "收款码",
        "群收款",
        "提醒你还",
        "结余",
        "攒钱",
        "花呗额度",
        "借呗",
        "额度提升",
        "邀请",
        "抽奖",
        "会员"
    )
    private val ALIPAY_AMOUNT_PATTERNS = listOf(
        Regex("""一笔\s*(\d+(?:\.\d{1,2})?)\s*元(?:的)?支出"""),
        Regex("""(\d+(?:\.\d{1,2})?)\s*元(?:的)?支出"""),
        Regex("""(?:支出|消费|付款|支付|扣款)\s*(\d+(?:\.\d{1,2})?)\s*元"""),
        Regex("""[¥￥]\s*(\d+(?:\.\d{1,2})?)"""),
        Regex("""(?:支出|消费|付款|支付|扣款).*?([0-9]+(?:\.[0-9]{1,2})?)\s*元""")
    )
    private val ALIPAY_MERCHANT_PATTERNS = listOf(
        Regex("""你在(.+?)(?:消费|付款|支付)"""),
        Regex("""向(.+?)(?:付款|支付)"""),
        Regex("""付款给(.+?)(?:\s|$)"""),
        Regex("""收款方[:：]\s*([^\s]+(?:\s*[^\s，。]+)*)"""),
        Regex("""商户[:：]\s*([^\s]+(?:\s*[^\s，。]+)*)""")
    )

    fun tryParse(
        packageName: String,
        title: String,
        text: String,
        timestamp: Long,
        notificationKey: String
    ): ParsedPayment? {
        val source = PaymentSource.entries.find { it.packageName == packageName } ?: return null
        val fullText = normalizeText("$title $text")
        if (fullText.isBlank()) return null

        return when (source) {
            PaymentSource.WECHAT -> parseWechat(fullText, timestamp, notificationKey)
            PaymentSource.ALIPAY -> parseAlipay(fullText, timestamp, notificationKey)
        }
    }

    private fun parseWechat(
        fullText: String,
        timestamp: Long,
        notificationKey: String
    ): ParsedPayment? {
        if (!containsAny(fullText, WECHAT_TRIGGER_KEYWORDS)) return null
        if (containsAny(fullText, WECHAT_EXCLUDE_KEYWORDS)) return null

        val amount = extractAmount(fullText, WECHAT_AMOUNT_PATTERNS) ?: return null
        val merchant = extractMerchant(fullText, WECHAT_MERCHANT_PATTERNS)

        return ParsedPayment(
            source = PaymentSource.WECHAT,
            amount = amount,
            merchant = merchant,
            timestamp = timestamp,
            notificationKey = notificationKey
        )
    }

    private fun parseAlipay(
        fullText: String,
        timestamp: Long,
        notificationKey: String
    ): ParsedPayment? {
        if (!containsAny(fullText, ALIPAY_TRIGGER_KEYWORDS)) return null
        if (containsAny(fullText, ALIPAY_EXCLUDE_KEYWORDS)) return null

        val amount = extractAmount(fullText, ALIPAY_AMOUNT_PATTERNS) ?: return null
        val merchant = extractMerchant(fullText, ALIPAY_MERCHANT_PATTERNS)

        return ParsedPayment(
            source = PaymentSource.ALIPAY,
            amount = amount,
            merchant = merchant,
            timestamp = timestamp,
            notificationKey = notificationKey
        )
    }

    private fun containsAny(text: String, keywords: List<String>): Boolean =
        keywords.any { keyword -> text.contains(keyword) }

    private fun extractAmount(text: String, patterns: List<Regex>): String? {
        val raw = patterns.firstNotNullOfOrNull { pattern ->
            pattern.find(text)?.groupValues?.getOrNull(1)
        }?.trim() ?: return null
        return raw.takeIf { value -> value.toDoubleOrNull()?.let { it > 0 } == true }
    }

    private fun extractMerchant(text: String, patterns: List<Regex>): String {
        val raw = patterns.firstNotNullOfOrNull { pattern ->
            pattern.find(text)?.groupValues?.getOrNull(1)
        } ?: return ""
        return sanitizeMerchant(raw)
    }

    private fun sanitizeMerchant(raw: String): String {
        return raw
            .trim()
            .trim(':', '：', '，', '。', ',', '.', ' ')
            .substringBefore(" 点击")
            .substringBefore("，")
            .substringBefore("。")
            .trim()
    }

    private fun normalizeText(raw: String): String {
        return raw
            .replace("\n", " ")
            .replace("\r", " ")
            .replace("\t", " ")
            .replace("，", " ")
            .replace("。", " ")
            .replace("；", " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    fun isNotificationListenerEnabled(context: Context): Boolean {
        val cn = ComponentName(context, PaymentNotificationListenerService::class.java)
        val flat = Settings.Secure.getString(
            context.contentResolver, "enabled_notification_listeners"
        )
        return flat?.contains(cn.flattenToString()) == true
    }
}