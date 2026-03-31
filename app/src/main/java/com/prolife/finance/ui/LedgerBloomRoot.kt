package com.prolife.finance.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.prolife.finance.notification.OverlayAccessibilityService
import com.prolife.finance.notification.PaymentNotificationParser
import com.prolife.finance.ui.components.AnimatedFloatingActionButton
import com.prolife.finance.ui.theme.LedgerBloomTokens
import android.content.Intent
import android.provider.Settings

@Composable
fun LedgerBloomRoot(
    state: AppUiState,
    onAction: (AppAction) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            onAction(AppAction.ConsumeMessage)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        FinanceShell(state = state, onAction = onAction, snackbarHostState = snackbarHostState)

        state.editor?.let { editor ->
            TransactionEditorDialog(editor = editor, onAction = onAction)
        }
        state.drinkEditor?.let { editor ->
            DrinkEditorDialog(editor = editor, onAction = onAction)
        }
        state.drinkSettingsEditor?.let { editor ->
            DrinkSettingsDialog(editor = editor, onAction = onAction)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinanceShell(
    state: AppUiState,
    onAction: (AppAction) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showAutoBookkeepDialog by remember { mutableStateOf(false) }

    val permissionEnabled = remember {
        mutableStateOf(PaymentNotificationParser.isNotificationListenerEnabled(context))
    }
    val overlayEnabled = remember {
        mutableStateOf(checkOverlayPermission(context))
    }
    val a11yEnabled = remember {
        mutableStateOf(OverlayAccessibilityService.isEnabled(context))
    }
    DisposableEffect(lifecycleOwner, showAutoBookkeepDialog, state.autoBookkeepEnabled) {
        val refreshPermissionState = {
            permissionEnabled.value = PaymentNotificationParser.isNotificationListenerEnabled(context)
            overlayEnabled.value = checkOverlayPermission(context)
            a11yEnabled.value = OverlayAccessibilityService.isEnabled(context)
        }
        refreshPermissionState()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissionState()
                onAction(AppAction.RefreshFromCache)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = tabTitle(state.currentTab),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when (state.currentTab) {
                                BottomDestination.DASHBOARD -> "\u7efc\u5408\u8d22\u52a1\u6982\u89c8"
                                BottomDestination.DETAIL -> state.selectedMonth.format(MonthFormatter)
                                BottomDestination.STATS -> formatStatsLabel(state.statsGranularity, state.statsAnchorDate)
                                BottomDestination.BILLS -> "\u6309\u5e74\u6216\u6309\u6708\u8df3\u8f6c\u7edf\u8ba1"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAutoBookkeepDialog = true }
                    ) {
                        Icon(
                            Icons.Rounded.Notifications,
                            contentDescription = "\u81ea\u52a8\u8bb0\u8d26",
                            tint = if (state.autoBookkeepEnabled && permissionEnabled.value)
                                LedgerBloomTokens.palette.income
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                shape = RoundedCornerShape(28.dp),
                tonalElevation = 2.dp,
                shadowElevation = 12.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                NavigationBar(containerColor = Color.Transparent) {
                    val destinations = listOf(
                        BottomDestination.DASHBOARD to Icons.Rounded.Home,
                        BottomDestination.DETAIL to Icons.AutoMirrored.Rounded.ReceiptLong,
                        BottomDestination.STATS to Icons.AutoMirrored.Rounded.ShowChart,
                        BottomDestination.BILLS to Icons.Rounded.CalendarMonth
                    )
                    destinations.forEach { (tab, icon) ->
                        NavigationBarItem(
                            selected = state.currentTab == tab,
                            onClick = { onAction(AppAction.SwitchTab(tab)) },
                            icon = { Icon(icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (state.currentTab == BottomDestination.DETAIL || state.currentTab == BottomDestination.DASHBOARD) {
                AnimatedFloatingActionButton(
                    onClick = { onAction(AppAction.OpenCreateExpense) },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("\u8bb0\u4e00\u7b14", style = MaterialTheme.typography.labelLarge) }
                )
            }
        }
    ) { innerPadding ->
        when (state.currentTab) {
            BottomDestination.DASHBOARD -> DashboardScreen(state = state, onAction = onAction, modifier = Modifier.padding(innerPadding))
            BottomDestination.DETAIL -> DetailScreen(state = state, onAction = onAction, modifier = Modifier.padding(innerPadding))
            BottomDestination.STATS -> StatsScreen(state = state, onAction = onAction, modifier = Modifier.padding(innerPadding))
            BottomDestination.BILLS -> BillsScreen(transactions = state.transactions, onAction = onAction, modifier = Modifier.padding(innerPadding))
        }
    }

    if (showAutoBookkeepDialog) {
        AutoBookkeepPermissionDialog(
            enabled = permissionEnabled.value,
            overlayEnabled = overlayEnabled.value,
            a11yEnabled = a11yEnabled.value,
            autoBookkeepOn = state.autoBookkeepEnabled,
            aaSplitEnabled = state.aaSplitEnabled,
            aaSplitThreshold = state.aaSplitThreshold,
            onToggle = { onAction(AppAction.ToggleAutoBookkeep) },
            onToggleAASplit = { onAction(AppAction.ToggleAASplit) },
            onSetAASplitThreshold = { onAction(AppAction.SetAASplitThreshold(it)) },
            onOpenSettings = {
                context.startActivity(
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                )
                showAutoBookkeepDialog = false
            },
            onOpenOverlaySettings = {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:${context.packageName}")
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
                showAutoBookkeepDialog = false
            },
            onOpenA11ySettings = {
                context.startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                )
                showAutoBookkeepDialog = false
            },
            onDismiss = { showAutoBookkeepDialog = false }
        )
    }
}

@Composable
private fun AutoBookkeepPermissionDialog(
    enabled: Boolean,
    overlayEnabled: Boolean,
    a11yEnabled: Boolean,
    autoBookkeepOn: Boolean,
    aaSplitEnabled: Boolean,
    aaSplitThreshold: Float,
    onToggle: () -> Unit,
    onToggleAASplit: () -> Unit,
    onSetAASplitThreshold: (Float) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenA11ySettings: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            when {
                !enabled -> {
                    androidx.compose.material3.FilledTonalButton(onClick = onOpenSettings) {
                        Text("\u53bb\u6388\u6743", fontWeight = FontWeight.SemiBold)
                    }
                }
                !overlayEnabled -> {
                    androidx.compose.material3.FilledTonalButton(onClick = onOpenOverlaySettings) {
                        Text("\u6388\u6743\u60ac\u6d6e\u7a97", fontWeight = FontWeight.SemiBold)
                    }
                }
                !a11yEnabled -> {
                    androidx.compose.material3.FilledTonalButton(onClick = onOpenA11ySettings) {
                        Text("\u5f00\u542f\u65e0\u969c\u788d\u670d\u52a1", fontWeight = FontWeight.SemiBold)
                    }
                }
                !autoBookkeepOn -> {
                    androidx.compose.material3.FilledTonalButton(onClick = {
                        onToggle()
                        onDismiss()
                    }) {
                        Text("\u5f00\u542f\u81ea\u52a8\u8bb0\u8d26", fontWeight = FontWeight.SemiBold)
                    }
                }
                else -> {
                    androidx.compose.material3.FilledTonalButton(onClick = onDismiss) {
                        Text("\u786e\u5b9a", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        dismissButton = {
            if (autoBookkeepOn) {
                androidx.compose.material3.TextButton(onClick = {
                    onToggle()
                    onDismiss()
                }) {
                    Text("\u5173\u95ed\u81ea\u52a8\u8bb0\u8d26")
                }
            } else {
                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    Text("\u53d6\u6d88")
                }
            }
        },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "\u81ea\u52a8\u8bb0\u8d26",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when {
                        !enabled -> "\u901a\u77e5\u76d1\u542c\u672a\u6388\u6743"
                        !overlayEnabled -> "\u60ac\u6d6e\u7a97\u672a\u6388\u6743"
                        !a11yEnabled -> "\u65e0\u969c\u788d\u670d\u52a1\u672a\u5f00\u542f"
                        autoBookkeepOn -> "\u5df2\u5f00\u542f"
                        else -> "\u5df2\u6388\u6743\uff0c\u672a\u5f00\u542f"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = when {
                        !enabled || !overlayEnabled || !a11yEnabled -> MaterialTheme.colorScheme.error
                        autoBookkeepOn -> LedgerBloomTokens.palette.income
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "\u76d1\u542c\u5fae\u4fe1\u548c\u652f\u4ed8\u5b9d\u7684\u652f\u4ed8\u901a\u77e5\uff0c\u652f\u4ed8\u5b8c\u6bd5\u540e\u7acb\u5373\u5f39\u51fa\u5206\u7c7b\u9009\u62e9\u7a97\u53e3\uff0c\u4e00\u952e\u5b8c\u6210\u8bb0\u8d26\u3002",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = when {
                        !enabled -> "\u9700\u8981\u5148\u6253\u5f00\u901a\u77e5\u76d1\u542c\u6743\u9650\uff0c\u6388\u6743\u540e\u624d\u80fd\u63a5\u6536\u5fae\u4fe1\u6216\u652f\u4ed8\u5b9d\u7684\u4ed8\u6b3e\u901a\u77e5\u3002"
                        !overlayEnabled -> "\u9700\u8981\u6388\u6743\u60ac\u6d6e\u7a97\u6743\u9650\uff0c\u652f\u4ed8\u5b8c\u540e\u624d\u80fd\u5728\u5176\u4ed6 App \u4e0a\u65b9\u5f39\u51fa\u5feb\u6377\u8bb0\u8d26\u7a97\u53e3\u3002"
                        !a11yEnabled -> "\u9700\u8981\u5f00\u542f\u65e0\u969c\u788d\u670d\u52a1\uff0c\u8fd9\u6837\u5728\u56fd\u4ea7 ROM \u4e0a\u4e5f\u80fd\u53ef\u9760\u5730\u5728\u540e\u53f0\u5f39\u51fa\u60ac\u6d6e\u7a97\u3002"
                        autoBookkeepOn -> "\u529f\u80fd\u5df2\u5f00\u542f\uff0c\u652f\u4ed8\u5b8c\u6bd5\u540e\u4f1a\u81ea\u52a8\u5f39\u51fa\u5206\u7c7b\u9009\u62e9\u7a97\u53e3\u3002"
                        else -> "\u6240\u6709\u6743\u9650\u5df2\u5c31\u7eea\uff0c\u73b0\u5728\u53ef\u4ee5\u5f00\u542f\u81ea\u52a8\u8bb0\u8d26\u3002"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (autoBookkeepOn) {
                    androidx.compose.material3.HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AA\u5206\u8d26\u63d0\u9192",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "\u91d1\u989d\u8d85\u8fc7\u9608\u503c\u65f6\u8be2\u95ee\u662f\u5426\u4e3aAA\u9879\u76ee",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        androidx.compose.material3.Switch(
                            checked = aaSplitEnabled,
                            onCheckedChange = { onToggleAASplit() }
                        )
                    }
                    if (aaSplitEnabled) {
                        var thresholdText by remember(aaSplitThreshold) {
                            mutableStateOf(
                                if (aaSplitThreshold == aaSplitThreshold.toInt().toFloat()) {
                                    aaSplitThreshold.toInt().toString()
                                } else {
                                    "%.1f".format(aaSplitThreshold)
                                }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "\u91d1\u989d\u9608\u503c \u00A5",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            androidx.compose.material3.OutlinedTextField(
                                value = thresholdText,
                                onValueChange = { newText ->
                                    thresholdText = newText
                                    newText.toFloatOrNull()?.let { onSetAASplitThreshold(it) }
                                },
                                singleLine = true,
                                modifier = Modifier.width(100.dp),
                                shape = RoundedCornerShape(12.dp),
                                textStyle = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

private fun tabTitle(tab: BottomDestination): String = when (tab) {
    BottomDestination.DASHBOARD -> "\u9996\u9875"
    BottomDestination.DETAIL -> "\u660e\u7ec6"
    BottomDestination.STATS -> "\u7edf\u8ba1"
    BottomDestination.BILLS -> "\u8d26\u5355"
}

private fun checkOverlayPermission(context: android.content.Context): Boolean {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        android.provider.Settings.canDrawOverlays(context)
    } else {
        true
    }
}
