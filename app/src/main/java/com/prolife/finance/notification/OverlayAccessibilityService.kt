package com.prolife.finance.notification

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
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

class OverlayAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var overlayView: View? = null
    private var overlayLifecycleOwner: ServiceLifecycleOwner? = null

    override fun onServiceConnected() {
        instance = this
        Log.d(TAG, "\u65e0\u969c\u788d\u60ac\u6d6e\u7a97\u670d\u52a1\u5df2\u8fde\u63a5")
    }

    override fun onDestroy() {
        dismissOverlay()
        instance = null
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    fun showOverlay(payment: ParsedPayment) {
        dismissOverlay()

        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val lifecycleOwner = ServiceLifecycleOwner()
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
                        saveTransaction(payment, categoryName, note, finalAmount)
                        dismissOverlay()
                    },
                    onDismiss = { dismissOverlay() },
                    onIgnore = {
                        PendingPaymentStore(applicationContext).removeByNotificationKey(payment.notificationKey)
                        dismissOverlay()
                    }
                )
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        @Suppress("DEPRECATION")
        params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE

        windowManager.addView(composeView, params)
        overlayView = composeView

        composeView.post { lifecycleOwner.onResume() }

        Log.d(TAG, "\u65e0\u969c\u788d\u60ac\u6d6e\u7a97\u5df2\u663e\u793a: ${payment.source.label} \u00A5${payment.amount}")
    }

    fun dismissOverlay() {
        overlayView?.let { view ->
            try {
                val wm = getSystemService(WINDOW_SERVICE) as WindowManager
                wm.removeView(view)
            } catch (_: Exception) {}
        }
        overlayView = null
        overlayLifecycleOwner?.onDestroy()
        overlayLifecycleOwner = null
    }

    private fun saveTransaction(payment: ParsedPayment, categoryName: String, note: String, finalAmount: Double) {
        val repository = FinanceRepository(
            sessionStore = SessionStore(applicationContext),
            financeCache = FinanceCache(applicationContext)
        )
        val paymentStore = PendingPaymentStore(applicationContext)

        val category = categoriesFor(TransactionType.EXPENSE)
            .firstOrNull { it.name == categoryName }
            ?: categoriesFor(TransactionType.EXPENSE).first()

        val date = Instant.ofEpochMilli(payment.timestamp)
            .atZone(ZoneId.systemDefault()).toLocalDate()

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
            .sortedWith(compareByDescending<TransactionRecord> { it.date }.thenByDescending { it.updatedAt })
        val newSnapshot = FinanceSnapshot(
            transactions = updatedTransactions,
            milktea = snapshot.milktea,
            coffee = snapshot.coffee
        )
        repository.cacheSnapshot(newSnapshot)
        paymentStore.removeByNotificationKey(payment.notificationKey)

        Log.d(TAG, "\u5feb\u6377\u8bb0\u8d26\u5df2\u4fdd\u5b58: ${category.name} \u00A5${payment.amount}")
    }

    private class ServiceLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
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
        private const val TAG = "OverlayA11y"
        var instance: OverlayAccessibilityService? = null
            private set

        fun isEnabled(context: Context): Boolean {
            val serviceName = "${context.packageName}/${OverlayAccessibilityService::class.java.canonicalName}"
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabled.contains(serviceName)
        }
    }
}
