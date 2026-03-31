package com.prolife.finance.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.prolife.finance.MainActivity
import com.prolife.finance.R
import com.prolife.finance.data.FinanceCache
import com.prolife.finance.data.FinanceRepository
import com.prolife.finance.data.SessionStore
import com.prolife.finance.model.FinanceSnapshot
import com.prolife.finance.model.ThemePreference
import com.prolife.finance.model.TransactionRecord
import com.prolife.finance.model.TransactionType
import com.prolife.finance.model.categoriesFor
import com.prolife.finance.model.normalizedForLegacyDesktop
import com.prolife.finance.ui.theme.LedgerBloomTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.time.Instant
import java.time.ZoneId

class PaymentNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var overlayView: View? = null
    private var overlayLifecycleOwner: OverlayLifecycleOwner? = null

    override fun onListenerConnected() {
        super.onListenerConnected()
        createNotificationChannel()
        if (SessionStore(applicationContext).loadAutoBookkeepEnabled()) {
            BookkeepKeepAliveService.start(applicationContext)
        }
        Log.d(TAG, "onListenerConnected: \u901a\u77e5\u76d1\u542c\u670d\u52a1\u5df2\u542f\u52a8")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "onListenerDisconnected: \u901a\u77e5\u76d1\u542c\u670d\u52a1\u5df2\u65ad\u5f00")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return

        if (!SessionStore(applicationContext).loadAutoBookkeepEnabled()) {
            return
        }
        if (sbn.packageName !in PaymentNotificationParser.TARGET_PACKAGES) {
            return
        }

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence("android.title")?.toString().orEmpty()
        val text = extras.getCharSequence("android.text")?.toString().orEmpty()
        val bigText = extras.getCharSequence("android.bigText")?.toString().orEmpty()
        val postTime = sbn.postTime.takeIf { it > 0L } ?: System.currentTimeMillis()
        val notificationKey = sbn.key?.takeIf { it.isNotBlank() }
            ?: "${sbn.packageName}_${postTime}_${title.hashCode()}_${text.hashCode()}"

        val combinedText = bigText.ifBlank { text }
        val payment = PaymentNotificationParser.tryParse(
            packageName = sbn.packageName,
            title = title,
            text = combinedText,
            timestamp = postTime,
            notificationKey = notificationKey
        ) ?: return

        val store = PendingPaymentStore(applicationContext)
        val existingKeys = store.loadAll().map { it.notificationKey }.toSet()
        store.enqueue(payment)

        if (payment.notificationKey !in existingKeys) {
            var overlayMethod = "none"
            try {
                val intent = Intent(applicationContext, QuickRecordActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putParsedPayment(payment)
                applicationContext.startActivity(intent)
                overlayMethod = "activity"
            } catch (e: Exception) {
                Log.w(TAG, "\u76f4\u63a5\u542f\u52a8 Activity \u5931\u8d25: ${e.message}")
            }

            if (overlayMethod == "none") {
                val a11y = OverlayAccessibilityService.instance
                if (a11y != null) {
                    try {
                        a11y.showOverlay(payment)
                        overlayMethod = "a11y"
                    } catch (e: Exception) {
                        Log.w(TAG, "\u65e0\u969c\u788d\u60ac\u6d6e\u7a97\u5f02\u5e38: ${e.message}")
                    }
                }
            }

            if (overlayMethod == "none") {
                tryShowQuickOverlay(payment)
                overlayMethod = "overlay"
            }

            showPaymentNotification(payment, overlayMethod)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        dismissOverlay()
    }

    private fun tryShowQuickOverlay(payment: ParsedPayment) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(applicationContext)) return
        }
        try {
            showComposeOverlay(payment)
        } catch (e: Exception) {
            Log.w(TAG, "\u60ac\u6d6e\u7a97\u521b\u5efa\u5931\u8d25: ${e.message}")
        }
    }

    private fun showComposeOverlay(payment: ParsedPayment) {
        dismissOverlay()
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val lifecycleOwner = OverlayLifecycleOwner()
        lifecycleOwner.onCreate()
        overlayLifecycleOwner = lifecycleOwner

        val defaultNote = if (payment.merchant.isNotBlank())
            "${payment.source.label} - ${payment.merchant}"
        else
            payment.source.label

        val context = ContextThemeWrapper(applicationContext, R.style.Theme_LedgerBloom_QuickRecord)
        val composeView = ComposeView(context)
        composeView.setViewTreeLifecycleOwner(lifecycleOwner)
        composeView.setViewTreeSavedStateRegistryOwner(lifecycleOwner)

        composeView.setContent {
            LedgerBloomTheme(preference = ThemePreference()) {
                QuickRecordOverlay(
                    payment = payment,
                    defaultNote = defaultNote,
                    onCategorySelected = { categoryName, note, finalAmount ->
                        saveTransactionFromOverlay(payment, categoryName, note, finalAmount)
                        dismissOverlay()
                    },
                    onDismiss = { dismissOverlay() }
                )
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        @Suppress("DEPRECATION")
        params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE

        windowManager.addView(composeView, params)
        overlayView = composeView
        composeView.post { lifecycleOwner.onResume() }
    }

    private fun dismissOverlay() {
        overlayView?.let { view ->
            try {
                val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
                windowManager.removeView(view)
            } catch (_: Exception) {}
        }
        overlayView = null
        overlayLifecycleOwner?.onDestroy()
        overlayLifecycleOwner = null
    }

    private fun saveTransactionFromOverlay(
        payment: ParsedPayment,
        categoryName: String,
        note: String,
        finalAmount: Double
    ) {
        val repository = FinanceRepository(
            sessionStore = SessionStore(applicationContext),
            financeCache = FinanceCache(applicationContext)
        )
        val paymentStore = PendingPaymentStore(applicationContext)

        val category = categoriesFor(TransactionType.EXPENSE, applicationContext)
            .firstOrNull { it.name == categoryName }
            ?: categoriesFor(TransactionType.EXPENSE, applicationContext).first()

        val date = Instant.ofEpochMilli(payment.timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        val transaction = TransactionRecord(
            id = System.currentTimeMillis().toString(),
            type = TransactionType.EXPENSE,
            date = date,
            category = category.name,
            amount = finalAmount,
            note = note,
            colorHex = category.colorHex,
            updatedAt = Instant.now(),
            createdAt = Instant.now()
        ).normalizedForLegacyDesktop()

        val snapshot = repository.loadCachedSnapshot()
        val updatedTransactions = (snapshot.transactions + transaction)
            .map { it.normalizedForLegacyDesktop() }
            .sortedWith(
                compareByDescending<TransactionRecord> { it.date }
                    .thenByDescending { it.updatedAt })
        val newSnapshot = FinanceSnapshot(
            transactions = updatedTransactions,
            milktea = snapshot.milktea,
            coffee = snapshot.coffee
        )
        repository.cacheSnapshot(newSnapshot)
        paymentStore.removeByNotificationKey(payment.notificationKey)

        Log.d(TAG, "\u5feb\u6377\u8bb0\u8d26\u5df2\u4fdd\u5b58: ${category.name} \u00A5${payment.amount}")
    }

    private fun showPaymentNotification(payment: ParsedPayment, method: String = "none") {
        val intent = Intent(applicationContext, QuickRecordActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putParsedPayment(payment)

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            payment.notificationKey.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val titleText = "${payment.source.label} \u00A5${payment.amount}"
        val methodLabel = when (method) {
            "a11y" -> "[\u65e0\u969c\u788d]"
            "activity" -> "[Activity]"
            "overlay" -> "[\u60ac\u6d6e\u7a97]"
            else -> "[\u4ec5\u901a\u77e5]"
        }
        val bodyText = if (payment.merchant.isNotBlank()) {
            "${payment.merchant} $methodLabel \u2014 \u70b9\u51fb\u9009\u62e9\u5206\u7c7b"
        } else {
            "$methodLabel \u70b9\u51fb\u9009\u62e9\u5206\u7c7b\u5feb\u901f\u8bb0\u8d26"
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_bookkeep)
            .setContentTitle(titleText)
            .setContentText(bodyText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$titleText\n$bodyText")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setTimeoutAfter(60_000)
            .build()

        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(payment.notificationKey.hashCode() and 0x7FFFFFFF, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "\u81ea\u52a8\u8bb0\u8d26\u63d0\u9192",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "\u68c0\u6d4b\u5230\u652f\u4ed8\u540e\u63d0\u9192\u8bb0\u8d26"
                enableVibration(true)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateController = SavedStateRegistryController.create(this)
        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

        fun onCreate() {
            savedStateController.performRestore(null)
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
        }
        fun onResume() { lifecycleRegistry.currentState = Lifecycle.State.RESUMED }
        fun onDestroy() { lifecycleRegistry.currentState = Lifecycle.State.DESTROYED }
    }

    companion object {
        private const val TAG = "AutoBookkeep"
        private const val CHANNEL_ID = "auto_bookkeep_reminder"
        const val ACTION_PAYMENT_DETECTED = "com.prolife.finance.ACTION_PAYMENT_DETECTED"
    }
}
