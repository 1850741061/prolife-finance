package com.prolife.finance.notification

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prolife.finance.data.FinanceCache
import com.prolife.finance.data.FinanceRepository
import com.prolife.finance.data.SessionStore
import com.prolife.finance.model.FinanceSnapshot
import com.prolife.finance.model.TransactionRecord
import com.prolife.finance.model.TransactionType
import com.prolife.finance.model.categoriesFor
import com.prolife.finance.model.normalizedForLegacyDesktop
import com.prolife.finance.ui.theme.LedgerBloomTheme
import java.time.Instant
import java.time.ZoneId

class QuickRecordActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

        val payment = intent.toParsedPaymentOrNull()
        if (payment == null) {
            finish()
            return
        }

        val repository = FinanceRepository(
            sessionStore = SessionStore(applicationContext),
            financeCache = FinanceCache(applicationContext)
        )
        val paymentStore = PendingPaymentStore(applicationContext)

        val defaultNote = if (payment.merchant.isNotBlank())
            "${payment.source.label} - ${payment.merchant}"
        else
            payment.source.label

        setContent {
            LedgerBloomTheme(preference = com.prolife.finance.model.ThemePreference()) {
                QuickRecordOverlay(
                    payment = payment,
                    defaultNote = defaultNote,
                    onCategorySelected = { categoryName, note, finalAmount ->
                        saveTransaction(repository, paymentStore, payment, categoryName, note, finalAmount)
                        finish()
                    },
                    onDismiss = { finish() },
                    onIgnore = {
                        paymentStore.removeByNotificationKey(payment.notificationKey)
                        finish()
                    }
                )
            }
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

    private fun saveTransaction(
        repository: FinanceRepository,
        paymentStore: PendingPaymentStore,
        payment: ParsedPayment,
        categoryName: String,
        note: String,
        finalAmount: Double
    ) {
        val category = categoriesFor(TransactionType.EXPENSE, this)
            .firstOrNull { it.name == categoryName }
            ?: categoriesFor(TransactionType.EXPENSE, this).first()

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
    }
}

@Composable
internal fun QuickRecordOverlay(
    payment: ParsedPayment,
    defaultNote: String,
    onCategorySelected: (String, String, Double) -> Unit,
    onDismiss: () -> Unit,
    onIgnore: () -> Unit = onDismiss
) {
    var visible by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf(defaultNote) }

    val ctx = LocalContext.current
    val aaEnabled = remember { SessionStore(ctx).loadAASplitEnabled() }
    val aaThreshold = remember { SessionStore(ctx).loadAASplitThreshold() }
    val totalAmount = remember { payment.amount.toDoubleOrNull() ?: 0.0 }
    val showAASection = aaEnabled && totalAmount >= aaThreshold
    var isAA by remember { mutableStateOf(false) }
    var totalPeople by remember { mutableStateOf(2) }
    var payingPeople by remember { mutableStateOf(1) }

    visible = true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 24.dp,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = parseColor(payment.source.accentColor)
                                    .copy(alpha = 0.12f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = payment.source.label.first().toString(),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = parseColor(payment.source.accentColor)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "${payment.source.label} \u00A5${payment.amount}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (payment.merchant.isNotBlank()) {
                                    Text(
                                        text = payment.merchant,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        TextButton(onClick = onDismiss) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (showAASection) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isAA = !isAA },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "\u8fd9\u662fAA\u9879\u76ee\u5417\uff1f",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isAA) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    ) {
                                        Text(
                                            text = if (isAA) "\u662f" else "\u5426",
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isAA) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (isAA) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "\u603b\u4eba\u6570",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            androidx.compose.material3.IconButton(
                                                onClick = { if (totalPeople > 2) totalPeople-- },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Text("-", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                            }
                                            Text(
                                                text = "$totalPeople \u4eba",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.width(48.dp),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                            androidx.compose.material3.IconButton(
                                                onClick = { if (totalPeople < 20) totalPeople++ },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Text("+", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "\u5165\u8d26\u4eba\u6570",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            androidx.compose.material3.IconButton(
                                                onClick = {
                                                    if (payingPeople > 1) payingPeople--
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Text("-", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                            }
                                            Text(
                                                text = "$payingPeople \u4eba",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.width(48.dp),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                            androidx.compose.material3.IconButton(
                                                onClick = {
                                                    if (payingPeople < totalPeople - 1) payingPeople++
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Text("+", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                            }
                                        }
                                    }

                                    val perPerson = totalAmount / totalPeople
                                    val recordedAmount = perPerson * payingPeople
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surface
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    text = "\u4eba\u5747",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "\u00A5${"%.2f".format(perPerson)}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                                                Text(
                                                    text = "\u5165\u8d26",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "\u00A5${"%.2f".format(recordedAmount)}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = parseColor(payment.source.accentColor)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("\u5907\u6ce8") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = parseColor(payment.source.accentColor),
                            focusedLabelColor = parseColor(payment.source.accentColor)
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "\u9009\u62e9\u5206\u7c7b\u5feb\u901f\u8bb0\u8d26",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val categories = remember { categoriesFor(TransactionType.EXPENSE, ctx) }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        categories.chunked(4).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                row.forEach { cat ->
                                    val tint = parseColor(cat.colorHex)
                                    CategoryChip(
                                        name = cat.name,
                                        icon = quickCategoryIcon(cat.iconName),
                                        tint = tint,
                                        onClick = {
                                            val finalAmount = if (isAA && showAASection) {
                                                totalAmount / totalPeople * payingPeople
                                            } else {
                                                totalAmount
                                            }
                                            val aaNote = if (isAA && showAASection) {
                                                val perPerson = totalAmount / totalPeople
                                                "$note [AA: ${totalPeople}\u4eba, \u4eba\u5747\u00A5${"%.2f".format(perPerson)}]"
                                            } else {
                                                note
                                            }
                                            onCategorySelected(cat.name, aaNote, finalAmount)
                                        }
                                    )
                                }
                                repeat(4 - row.size) {
                                    Spacer(modifier = Modifier.width(72.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 忽略按钮
                    TextButton(
                        onClick = onIgnore,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "忽略本次支付",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    name: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = tint.copy(alpha = 0.12f),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = name,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}

private fun quickCategoryIcon(iconName: String): ImageVector = when (iconName) {
    "utensils" -> Icons.Rounded.Restaurant
    "car" -> Icons.Rounded.DirectionsCar
    "bag" -> Icons.Rounded.ShoppingBag
    "gamepad" -> Icons.Rounded.SportsEsports
    "home" -> Icons.Rounded.Home
    "heart" -> Icons.Rounded.Favorite
    "school" -> Icons.Rounded.School
    "plane" -> Icons.Rounded.Flight
    "gift" -> Icons.Rounded.Redeem
    "phone" -> Icons.Rounded.Smartphone
    "mug-hot" -> Icons.Rounded.LocalDrink
    "coffee" -> Icons.Rounded.LocalCafe
    "salary" -> Icons.Rounded.Payments
    "award" -> Icons.Rounded.EmojiEvents
    "briefcase" -> Icons.Rounded.BusinessCenter
    "chart" -> Icons.AutoMirrored.Rounded.ShowChart
    "more" -> Icons.Rounded.MoreHoriz
    else -> Icons.AutoMirrored.Rounded.Label
}

private fun parseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        Color(0xFF6B7280.toInt())
    }
}

private val PaymentSource.accentColor: String
    get() = when (this) {
        PaymentSource.ALIPAY -> "#1677FF"
        PaymentSource.WECHAT -> "#07C160"
    }
