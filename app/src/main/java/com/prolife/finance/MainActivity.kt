package com.prolife.finance

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prolife.finance.data.FinanceCache
import com.prolife.finance.data.FinanceRepository
import com.prolife.finance.data.SessionStore
import com.prolife.finance.notification.ParsedPayment
import com.prolife.finance.notification.PaymentNotificationListenerService
import com.prolife.finance.notification.PendingPaymentStore
import com.prolife.finance.notification.toParsedPaymentOrNull
import com.prolife.finance.ui.AppAction
import com.prolife.finance.ui.AppViewModel
import com.prolife.finance.ui.LedgerBloomRoot
import com.prolife.finance.ui.theme.LedgerBloomTheme

class MainActivity : ComponentActivity() {

    private var paymentReceiver: BroadcastReceiver? = null
    private var viewModelRef: AppViewModel? = null
    private var pendingLaunchPayment by mutableStateOf<ParsedPayment?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        pendingLaunchPayment = intent.toParsedPaymentOrNull()

        val paymentStore = PendingPaymentStore(applicationContext)
        val repository = FinanceRepository(
            sessionStore = SessionStore(applicationContext),
            financeCache = FinanceCache(applicationContext)
        )

        setContent {
            val viewModel: AppViewModel = viewModel(
                factory = AppViewModel.factory(repository, paymentStore, application)
            )
            viewModelRef = viewModel
            val launchPayment = pendingLaunchPayment
            LaunchedEffect(launchPayment?.notificationKey) {
                launchPayment?.let {
                    viewModel.onAction(AppAction.OpenEditorFromPayment(it))
                    pendingLaunchPayment = null
                }
            }
            val state = viewModel.uiState
            LedgerBloomTheme(preference = state.themePreference) {
                LedgerBloomRoot(
                    state = state,
                    onAction = viewModel::onAction
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        isVisible = true
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val payment = intent.toParsedPaymentOrNull()
                if (payment != null) {
                    pendingLaunchPayment = payment
                } else {
                    viewModelRef?.onAction(AppAction.CheckPendingPayments)
                }
            }
        }
        paymentReceiver = receiver
        registerReceiver(
            receiver,
            IntentFilter(PaymentNotificationListenerService.ACTION_PAYMENT_DETECTED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onResume() {
        super.onResume()
        viewModelRef?.onAction(AppAction.CheckPendingPayments)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.toParsedPaymentOrNull()?.let { pendingLaunchPayment = it }
    }

    override fun onStop() {
        super.onStop()
        isVisible = false
        paymentReceiver?.let { unregisterReceiver(it) }
        paymentReceiver = null
    }

    companion object {
        const val ACTION_OPEN_AUTO_BOOKKEEP = "com.prolife.finance.action.OPEN_AUTO_BOOKKEEP"

        @Volatile
        var isVisible: Boolean = false
            private set
    }
}
