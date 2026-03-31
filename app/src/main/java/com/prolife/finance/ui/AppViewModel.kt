package com.prolife.finance.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.prolife.finance.data.FinanceRepository
import com.prolife.finance.notification.PendingPaymentStore
import com.prolife.finance.notification.ParsedPayment
import com.prolife.finance.model.AppThemeStyle
import com.prolife.finance.model.DrinkCollection
import com.prolife.finance.model.DrinkRecord
import com.prolife.finance.model.DrinkType
import com.prolife.finance.model.FinanceSnapshot
import com.prolife.finance.model.StatsGranularity
import com.prolife.finance.model.ThemePreference
import com.prolife.finance.model.TransactionRecord
import com.prolife.finance.model.TransactionType
import com.prolife.finance.model.categoriesFor
import com.prolife.finance.model.saveCategories
import com.prolife.finance.model.FinanceCategory
import com.prolife.finance.model.normalizedForLegacyDesktop
import android.app.Application
import com.prolife.finance.notification.BookkeepKeepAliveService
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

enum class BottomDestination(val label: String) {
    DASHBOARD("\u9996\u9875"),
    DETAIL("\u660e\u7ec6"),
    STATS("\u7edf\u8ba1"),
    BILLS("\u8d26\u5355")
}

@Immutable
data class TransactionEditorState(
    val transactionId: String? = null,
    val type: TransactionType = TransactionType.EXPENSE,
    val amount: String = "",
    val category: String = categoriesFor(TransactionType.EXPENSE).first().name,
    val date: String = LocalDate.now().toString(),
    val note: String = "",
    val pendingPaymentKey: String? = null
)

@Immutable
data class DrinkEditorState(
    val recordId: String? = null,
    val linkedTransactionId: String? = null,
    val type: DrinkType = DrinkType.MILK_TEA,
    val date: String = LocalDate.now().toString(),
    val cups: String = "1",
    val cost: String = "",
    val sugar: String = "",
    val brand: String = "",
    val notes: String = ""
)

@Immutable
data class DrinkSettingsEditorState(
    val milkteaWeeklyLimit: String,
    val milkteaMonthlyLimit: String,
    val coffeeWeeklyLimit: String,
    val coffeeMonthlyLimit: String
)

@Immutable
data class AppUiState(
    val currentTab: BottomDestination = BottomDestination.DASHBOARD,
    val transactions: List<TransactionRecord> = emptyList(),
    val milktea: DrinkCollection = DrinkCollection.defaultFor(DrinkType.MILK_TEA),
    val coffee: DrinkCollection = DrinkCollection.defaultFor(DrinkType.COFFEE),
    val selectedMonth: YearMonth = YearMonth.now(),
    val statsGranularity: StatsGranularity = StatsGranularity.MONTH,
    val statsAnchorDate: LocalDate = LocalDate.now().withDayOfMonth(1),
    val themePreference: ThemePreference = ThemePreference(),
    val editor: TransactionEditorState? = null,
    val drinkEditor: DrinkEditorState? = null,
    val drinkSettingsEditor: DrinkSettingsEditorState? = null,
    val message: String? = null,
    val pendingPayments: List<ParsedPayment> = emptyList(),
    val autoBookkeepEnabled: Boolean = false,
    val aaSplitEnabled: Boolean = false,
    val aaSplitThreshold: Float = 100f
)

sealed interface AppAction {
    data class SwitchTab(val tab: BottomDestination) : AppAction
    data object PreviousMonth : AppAction
    data object NextMonth : AppAction
    data class SelectMonth(val value: YearMonth) : AppAction
    data class SelectStatsGranularity(val value: StatsGranularity) : AppAction
    data class SelectStatsPeriod(val granularity: StatsGranularity, val anchorDate: LocalDate) : AppAction
    data object PreviousStatsPeriod : AppAction
    data object NextStatsPeriod : AppAction
    data class JumpToBillMonth(val value: YearMonth) : AppAction
    data class JumpToBillYear(val year: Int) : AppAction
    data class ToggleDarkMode(val enabled: Boolean) : AppAction
    data class SetThemeStyle(val style: AppThemeStyle) : AppAction
    data object OpenCreateExpense : AppAction
    data object OpenCreateIncome : AppAction
    data class OpenCreateDrink(val preferredType: DrinkType) : AppAction
    data class OpenEditTransaction(val transactionId: String) : AppAction
    data class OpenEditDrink(val recordId: String, val type: DrinkType) : AppAction
    data object DismissEditor : AppAction
    data object DismissDrinkEditor : AppAction
    data object DismissDrinkSettings : AppAction
    data class UpdateEditorAmount(val value: String) : AppAction
    data class UpdateEditorDate(val value: String) : AppAction
    data class UpdateEditorNote(val value: String) : AppAction
    data class UpdateEditorCategory(val value: String) : AppAction
    data class UpdateEditorType(val value: TransactionType) : AppAction
    data class UpdateDrinkType(val value: DrinkType) : AppAction
    data class UpdateDrinkDate(val value: String) : AppAction
    data class UpdateDrinkCups(val value: String) : AppAction
    data class UpdateDrinkCost(val value: String) : AppAction
    data class UpdateDrinkSugar(val value: String) : AppAction
    data class UpdateDrinkBrand(val value: String) : AppAction
    data class UpdateDrinkNotes(val value: String) : AppAction
    data object SaveEditor : AppAction
    data object SaveDrinkEditor : AppAction
    data object OpenDrinkSettings : AppAction
    data class UpdateMilkteaWeeklyLimit(val value: String) : AppAction
    data class UpdateMilkteaMonthlyLimit(val value: String) : AppAction
    data class UpdateCoffeeWeeklyLimit(val value: String) : AppAction
    data class UpdateCoffeeMonthlyLimit(val value: String) : AppAction
    data object SaveDrinkSettings : AppAction
    data class DeleteTransaction(val transactionId: String) : AppAction
    data class DeleteDrinkRecord(val recordId: String, val type: DrinkType) : AppAction
    data object ConsumeMessage : AppAction
    data object CheckPendingPayments : AppAction
    data class OpenEditorFromPayment(val payment: ParsedPayment) : AppAction
    data class DismissPendingPayment(val payment: ParsedPayment) : AppAction
    data object DismissAllPendingPayments : AppAction
    data object ToggleAutoBookkeep : AppAction
    data object ToggleAASplit : AppAction
    data class SetAASplitThreshold(val amount: Float) : AppAction
    data object RefreshFromCache : AppAction
    data class AddCategory(val category: FinanceCategory) : AppAction
    data class EditCategory(val oldName: String, val category: FinanceCategory) : AppAction
    data class DeleteCategory(val name: String, val type: TransactionType) : AppAction
}

class AppViewModel(
    private val repository: FinanceRepository,
    private val paymentStore: PendingPaymentStore? = null,
    private val application: Application? = null
) : ViewModel() {
    private val cachedSnapshot = repository.loadCachedSnapshot()
    private val normalizedCachedSnapshot = cachedSnapshot.copy(
        transactions = cachedSnapshot.transactions.normalizedForLegacyDesktop()
    )

    var uiState by mutableStateOf(
        AppUiState(
            themePreference = repository.loadThemePreference(),
            transactions = normalizedCachedSnapshot.transactions,
            milktea = normalizedCachedSnapshot.milktea,
            coffee = normalizedCachedSnapshot.coffee,
            autoBookkeepEnabled = repository.loadAutoBookkeepEnabled(),
            aaSplitEnabled = repository.loadAASplitEnabled(),
            aaSplitThreshold = repository.loadAASplitThreshold()
        )
    )
        private set

    init {
        if (normalizedCachedSnapshot != cachedSnapshot) {
            repository.cacheSnapshot(normalizedCachedSnapshot)
        }
        checkPendingPayments()
    }

    fun onAction(action: AppAction) {
        when (action) {
            is AppAction.SwitchTab -> updateState { copy(currentTab = action.tab) }
            AppAction.PreviousMonth -> updateState { copy(selectedMonth = selectedMonth.minusMonths(1)) }
            AppAction.NextMonth -> updateState { copy(selectedMonth = selectedMonth.plusMonths(1)) }
            is AppAction.SelectMonth -> updateState { copy(selectedMonth = action.value) }
            is AppAction.SelectStatsGranularity -> setStatsGranularity(action.value)
            is AppAction.SelectStatsPeriod -> setStatsPeriod(action.granularity, action.anchorDate)
            AppAction.PreviousStatsPeriod -> shiftStatsPeriod(-1)
            AppAction.NextStatsPeriod -> shiftStatsPeriod(1)
            is AppAction.JumpToBillMonth -> jumpToBillMonth(action.value)
            is AppAction.JumpToBillYear -> jumpToBillYear(action.year)
            is AppAction.ToggleDarkMode -> persistTheme(uiState.themePreference.copy(darkMode = action.enabled))
            is AppAction.SetThemeStyle -> persistTheme(uiState.themePreference.copy(style = action.style))
            AppAction.OpenCreateExpense -> openCreateEditor(TransactionType.EXPENSE)
            AppAction.OpenCreateIncome -> openCreateEditor(TransactionType.INCOME)
            is AppAction.OpenCreateDrink -> openCreateDrinkEditor(action.preferredType)
            is AppAction.OpenEditTransaction -> openEditEditor(action.transactionId)
            is AppAction.OpenEditDrink -> openEditDrinkEditor(action.recordId, action.type)
            AppAction.DismissEditor -> updateState { copy(editor = null) }
            AppAction.DismissDrinkEditor -> updateState { copy(drinkEditor = null) }
            AppAction.DismissDrinkSettings -> updateState { copy(drinkSettingsEditor = null) }
            is AppAction.UpdateEditorAmount -> updateEditor { copy(amount = action.value) }
            is AppAction.UpdateEditorDate -> updateEditor { copy(date = action.value) }
            is AppAction.UpdateEditorNote -> updateEditor { copy(note = action.value) }
            is AppAction.UpdateEditorCategory -> {
                updateEditor { copy(category = action.value) }
                val key = uiState.editor?.pendingPaymentKey
                if (key != null && uiState.editor?.amount?.toDoubleOrNull()?.let { it > 0 } == true) {
                    saveEditor()
                }
            }
            is AppAction.UpdateEditorType -> updateEditor {
                val nextCategories = categoriesFor(action.value)
                val nextCategory = if (nextCategories.any { it.name == category }) category else nextCategories.first().name
                copy(type = action.value, category = nextCategory)
            }
            is AppAction.UpdateDrinkType -> updateDrinkEditor { copy(type = action.value) }
            is AppAction.UpdateDrinkDate -> updateDrinkEditor { copy(date = action.value) }
            is AppAction.UpdateDrinkCups -> updateDrinkEditor { copy(cups = action.value) }
            is AppAction.UpdateDrinkCost -> updateDrinkEditor { copy(cost = action.value) }
            is AppAction.UpdateDrinkSugar -> updateDrinkEditor { copy(sugar = action.value) }
            is AppAction.UpdateDrinkBrand -> updateDrinkEditor { copy(brand = action.value) }
            is AppAction.UpdateDrinkNotes -> updateDrinkEditor { copy(notes = action.value) }
            AppAction.SaveEditor -> saveEditor()
            AppAction.SaveDrinkEditor -> saveDrinkEditor()
            AppAction.OpenDrinkSettings -> openDrinkSettingsEditor()
            is AppAction.UpdateMilkteaWeeklyLimit -> updateDrinkSettingsEditor { copy(milkteaWeeklyLimit = action.value) }
            is AppAction.UpdateMilkteaMonthlyLimit -> updateDrinkSettingsEditor { copy(milkteaMonthlyLimit = action.value) }
            is AppAction.UpdateCoffeeWeeklyLimit -> updateDrinkSettingsEditor { copy(coffeeWeeklyLimit = action.value) }
            is AppAction.UpdateCoffeeMonthlyLimit -> updateDrinkSettingsEditor { copy(coffeeMonthlyLimit = action.value) }
            AppAction.SaveDrinkSettings -> saveDrinkSettings()
            is AppAction.DeleteTransaction -> deleteTransaction(action.transactionId)
            is AppAction.DeleteDrinkRecord -> deleteDrinkRecord(action.recordId, action.type)
            AppAction.ConsumeMessage -> updateState { copy(message = null) }
            AppAction.CheckPendingPayments -> checkPendingPayments()
            is AppAction.OpenEditorFromPayment -> openEditorFromPayment(action.payment)
            is AppAction.DismissPendingPayment -> dismissPendingPayment(action.payment)
            AppAction.DismissAllPendingPayments -> dismissAllPendingPayments()
            AppAction.ToggleAutoBookkeep -> toggleAutoBookkeep()
            AppAction.ToggleAASplit -> toggleAASplit()
            is AppAction.SetAASplitThreshold -> setAASplitThreshold(action.amount)
            AppAction.RefreshFromCache -> refreshFromCache()
            is AppAction.AddCategory -> addCategory(action.category)
            is AppAction.EditCategory -> editCategory(action.oldName, action.category)
            is AppAction.DeleteCategory -> deleteCategory(action.name, action.type)
        }
    }

    private fun persistTheme(preference: ThemePreference) {
        repository.saveThemePreference(preference)
        updateState { copy(themePreference = preference) }
    }

    private fun setStatsGranularity(value: StatsGranularity) {
        updateState {
            copy(
                statsGranularity = value,
                statsAnchorDate = normalizeAnchor(statsAnchorDate, value)
            )
        }
    }

    private fun setStatsPeriod(granularity: StatsGranularity, anchorDate: LocalDate) {
        updateState {
            val normalizedAnchor = normalizeAnchor(anchorDate, granularity)
            copy(
                statsGranularity = granularity,
                statsAnchorDate = normalizedAnchor,
                selectedMonth = if (granularity == StatsGranularity.MONTH) {
                    YearMonth.from(normalizedAnchor)
                } else {
                    selectedMonth
                }
            )
        }
    }

    private fun shiftStatsPeriod(offset: Long) {
        val nextAnchor = when (uiState.statsGranularity) {
            StatsGranularity.WEEK -> uiState.statsAnchorDate.plusWeeks(offset)
            StatsGranularity.MONTH -> uiState.statsAnchorDate.plusMonths(offset)
            StatsGranularity.YEAR -> uiState.statsAnchorDate.plusYears(offset)
        }
        updateState { copy(statsAnchorDate = normalizeAnchor(nextAnchor, statsGranularity)) }
    }

    private fun jumpToBillMonth(month: YearMonth) {
        updateState {
            copy(
                currentTab = BottomDestination.STATS,
                statsGranularity = StatsGranularity.MONTH,
                statsAnchorDate = month.atDay(1),
                selectedMonth = month
            )
        }
    }

    private fun jumpToBillYear(year: Int) {
        updateState {
            copy(
                currentTab = BottomDestination.STATS,
                statsGranularity = StatsGranularity.YEAR,
                statsAnchorDate = LocalDate.of(year, 1, 1)
            )
        }
    }

    private fun openCreateEditor(type: TransactionType) {
        updateState {
            copy(
                editor = TransactionEditorState(
                    type = type,
                    category = categoriesFor(type).first().name
                )
            )
        }
    }

    private fun openCreateDrinkEditor(preferredType: DrinkType) {
        updateState {
            copy(
                currentTab = BottomDestination.STATS,
                drinkEditor = DrinkEditorState(
                    type = preferredType,
                    cups = "1"
                )
            )
        }
    }

    private fun openEditEditor(transactionId: String) {
        val transaction = uiState.transactions.firstOrNull { it.id == transactionId }
        if (transaction == null) {
            updateState { copy(message = "\u8be5\u8bb0\u5f55\u5df2\u88ab\u5220\u9664") }
            return
        }
        transaction.milkteaRecordId?.let { linkedId ->
            openEditDrinkEditor(
                recordId = linkedId,
                type = detectDrinkType(transaction, linkedId),
                linkedTransactionId = transaction.id
            )
            return
        }
        updateState {
            copy(
                editor = TransactionEditorState(
                    transactionId = transaction.id,
                    type = transaction.type,
                    amount = if (transaction.amount == 0.0) "" else formatDecimalInput(transaction.amount),
                    category = transaction.category,
                    date = transaction.date.toString(),
                    note = transaction.note
                )
            )
        }
    }

    private fun openEditDrinkEditor(recordId: String, type: DrinkType, linkedTransactionId: String? = null) {
        val record = findDrinkRecord(recordId, type)
        if (record == null) {
            updateState { copy(message = "\u8be5\u996e\u54c1\u8bb0\u5f55\u5df2\u88ab\u5220\u9664") }
            return
        }
        val resolvedLinkedTransactionId = linkedTransactionId
            ?: uiState.transactions.firstOrNull { isLinkedDrinkTransaction(it, record.id) }?.id
        updateState {
            copy(
                currentTab = BottomDestination.STATS,
                drinkEditor = DrinkEditorState(
                    recordId = record.id,
                    linkedTransactionId = resolvedLinkedTransactionId,
                    type = record.drinkType,
                    date = record.date.toString(),
                    cups = formatDecimalInput(record.cups),
                    cost = formatDecimalInput(record.cost),
                    sugar = record.sugar,
                    brand = record.brand,
                    notes = record.notes
                )
            )
        }
    }

    private fun updateEditor(transform: TransactionEditorState.() -> TransactionEditorState) {
        val current = uiState.editor ?: return
        updateState { copy(editor = current.transform()) }
    }

    private fun updateDrinkEditor(transform: DrinkEditorState.() -> DrinkEditorState) {
        val current = uiState.drinkEditor ?: return
        updateState { copy(drinkEditor = current.transform()) }
    }

    private fun updateDrinkSettingsEditor(transform: DrinkSettingsEditorState.() -> DrinkSettingsEditorState) {
        val current = uiState.drinkSettingsEditor ?: return
        updateState { copy(drinkSettingsEditor = current.transform()) }
    }

    private fun openDrinkSettingsEditor() {
        updateState {
            copy(
                drinkSettingsEditor = DrinkSettingsEditorState(
                    milkteaWeeklyLimit = milktea.settings.weeklyLimit.toString(),
                    milkteaMonthlyLimit = milktea.settings.monthlyLimit.toString(),
                    coffeeWeeklyLimit = coffee.settings.weeklyLimit.toString(),
                    coffeeMonthlyLimit = coffee.settings.monthlyLimit.toString()
                )
            )
        }
    }

    private fun saveEditor() {
        val editor = uiState.editor ?: return
        val amount = editor.amount.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            updateState { copy(message = "\u8bf7\u8f93\u5165\u6709\u6548\u91d1\u989d") }
            return
        }

        val date = runCatching { LocalDate.parse(editor.date) }.getOrNull()
        if (date == null) {
            updateState { copy(message = "\u65e5\u671f\u683c\u5f0f\u5e94\u4e3a YYYY-MM-DD") }
            return
        }

        val previous = editor.transactionId?.let { transactionId ->
            uiState.transactions.firstOrNull { it.id == transactionId }
        }
        val category = categoriesFor(editor.type).firstOrNull { it.name == editor.category }
            ?: categoriesFor(editor.type).first()
        val transaction = TransactionRecord(
            id = editor.transactionId ?: System.currentTimeMillis().toString(),
            type = editor.type,
            date = date,
            category = category.name,
            amount = amount,
            note = editor.note.trim(),
            colorHex = category.colorHex,
            updatedAt = Instant.now(),
            createdAt = previous?.createdAt ?: Instant.now(),
            milkteaRecordId = previous?.milkteaRecordId,
            extraFieldsJson = previous?.extraFieldsJson
        ).normalizedForLegacyDesktop()

        val updatedTransactions = uiState.transactions
            .filterNot { it.id == transaction.id }
            .plus(transaction)
            .sortedWith(compareByDescending<TransactionRecord> { it.date }.thenByDescending { it.updatedAt })

        var nextSnapshot = FinanceSnapshot(
            transactions = updatedTransactions,
            milktea = uiState.milktea,
            coffee = uiState.coffee
        )
        if (transaction.milkteaRecordId != null) {
            nextSnapshot = syncLinkedDrinkRecord(nextSnapshot, transaction)
        }

        persistSnapshot(
            snapshot = nextSnapshot,
            successMessage = "\u8d26\u76ee\u5df2\u4fdd\u5b58"
        )
        editor.pendingPaymentKey?.let { paymentStore?.removeByNotificationKey(it) }
        updateState { copy(editor = null) }
        checkPendingPayments()
    }

    private fun saveDrinkEditor() {
        val editor = uiState.drinkEditor ?: return
        val cups = editor.cups.toDoubleOrNull()
        if (cups == null || cups <= 0.0) {
            updateState { copy(message = "\u8bf7\u8f93\u5165\u6709\u6548\u676f\u6570") }
            return
        }

        val cost = editor.cost.toDoubleOrNull()
        if (cost == null || cost <= 0.0) {
            updateState { copy(message = "\u8bf7\u8f93\u5165\u6709\u6548\u82b1\u8d39") }
            return
        }

        val date = runCatching { LocalDate.parse(editor.date) }.getOrNull()
        if (date == null) {
            updateState { copy(message = "\u65e5\u671f\u683c\u5f0f\u5e94\u4e3a YYYY-MM-DD") }
            return
        }

        val normalizedRecord = DrinkRecord(
            id = editor.recordId ?: System.currentTimeMillis().toString(),
            date = date,
            cups = cups,
            cost = cost,
            sugar = editor.sugar.trim(),
            brand = editor.brand.trim(),
            notes = editor.notes.trim(),
            drinkType = editor.type
        ).normalized()

        val remainingMilktea = uiState.milktea.records.filterNot { it.id == normalizedRecord.id }
        val remainingCoffee = uiState.coffee.records.filterNot { it.id == normalizedRecord.id }
        val nextMilktea = if (normalizedRecord.drinkType == DrinkType.MILK_TEA) {
            uiState.milktea.copy(records = sortDrinkRecords(remainingMilktea + normalizedRecord))
        } else {
            uiState.milktea.copy(records = remainingMilktea)
        }
        val nextCoffee = if (normalizedRecord.drinkType == DrinkType.COFFEE) {
            uiState.coffee.copy(records = sortDrinkRecords(remainingCoffee + normalizedRecord))
        } else {
            uiState.coffee.copy(records = remainingCoffee)
        }

        val existingLinkedTransaction = editor.linkedTransactionId
            ?.let { transactionId -> uiState.transactions.firstOrNull { it.id == transactionId } }
            ?: uiState.transactions.firstOrNull { isLinkedDrinkTransaction(it, normalizedRecord.id) }
        val linkedTransaction = buildDrinkTransaction(normalizedRecord, existingLinkedTransaction)
        val nextTransactions = uiState.transactions
            .filterNot { transaction ->
                isLinkedDrinkTransaction(transaction, normalizedRecord.id) ||
                    (editor.linkedTransactionId != null && transaction.id == editor.linkedTransactionId)
            }
            .plus(linkedTransaction)
            .sortedWith(compareByDescending<TransactionRecord> { it.date }.thenByDescending { it.updatedAt })

        persistSnapshot(
            snapshot = FinanceSnapshot(
                transactions = nextTransactions,
                milktea = nextMilktea,
                coffee = nextCoffee
            ),
            successMessage = "${normalizedRecord.drinkType.label}\u8bb0\u5f55\u5df2\u4fdd\u5b58"
        )
        updateState { copy(drinkEditor = null) }
    }

    private fun saveDrinkSettings() {
        val editor = uiState.drinkSettingsEditor ?: return
        val milkteaWeeklyLimit = editor.milkteaWeeklyLimit.toIntOrNull()
        val milkteaMonthlyLimit = editor.milkteaMonthlyLimit.toIntOrNull()
        val coffeeWeeklyLimit = editor.coffeeWeeklyLimit.toIntOrNull()
        val coffeeMonthlyLimit = editor.coffeeMonthlyLimit.toIntOrNull()
        if (milkteaWeeklyLimit == null || milkteaMonthlyLimit == null || coffeeWeeklyLimit == null || coffeeMonthlyLimit == null) {
            updateState { copy(message = "\u8bf7\u8f93\u5165\u6709\u6548\u7684\u6574\u6570\u9650\u989d") }
            return
        }
        if (milkteaWeeklyLimit < 0 || milkteaMonthlyLimit < 0 || coffeeWeeklyLimit < 0 || coffeeMonthlyLimit < 0) {
            updateState { copy(message = "\u9650\u989d\u4e0d\u80fd\u5c0f\u4e8e 0") }
            return
        }

        persistSnapshot(
            snapshot = FinanceSnapshot(
                transactions = uiState.transactions,
                milktea = uiState.milktea.copy(
                    settings = uiState.milktea.settings.copy(
                        weeklyLimit = milkteaWeeklyLimit,
                        monthlyLimit = milkteaMonthlyLimit
                    )
                ),
                coffee = uiState.coffee.copy(
                    settings = uiState.coffee.settings.copy(
                        weeklyLimit = coffeeWeeklyLimit,
                        monthlyLimit = coffeeMonthlyLimit
                    )
                )
            ),
            successMessage = "\u996e\u54c1\u9650\u989d\u5df2\u4fdd\u5b58"
        )
        updateState { copy(drinkSettingsEditor = null) }
    }

    private fun deleteTransaction(transactionId: String) {
        val removed = uiState.transactions.firstOrNull { it.id == transactionId }
        val updatedTransactions = uiState.transactions
            .filterNot { it.id == transactionId }

        var nextSnapshot = FinanceSnapshot(
            transactions = updatedTransactions,
            milktea = uiState.milktea,
            coffee = uiState.coffee
        )
        removed?.milkteaRecordId?.let { linkedId ->
            nextSnapshot = removeLinkedDrinkRecord(nextSnapshot, linkedId)
        }

        persistSnapshot(
            snapshot = nextSnapshot,
            successMessage = "\u5df2\u5220\u9664"
        )
    }

    private fun deleteDrinkRecord(recordId: String, type: DrinkType) {
        val nextSnapshot = FinanceSnapshot(
            transactions = uiState.transactions
                .filterNot { isLinkedDrinkTransaction(it, recordId) },
            milktea = uiState.milktea.copy(records = uiState.milktea.records.filterNot { it.id == recordId }),
            coffee = uiState.coffee.copy(records = uiState.coffee.records.filterNot { it.id == recordId })
        )

        persistSnapshot(
            snapshot = nextSnapshot,
            successMessage = "${type.label}\u8bb0\u5f55\u5df2\u5220\u9664"
        )
        updateState { copy(drinkEditor = null) }
    }

    private fun persistSnapshot(
        snapshot: FinanceSnapshot,
        successMessage: String
    ) {
        repository.cacheSnapshot(snapshot)
        updateState {
            copy(
                transactions = snapshot.transactions,
                milktea = snapshot.milktea,
                coffee = snapshot.coffee,
                message = successMessage
            )
        }
    }

    private fun syncLinkedDrinkRecord(snapshot: FinanceSnapshot, transaction: TransactionRecord): FinanceSnapshot {
        val linkedId = transaction.milkteaRecordId ?: return snapshot
        return snapshot.copy(
            milktea = updateLinkedDrinkRecord(snapshot.milktea, linkedId, transaction),
            coffee = updateLinkedDrinkRecord(snapshot.coffee, linkedId, transaction)
        )
    }

    private fun updateLinkedDrinkRecord(
        collection: DrinkCollection,
        linkedId: String,
        transaction: TransactionRecord
    ): DrinkCollection {
        if (collection.records.none { it.id == linkedId }) return collection
        return collection.copy(
            records = collection.records.map { record ->
                if (record.id == linkedId) {
                    record.copy(
                        date = transaction.date,
                        cost = transaction.amount
                    )
                } else {
                    record
                }
            }
        )
    }

    private fun removeLinkedDrinkRecord(snapshot: FinanceSnapshot, linkedId: String): FinanceSnapshot {
        return snapshot.copy(
            milktea = snapshot.milktea.copy(records = snapshot.milktea.records.filterNot { it.id == linkedId }),
            coffee = snapshot.coffee.copy(records = snapshot.coffee.records.filterNot { it.id == linkedId })
        )
    }

    private fun findDrinkRecord(recordId: String, preferredType: DrinkType): DrinkRecord? {
        val primary = when (preferredType) {
            DrinkType.MILK_TEA -> uiState.milktea.records
            DrinkType.COFFEE -> uiState.coffee.records
        }
        return primary.firstOrNull { it.id == recordId }
            ?: uiState.milktea.records.firstOrNull { it.id == recordId }
            ?: uiState.coffee.records.firstOrNull { it.id == recordId }
    }

    private fun detectDrinkType(transaction: TransactionRecord, linkedId: String): DrinkType {
        return when {
            uiState.coffee.records.any { it.id == linkedId } -> DrinkType.COFFEE
            uiState.milktea.records.any { it.id == linkedId } -> DrinkType.MILK_TEA
            transaction.category == DrinkType.COFFEE.categoryName -> DrinkType.COFFEE
            else -> DrinkType.MILK_TEA
        }
    }

    private fun buildDrinkTransaction(
        record: DrinkRecord,
        existingTransaction: TransactionRecord?
    ): TransactionRecord {
        val category = categoriesFor(TransactionType.EXPENSE)
            .firstOrNull { it.name == record.drinkType.categoryName }
        val now = Instant.now()
        val note = listOf(record.brand.trim(), record.notes.trim())
            .filter { it.isNotBlank() }
            .joinToString(" \u00b7 ")
            .ifBlank { "${record.drinkType.label}\u6d88\u8d39" }
        return TransactionRecord(
            id = existingTransaction?.id ?: "mt_${record.id}",
            type = TransactionType.EXPENSE,
            date = record.date,
            category = record.drinkType.categoryName,
            amount = record.cost,
            note = note,
            colorHex = category?.colorHex ?: record.drinkType.accentHex,
            updatedAt = now,
            createdAt = existingTransaction?.createdAt ?: now,
            milkteaRecordId = record.id,
            extraFieldsJson = existingTransaction?.extraFieldsJson
        ).normalizedForLegacyDesktop()
    }

    private fun isLinkedDrinkTransaction(transaction: TransactionRecord, linkedId: String): Boolean {
        val linkedMatch = transaction.milkteaRecordId == linkedId
        val legacyMatch = transaction.id.equals("mt_$linkedId", ignoreCase = true)
        return linkedMatch || legacyMatch
    }

    private fun sortDrinkRecords(records: List<DrinkRecord>): List<DrinkRecord> {
        return records.sortedWith(compareByDescending<DrinkRecord> { it.date }.thenByDescending { it.id })
    }

    private fun formatDecimalInput(value: Double): String {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
    }

    private fun currentSnapshot(): FinanceSnapshot = FinanceSnapshot(
        transactions = uiState.transactions,
        milktea = uiState.milktea,
        coffee = uiState.coffee
    )

    private fun updateState(transform: AppUiState.() -> AppUiState) {
        uiState = uiState.transform()
    }

    private fun normalizeAnchor(date: LocalDate, granularity: StatsGranularity): LocalDate {
        return when (granularity) {
            StatsGranularity.WEEK -> date.minusDays((date.dayOfWeek.value - 1).toLong())
            StatsGranularity.MONTH -> date.withDayOfMonth(1)
            StatsGranularity.YEAR -> date.withDayOfYear(1)
        }
    }

    // --- Auto bookkeep ---

    private fun checkPendingPayments() {
        val store = paymentStore ?: return
        if (!uiState.autoBookkeepEnabled) {
            updateState { copy(pendingPayments = emptyList()) }
            return
        }
        val now = System.currentTimeMillis()
        val twentyFourHours = 24 * 60 * 60 * 1000L
        val storedPayments = store.loadAll()
        val fresh = storedPayments.filter { now - it.timestamp < twentyFourHours }
        if (fresh.size != storedPayments.size) {
            store.clearAll()
            fresh.forEach { store.enqueue(it) }
        }

        val hadPending = uiState.pendingPayments.isNotEmpty()
        val hasPendingNow = fresh.isNotEmpty()
        if (hadPending && !hasPendingNow) {
            reloadFromCache()
        }

        updateState { copy(pendingPayments = fresh) }
    }

    private fun openEditorFromPayment(payment: ParsedPayment) {
        if (!uiState.autoBookkeepEnabled) return
        if (uiState.editor != null) {
            checkPendingPayments()
            updateState { copy(message = "\u68c0\u6d4b\u5230\u65b0\u7684\u652f\u4ed8\uff0c\u5df2\u52a0\u5165\u5f85\u8bb0\u8d26\u5217\u8868") }
            return
        }
        updateState {
            copy(
                editor = TransactionEditorState(
                    type = TransactionType.EXPENSE,
                    amount = payment.amount,
                    date = paymentDateString(payment),
                    note = buildAutoNote(payment),
                    pendingPaymentKey = payment.notificationKey
                )
            )
        }
        checkPendingPayments()
    }

    private fun dismissPendingPayment(payment: ParsedPayment) {
        paymentStore?.remove(payment)
        checkPendingPayments()
    }

    private fun dismissAllPendingPayments() {
        paymentStore?.clearAll()
        updateState { copy(pendingPayments = emptyList()) }
    }

    private fun reloadFromCache() {
        val snapshot = repository.loadCachedSnapshot()
        val normalized = snapshot.copy(
            transactions = snapshot.transactions.normalizedForLegacyDesktop()
        )
        updateState {
            copy(
                transactions = normalized.transactions,
                milktea = normalized.milktea,
                coffee = normalized.coffee
            )
        }
    }

    private fun toggleAutoBookkeep() {
        val enabled = !uiState.autoBookkeepEnabled
        repository.saveAutoBookkeepEnabled(enabled)
        if (!enabled) {
            paymentStore?.clearAll()
        }
        updateState {
            copy(
                autoBookkeepEnabled = enabled,
                pendingPayments = if (enabled) pendingPayments else emptyList()
            )
        }
        if (enabled) {
            checkPendingPayments()
            application?.let { BookkeepKeepAliveService.start(it) }
        } else {
            application?.let { BookkeepKeepAliveService.stop(it) }
        }
    }

    private fun toggleAASplit() {
        val enabled = !uiState.aaSplitEnabled
        repository.saveAASplitEnabled(enabled)
        updateState { copy(aaSplitEnabled = enabled) }
    }

    private fun setAASplitThreshold(amount: Float) {
        repository.saveAASplitThreshold(amount)
        updateState { copy(aaSplitThreshold = amount) }
    }

    private fun refreshFromCache() {
        val freshSnapshot = repository.loadCachedSnapshot().let {
            it.copy(transactions = it.transactions.normalizedForLegacyDesktop())
        }
        updateState {
            copy(
                transactions = freshSnapshot.transactions,
                milktea = freshSnapshot.milktea,
                coffee = freshSnapshot.coffee
            )
        }
    }

    private fun addCategory(category: FinanceCategory) {
        val ctx = application ?: return
        val current = categoriesFor(category.type, ctx).toMutableList()
        current.add(category)
        saveCategories(ctx, category.type, current)
    }

    private fun editCategory(oldName: String, category: FinanceCategory) {
        val ctx = application ?: return
        val current = categoriesFor(category.type, ctx).toMutableList()
        val index = current.indexOfFirst { it.name == oldName }
        if (index >= 0) {
            current[index] = category
            saveCategories(ctx, category.type, current)
        }
    }

    private fun deleteCategory(name: String, type: TransactionType) {
        val ctx = application ?: return
        val current = categoriesFor(type, ctx).toMutableList()
        current.removeAll { it.name == name }
        saveCategories(ctx, type, current)
    }

    private fun buildAutoNote(payment: ParsedPayment): String {
        val sourceLabel = payment.source.label
        return if (payment.merchant.isNotBlank()) "$sourceLabel - ${payment.merchant}" else sourceLabel
    }

    private fun paymentDateString(payment: ParsedPayment): String {
        return Instant.ofEpochMilli(payment.timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .toString()
    }

    companion object {
        fun factory(
            repository: FinanceRepository,
            paymentStore: PendingPaymentStore? = null,
            application: Application? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AppViewModel(repository, paymentStore, application) as T
                }
            }
    }
}
