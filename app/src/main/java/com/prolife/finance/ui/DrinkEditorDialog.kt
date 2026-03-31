package com.prolife.finance.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prolife.finance.model.DrinkType
import com.prolife.finance.ui.theme.LedgerBloomTokens

private enum class DrinkNumericField {
    COST,
    CUPS
}

@Composable
fun DrinkEditorDialog(
    editor: DrinkEditorState,
    onAction: (AppAction) -> Unit
) {
    val accent = if (editor.type == DrinkType.MILK_TEA) {
        MaterialTheme.colorScheme.tertiary
    } else {
        LedgerBloomTokens.palette.chartAccent
    }
    val selectedDate = parseEditorDate(editor.date)

    var activeField by remember(editor.recordId, editor.type) { mutableStateOf(DrinkNumericField.COST) }
    var showDatePicker by remember(editor.recordId) { mutableStateOf(false) }
    var showBrandDialog by remember(editor.recordId) { mutableStateOf(false) }
    var showSugarDialog by remember(editor.recordId) { mutableStateOf(false) }
    var showNotesDialog by remember(editor.recordId) { mutableStateOf(false) }

    EntryFullscreenDialog(onDismiss = { onAction(AppAction.DismissDrinkEditor) }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            EntryHeaderBar(
                title = if (editor.recordId == null) "饮品记录" else "编辑饮品记录",
                subtitle = if (editor.type == DrinkType.MILK_TEA) "奶茶快记" else "咖啡快记",
                onClose = { onAction(AppAction.DismissDrinkEditor) }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EntryTypeTabs(
                    options = listOf(DrinkType.MILK_TEA to "奶茶", DrinkType.COFFEE to "咖啡"),
                    selectedIndex = if (editor.type == DrinkType.MILK_TEA) 0 else 1,
                    accent = accent,
                    onSelect = { index ->
                        onAction(
                            AppAction.UpdateDrinkType(
                                if (index == 0) DrinkType.MILK_TEA else DrinkType.COFFEE
                            )
                        )
                    }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    EntryValuePill(
                        label = "花费",
                        value = formatEntryCurrency(editor.cost),
                        selected = activeField == DrinkNumericField.COST,
                        accent = accent,
                        onClick = { activeField = DrinkNumericField.COST },
                        modifier = Modifier.weight(1f)
                    )
                    EntryValuePill(
                        label = "杯数",
                        value = formatEntryCount(editor.cups, "杯"),
                        selected = activeField == DrinkNumericField.CUPS,
                        accent = accent,
                        onClick = { activeField = DrinkNumericField.CUPS },
                        modifier = Modifier.weight(1f)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            DrinkMetaChip(
                                label = "日期",
                                value = selectedDate.toString(),
                                accent = MaterialTheme.colorScheme.primary,
                                onClick = { showDatePicker = true },
                                modifier = Modifier.weight(1f)
                            )
                            DrinkMetaChip(
                                label = "品牌",
                                value = editor.brand.ifBlank { "点击填写品牌" },
                                accent = accent,
                                onClick = { showBrandDialog = true },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        AnimatedVisibility(
                            visible = editor.type == DrinkType.MILK_TEA,
                            enter = fadeIn() + slideInVertically(),
                            exit = fadeOut() + slideOutVertically()
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                DrinkMetaChip(
                                    label = "糖度",
                                    value = editor.sugar.ifBlank { "点击填写糖度" },
                                    accent = MaterialTheme.colorScheme.tertiary,
                                    onClick = { showSugarDialog = true },
                                    modifier = Modifier.weight(1f)
                                )
                                SpacerSurface(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            DrinkComposerBar(
                note = editor.notes,
                displayValue = if (activeField == DrinkNumericField.COST) {
                    formatEntryCurrency(editor.cost)
                } else {
                    formatEntryCount(editor.cups, "杯")
                },
                iconAccent = accent,
                icon = if (editor.type == DrinkType.MILK_TEA) Icons.Rounded.LocalDrink else Icons.Rounded.LocalCafe,
                onNoteClick = { showNotesDialog = true },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            EntryNumberPad(
                accent = accent,
                dateLabel = entryDateButtonLabel(selectedDate),
                onDateClick = { showDatePicker = true },
                onInput = { token ->
                    when (activeField) {
                        DrinkNumericField.COST -> {
                            onAction(
                                AppAction.UpdateDrinkCost(
                                    appendNumericToken(editor.cost, token, maxDecimals = 2)
                                )
                            )
                        }

                        DrinkNumericField.CUPS -> {
                            onAction(
                                AppAction.UpdateDrinkCups(
                                    appendNumericToken(editor.cups, token, maxDecimals = 1)
                                )
                            )
                        }
                    }
                },
                onDelete = {
                    when (activeField) {
                        DrinkNumericField.COST -> onAction(AppAction.UpdateDrinkCost(deleteNumericToken(editor.cost)))
                        DrinkNumericField.CUPS -> onAction(AppAction.UpdateDrinkCups(deleteNumericToken(editor.cups)))
                    }
                },
                onClear = {
                    when (activeField) {
                        DrinkNumericField.COST -> onAction(AppAction.UpdateDrinkCost(""))
                        DrinkNumericField.CUPS -> onAction(AppAction.UpdateDrinkCups(""))
                    }
                },
                onDone = { onAction(AppAction.SaveDrinkEditor) },
                modifier = Modifier.navigationBarsPadding()
            )
        }

        if (showDatePicker) {
            EntryDatePickerDialog(
                initialDate = selectedDate,
                accent = accent,
                onDismiss = { showDatePicker = false },
                onDateSelected = { date ->
                    onAction(AppAction.UpdateDrinkDate(date.toString()))
                    showDatePicker = false
                }
            )
        }

        if (showBrandDialog) {
            EntryTextEditDialog(
                title = "品牌",
                value = editor.brand,
                placeholder = if (editor.type == DrinkType.MILK_TEA) "输入奶茶品牌" else "输入咖啡品牌",
                singleLine = true,
                accent = accent,
                onDismiss = { showBrandDialog = false },
                onConfirm = {
                    onAction(AppAction.UpdateDrinkBrand(it))
                    showBrandDialog = false
                }
            )
        }

        if (showSugarDialog && editor.type == DrinkType.MILK_TEA) {
            EntryTextEditDialog(
                title = "糖度",
                value = editor.sugar,
                placeholder = "例如：全糖、半糖、少糖",
                singleLine = true,
                accent = MaterialTheme.colorScheme.tertiary,
                onDismiss = { showSugarDialog = false },
                onConfirm = {
                    onAction(AppAction.UpdateDrinkSugar(it))
                    showSugarDialog = false
                }
            )
        }

        if (showNotesDialog) {
            EntryTextEditDialog(
                title = "备注",
                value = editor.notes,
                placeholder = "输入门店、联名款或优惠信息",
                singleLine = false,
                accent = MaterialTheme.colorScheme.primary,
                onDismiss = { showNotesDialog = false },
                onConfirm = {
                    onAction(AppAction.UpdateDrinkNotes(it))
                    showNotesDialog = false
                }
            )
        }
    }
}

@Composable
private fun DrinkMetaChip(
    label: String,
    value: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = accent
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SpacerSurface(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {}
}

@Composable
private fun DrinkComposerBar(
    note: String,
    displayValue: String,
    iconAccent: Color,
    icon: ImageVector,
    onNoteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = iconAccent.copy(alpha = 0.12f)
            ) {
                Box(
                    modifier = Modifier.size(46.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onNoteClick),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "备注",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = note.ifBlank { "点击写备注..." },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (note.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = displayValue,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = iconAccent
            )
        }
    }
}

@Composable
fun DrinkSettingsDialog(
    editor: DrinkSettingsEditorState,
    onAction: (AppAction) -> Unit
) {
    AlertDialog(
        onDismissRequest = { onAction(AppAction.DismissDrinkSettings) },
        confirmButton = {
            FilledTonalButton(
                onClick = { onAction(AppAction.SaveDrinkSettings) },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("保存", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = { onAction(AppAction.DismissDrinkSettings) }) {
                Text("取消")
            }
        },
        title = {
            EditorDialogTitle(
                icon = Icons.Rounded.Tune,
                tint = MaterialTheme.colorScheme.primary,
                title = "限额控制",
                subtitle = "按周和按月控制奶茶、咖啡杯数，额度条会实时更新"
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EditorInfoPill(
                        text = "按杯数限制",
                        accent = MaterialTheme.colorScheme.primary
                    )
                    EditorInfoPill(
                        text = "影响周/月额度",
                        accent = MaterialTheme.colorScheme.tertiary
                    )
                }

                EditorSectionCard(
                    title = "奶茶",
                    subtitle = "用于奶茶统计里的本周额度和本月额度"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = editor.milkteaWeeklyLimit,
                            onValueChange = { onAction(AppAction.UpdateMilkteaWeeklyLimit(it)) },
                            label = { Text("周限额") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            colors = editorTextFieldColors(MaterialTheme.colorScheme.tertiary)
                        )
                        OutlinedTextField(
                            value = editor.milkteaMonthlyLimit,
                            onValueChange = { onAction(AppAction.UpdateMilkteaMonthlyLimit(it)) },
                            label = { Text("月限额") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            colors = editorTextFieldColors(MaterialTheme.colorScheme.tertiary)
                        )
                    }
                }

                EditorSectionCard(
                    title = "咖啡",
                    subtitle = "用于咖啡统计里的本周额度和本月额度"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = editor.coffeeWeeklyLimit,
                            onValueChange = { onAction(AppAction.UpdateCoffeeWeeklyLimit(it)) },
                            label = { Text("周限额") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            colors = editorTextFieldColors(LedgerBloomTokens.palette.chartAccent)
                        )
                        OutlinedTextField(
                            value = editor.coffeeMonthlyLimit,
                            onValueChange = { onAction(AppAction.UpdateCoffeeMonthlyLimit(it)) },
                            label = { Text("月限额") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            colors = editorTextFieldColors(LedgerBloomTokens.palette.chartAccent)
                        )
                    }
                }
            }
        },
        shape = RoundedCornerShape(30.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}
