package com.prolife.finance.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prolife.finance.model.FinanceCategory
import com.prolife.finance.model.categoriesFor
import com.prolife.finance.model.saveCategories
import com.prolife.finance.model.TransactionType
import com.prolife.finance.model.CategoryIconOptions
import com.prolife.finance.model.CategoryColorOptions
import com.prolife.finance.ui.theme.LedgerBloomTokens

private sealed interface TransactionCategoryTile {
    data class CategoryItem(val category: FinanceCategory) : TransactionCategoryTile
    data object Settings : TransactionCategoryTile
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionEditorDialog(
    editor: TransactionEditorState,
    onAction: (AppAction) -> Unit
) {
    val accent = if (editor.type == TransactionType.EXPENSE) {
        LedgerBloomTokens.palette.expense
    } else {
        LedgerBloomTokens.palette.income
    }
    val context = LocalContext.current
    var categories by remember(editor.type) {
        mutableStateOf(categoriesFor(editor.type, context))
    }
    val categoryPages = remember(categories) { buildTransactionCategoryPages(categories) }
    val initialPage = remember(categories, editor.category) {
        initialTransactionCategoryPage(categoryPages, editor.category)
    }
    val pagerState = rememberPagerState(
        pageCount = { categoryPages.size },
        initialPage = initialPage
    )
    val scope = rememberCoroutineScope()
    var showSettingsPage by remember(editor.transactionId) { mutableStateOf(false) }
    var showDatePicker by remember(editor.transactionId) { mutableStateOf(false) }
    var showNoteDialog by remember(editor.transactionId) { mutableStateOf(false) }

    val selectedDate = parseEditorDate(editor.date)
    val activeCategory = categories.firstOrNull { it.name == editor.category } ?: categories.first()

    LaunchedEffect(initialPage) {
        pagerState.scrollToPage(initialPage)
    }

    EntryFullscreenDialog(onDismiss = { onAction(AppAction.DismissEditor) }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            EntryHeaderBar(
                title = if (editor.transactionId == null) "记一笔" else "编辑账目",
                subtitle = if (showSettingsPage) {
                    "分类设置"
                } else if (editor.type == TransactionType.EXPENSE) {
                    "支出快记"
                } else {
                    "收入快记"
                },
                onClose = { onAction(AppAction.DismissEditor) },
                navigationIcon = if (showSettingsPage) defaultBackIcon() else null,
                onNavigationClick = if (showSettingsPage) {
                    { showSettingsPage = false }
                } else {
                    null
                }
            )

            if (showSettingsPage) {
                TransactionCategorySettingsPage(
                    currentType = editor.type,
                    categories = categories,
                    accent = accent,
                    onSelectType = { index ->
                        onAction(
                            AppAction.UpdateEditorType(
                                if (index == 0) TransactionType.EXPENSE else TransactionType.INCOME
                            )
                        )
                    },
                    onAddCategory = { category ->
                        onAction(AppAction.AddCategory(category))
                        categories = categoriesFor(editor.type, context)
                    },
                    onEditCategory = { oldName, category ->
                        onAction(AppAction.EditCategory(oldName, category))
                        categories = categoriesFor(editor.type, context)
                    },
                    onDeleteCategory = { name, type ->
                        onAction(AppAction.DeleteCategory(name, type))
                        categories = categoriesFor(editor.type, context)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EntryTypeTabs(
                        options = listOf(TransactionType.EXPENSE to "支出", TransactionType.INCOME to "收入"),
                        selectedIndex = if (editor.type == TransactionType.EXPENSE) 0 else 1,
                        accent = accent,
                        onSelect = { index ->
                            onAction(
                                AppAction.UpdateEditorType(
                                    if (index == 0) TransactionType.EXPENSE else TransactionType.INCOME
                                )
                            )
                        }
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) { page ->
                                TransactionCategoryGrid(
                                    pageItems = categoryPages[page],
                                    selectedCategory = editor.category,
                                    onSelectCategory = { category ->
                                        onAction(AppAction.UpdateEditorCategory(category.name))
                                    },
                                    onOpenSettings = { showSettingsPage = true },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            EntryPageDots(
                                pageCount = categoryPages.size,
                                currentPage = pagerState.currentPage,
                                onSelect = { scope.launch { pagerState.animateScrollToPage(it) } }
                            )
                        }
                    }
                }

                EntryNumberPad(
                    accent = accent,
                    dateLabel = entryDateButtonLabel(selectedDate),
                    onDateClick = { showDatePicker = true },
                    onInput = { token ->
                        onAction(
                            AppAction.UpdateEditorAmount(
                                appendNumericToken(editor.amount, token, maxDecimals = 2)
                            )
                        )
                    },
                    onDelete = { onAction(AppAction.UpdateEditorAmount(deleteNumericToken(editor.amount))) },
                    onClear = { onAction(AppAction.UpdateEditorAmount("")) },
                    onDone = { onAction(AppAction.SaveEditor) },
                    modifier = Modifier.navigationBarsPadding(),
                    topContent = {
                        TransactionComposerBar(
                            note = editor.note,
                            amount = editor.amount,
                            category = activeCategory,
                            accent = accent,
                            onNoteClick = { showNoteDialog = true },
                            embedded = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                )
            }
        }

        if (showDatePicker) {
            EntryDatePickerDialog(
                initialDate = selectedDate,
                accent = accent,
                onDismiss = { showDatePicker = false },
                onDateSelected = { date ->
                    onAction(AppAction.UpdateEditorDate(date.toString()))
                    showDatePicker = false
                }
            )
        }

        if (showNoteDialog) {
            EntryTextEditDialog(
                title = "备注",
                value = editor.note,
                placeholder = "输入备注、门店或场景",
                singleLine = false,
                accent = accent,
                onDismiss = { showNoteDialog = false },
                onConfirm = {
                    onAction(AppAction.UpdateEditorNote(it))
                    showNoteDialog = false
                }
            )
        }

    }
}

@Composable
private fun TransactionCategorySettingsPage(
    currentType: TransactionType,
    categories: List<FinanceCategory>,
    accent: Color,
    onSelectType: (Int) -> Unit,
    onAddCategory: (FinanceCategory) -> Unit,
    onEditCategory: (String, FinanceCategory) -> Unit,
    onDeleteCategory: (String, TransactionType) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<FinanceCategory?>(null) }
    var deleteTarget by remember { mutableStateOf<FinanceCategory?>(null) }

    Column(
        modifier = modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        EditorSectionCard(
            title = "目前共${categories.size}个类别",
            subtitle = "点击切换支出/收入查看不同分类",
            modifier = Modifier.fillMaxWidth()
        ) {
            EntryModeTabs(
                options = listOf("支出", "收入"),
                selectedIndex = if (currentType == TransactionType.EXPENSE) 0 else 1,
                accent = accent,
                onSelect = onSelectType
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(categories, key = { it.name }) { category ->
                EntrySettingRow(
                    icon = categoryIcon(category.iconName),
                    label = category.name,
                    tint = colorFromHex(category.colorHex),
                    onClick = { editingCategory = category },
                    onDelete = {
                        if (categories.size > 1) deleteTarget = category
                    }
                )
            }
            item {
                // 添加分类按钮
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { showAddDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "添加分类",
                            style = MaterialTheme.typography.bodyMedium,
                            color = accent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    // 添加分类对话框
    if (showAddDialog) {
        CategoryEditDialog(
            title = "添加分类",
            initialName = "",
            initialIcon = CategoryIconOptions.first(),
            initialColor = CategoryColorOptions.first(),
            type = currentType,
            onConfirm = { name, icon, color ->
                onAddCategory(FinanceCategory(name, color, icon, currentType))
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    // 编辑分类对话框
    editingCategory?.let { cat ->
        CategoryEditDialog(
            title = "编辑分类",
            initialName = cat.name,
            initialIcon = cat.iconName,
            initialColor = cat.colorHex,
            type = currentType,
            onConfirm = { name, icon, color ->
                onEditCategory(cat.name, FinanceCategory(name, color, icon, currentType))
                editingCategory = null
            },
            onDismiss = { editingCategory = null }
        )
    }

    // 删除确认对话框
    deleteTarget?.let { cat ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除分类") },
            text = { Text("确定要删除「${cat.name}」吗？已有交易不会受影响。") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteCategory(cat.name, cat.type)
                    deleteTarget = null
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun CategoryEditDialog(
    title: String,
    initialName: String,
    initialIcon: String,
    initialColor: String,
    type: TransactionType,
    onConfirm: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedIcon by remember { mutableStateOf(initialIcon) }
    var selectedColor by remember { mutableStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 名称输入
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("分类名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                // 颜色选择
                Text("颜色", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 80.dp)
                ) {
                    items(CategoryColorOptions) { hex ->
                        val selected = hex == selectedColor
                        Surface(
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { selectedColor = hex },
                            shape = CircleShape,
                            color = colorFromHex(hex),
                            border = if (selected) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null
                        ) {}
                    }
                }

                // 图标选择
                Text("图标", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 160.dp)
                ) {
                    items(CategoryIconOptions) { iconName ->
                        val selected = iconName == selectedIcon
                        Surface(
                            modifier = Modifier
                                .size(42.dp)
                                .clickable { selectedIcon = iconName },
                            shape = CircleShape,
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    categoryIcon(iconName),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), selectedIcon, selectedColor) },
                enabled = name.isNotBlank()
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun TransactionCategoryGrid(
    pageItems: List<TransactionCategoryTile>,
    selectedCategory: String,
    onSelectCategory: (FinanceCategory) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        pageItems.chunked(4).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(4) { index ->
                    val item = rowItems.getOrNull(index)
                    if (item == null) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        when (item) {
                            is TransactionCategoryTile.CategoryItem -> {
                                TransactionCategoryButton(
                                    label = item.category.name,
                                    iconTint = colorFromHex(item.category.colorHex),
                                    icon = categoryIcon(item.category.iconName),
                                    selected = selectedCategory == item.category.name,
                                    onClick = { onSelectCategory(item.category) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            TransactionCategoryTile.Settings -> {
                                TransactionCategoryButton(
                                    label = "设置",
                                    iconTint = MaterialTheme.colorScheme.primary,
                                    icon = Icons.Rounded.Tune,
                                    selected = false,
                                    onClick = onOpenSettings,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionCategoryButton(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(52.dp)
                .border(
                    width = 2.dp,
                    color = if (selected) iconTint else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.34f),
                    shape = CircleShape
                ),
            shape = CircleShape,
            color = if (selected) iconTint.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
            tonalElevation = if (selected) 2.dp else 0.dp,
            shadowElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
@Composable
private fun TransactionComposerBar(
    note: String,
    amount: String,
    category: FinanceCategory,
    accent: Color,
    onNoteClick: () -> Unit,
    embedded: Boolean = false,
    modifier: Modifier = Modifier
) {
    val categoryColor = colorFromHex(category.colorHex)
    val shape = RoundedCornerShape(if (embedded) 20.dp else 24.dp)
    val iconContainerSize = if (embedded) 40.dp else 46.dp
    val iconSize = if (embedded) 20.dp else 24.dp
    val amountStyle = if (embedded) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium
    val noteStyle = if (embedded) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge
    val noteSpacing = if (embedded) 2.dp else 4.dp
    val horizontalPadding = if (embedded) 14.dp else 12.dp
    val verticalPadding = if (embedded) 10.dp else 12.dp

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(if (embedded) 1.dp else 1.5.dp, MaterialTheme.colorScheme.outlineVariant, shape),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (embedded) 1.dp else 2.dp,
        shadowElevation = if (embedded) 1.dp else 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = categoryColor.copy(alpha = 0.12f)
            ) {
                Box(
                    modifier = Modifier.size(iconContainerSize),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon(category.iconName),
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onNoteClick),
                verticalArrangement = Arrangement.spacedBy(noteSpacing)
            ) {
                Text(
                    text = "备注",
                    style = if (embedded) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = note.ifBlank { "点击写备注..." },
                    style = noteStyle,
                    color = if (note.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = formatEntryCurrency(amount),
                style = amountStyle,
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }
    }
}

private fun buildTransactionCategoryPages(categories: List<FinanceCategory>): List<List<TransactionCategoryTile>> {
    val items = categories.map(TransactionCategoryTile::CategoryItem) + TransactionCategoryTile.Settings
    return items.chunked(12).ifEmpty { listOf(listOf(TransactionCategoryTile.Settings)) }
}

private fun initialTransactionCategoryPage(
    pages: List<List<TransactionCategoryTile>>,
    selectedCategory: String
): Int {
    val foundIndex = pages.indexOfFirst { page ->
        page.any { item ->
            item is TransactionCategoryTile.CategoryItem && item.category.name == selectedCategory
        }
    }
    return if (foundIndex >= 0) foundIndex else 0
}
