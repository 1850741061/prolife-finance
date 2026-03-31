package com.prolife.finance.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prolife.finance.notification.ParsedPayment
import com.prolife.finance.ui.theme.LedgerBloomTokens

@Composable
fun AutoBookkeepBanner(
    payments: List<ParsedPayment>,
    onBook: (ParsedPayment) -> Unit,
    onDismiss: (ParsedPayment) -> Unit,
    onDismissAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (payments.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = LedgerBloomTokens.palette.expense.copy(alpha = 0.06f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = LedgerBloomTokens.palette.expense.copy(alpha = 0.15f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Payments,
                        contentDescription = null,
                        tint = LedgerBloomTokens.palette.expense,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (payments.size == 1) "\u5f85\u8bb0\u8d26" else "${payments.size} \u7b14\u5f85\u8bb0\u8d26",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = LedgerBloomTokens.palette.expense
                    )
                }
                if (payments.size > 1) {
                    TextButton(onClick = onDismissAll) {
                        Text("\u5168\u90e8\u5ffd\u7565", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 每笔待记账占一行
            payments.forEach { payment ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${payment.source.label} \u00A5${payment.amount}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (payment.merchant.isNotBlank()) {
                            Text(
                                text = payment.merchant,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    FilledTonalButton(
                        onClick = { onBook(payment) },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("\u8bb0\u8d26", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { onDismiss(payment) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "\u5ffd\u7565",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
