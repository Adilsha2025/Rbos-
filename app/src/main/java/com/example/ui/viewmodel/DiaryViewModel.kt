package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.DiaryRepository
import com.example.model.AppSettings
import com.example.model.CreditEntry
import com.example.model.DayEntry
import com.example.model.DiaryItem
import com.example.model.EditLog
import com.example.util.GujaratiDateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class EntryDraftState(
    val date: String = "",
    val dateDisplay: String = "",
    val items: List<DiaryItem> = emptyList(),
    val total: Long = 0L,
    val isLocked: Boolean = false,
    val isEditingLocked: Boolean = false,
    val edits: List<EditLog> = emptyList(),
    val notes: String = "",
    val bankDepositAmount: Long = 0L,
    val bankDepositSlipUri: String? = null,
    val originalSavedEntry: DayEntry? = null,
    val previousSavedDayTotal: Long? = null
) {
    // Float total: "ID માં" + "રોકડ"
    val floatTotal: Long
        get() {
            val idAmt = items.firstOrNull { it.name.trim().contains("ID") }?.amount ?: 0L
            val cashAmt = items.firstOrNull { it.name.trim().contains("રોકડ") }?.amount ?: 0L
            return idAmt + cashAmt
        }

    // Red alert banner if today's total is more than 10,000 lower than yesterday
    val isDropAlertActive: Boolean
        get() {
            if (previousSavedDayTotal == null) return false
            return (previousSavedDayTotal - total) > 10000L
        }

    val dropDifference: Long
        get() = if (previousSavedDayTotal != null) previousSavedDayTotal - total else 0L
}

data class PeriodComparison(
    val currentPeriodLabel: String,
    val currentPeriodTotal: Long,
    val previousPeriodLabel: String,
    val previousPeriodTotal: Long,
    val difference: Long,
    val percentChange: Double
)

class DiaryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DiaryRepository(application)

    // All saved days from Room
    val allDays: StateFlow<List<DayEntry>> = repository.allDays.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // All Credit Entries ("ઉધાર યાદી")
    val allCredits: StateFlow<List<CreditEntry>> = repository.allCredits.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Current Entry Draft (Screen 1)
    private val _entryDraft = MutableStateFlow(EntryDraftState())
    val entryDraft: StateFlow<EntryDraftState> = _entryDraft.asStateFlow()

    // Cash Note Counter state (Screen 2)
    val denominations = listOf(500, 200, 100, 50, 20, 10, 5, 2, 1)
    private val _noteCounts = MutableStateFlow<Map<Int, Int>>(denominations.associateWith { 0 })
    val noteCounts: StateFlow<Map<Int, Int>> = _noteCounts.asStateFlow()

    private val _showNoteCounter = MutableStateFlow(false)
    val showNoteCounter: StateFlow<Boolean> = _showNoteCounter.asStateFlow()

    // History & Report Calendar selected date/month (Screen 3 & 4 in India Time)
    private val _selectedHistoryDate = MutableStateFlow(GujaratiDateUtils.getTodayIsoDate())
    val selectedHistoryDate: StateFlow<String> = _selectedHistoryDate.asStateFlow()

    private val _historyCalendarMonth = MutableStateFlow(run {
        val cal = GujaratiDateUtils.getIndiaCalendar()
        Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    })
    val historyCalendarMonth: StateFlow<Pair<Int, Int>> = _historyCalendarMonth.asStateFlow()

    private val _reportMonth = MutableStateFlow(run {
        val cal = GujaratiDateUtils.getIndiaCalendar()
        Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    })
    val reportMonth: StateFlow<Pair<Int, Int>> = _reportMonth.asStateFlow()

    // Settings (Screen 5)
    private val _settings = MutableStateFlow(repository.getSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    // 4-Digit PIN Lock State
    private val _isAppLocked = MutableStateFlow(_settings.value.pinEnabled && _settings.value.securityPin.length == 4)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    // Snackbar alerts
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.cleanSampleDataIfExists()
            initializeDay(GujaratiDateUtils.getTodayIsoDate())
        }
    }

    /**
     * Initializes or loads the day's entry in Asia/Kolkata timezone.
     */
    fun initializeDay(targetDate: String) {
        viewModelScope.launch {
            val existing = repository.getDay(targetDate)
            val previousDay = repository.getPreviousSavedDay(targetDate)
                ?: if (targetDate == GujaratiDateUtils.getTodayIsoDate()) repository.getMostRecentDay() else null

            if (existing != null) {
                _entryDraft.value = EntryDraftState(
                    date = existing.date,
                    dateDisplay = existing.dateDisplay,
                    items = existing.items,
                    total = existing.total,
                    isLocked = existing.locked,
                    isEditingLocked = false,
                    edits = existing.edits,
                    notes = existing.notes,
                    bankDepositAmount = existing.bankDepositAmount,
                    bankDepositSlipUri = existing.bankDepositSlipUri,
                    originalSavedEntry = existing,
                    previousSavedDayTotal = previousDay?.total
                )
            } else {
                val lastSavedDay = repository.getMostRecentDay()
                val currentSettings = _settings.value

                val initialItems = if (lastSavedDay != null && lastSavedDay.items.isNotEmpty()) {
                    lastSavedDay.items.map { it.copy() }
                } else {
                    currentSettings.defaultItems.map { DiaryItem(name = it, amount = 0L) }
                }

                val computedTotal = initialItems.sumOf { it.amount }

                _entryDraft.value = EntryDraftState(
                    date = targetDate,
                    dateDisplay = GujaratiDateUtils.formatGujaratiDate(targetDate),
                    items = initialItems,
                    total = computedTotal,
                    isLocked = false,
                    isEditingLocked = false,
                    edits = emptyList(),
                    notes = "",
                    bankDepositAmount = 0L,
                    bankDepositSlipUri = null,
                    originalSavedEntry = null,
                    previousSavedDayTotal = lastSavedDay?.total
                )
            }
        }
    }

    fun onItemAmountChanged(index: Int, newAmount: Long) {
        val current = _entryDraft.value
        if (index !in current.items.indices) return

        val updatedItems = current.items.toMutableList()
        val oldItem = updatedItems[index]
        updatedItems[index] = oldItem.copy(amount = newAmount.coerceAtLeast(0L))

        val newTotal = updatedItems.sumOf { it.amount }

        _entryDraft.value = current.copy(
            items = updatedItems,
            total = newTotal
        )
    }

    fun onAddNewCustomItem(name: String, amount: Long) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return

        val current = _entryDraft.value
        val updatedItems = current.items.toMutableList()
        updatedItems.add(DiaryItem(name = trimmed, amount = amount.coerceAtLeast(0L)))

        val newTotal = updatedItems.sumOf { it.amount }
        _entryDraft.value = current.copy(
            items = updatedItems,
            total = newTotal
        )
        _snackbarMessage.value = "'$trimmed' નવી વસ્તુ ઉમેરાઈ ગઈ"
    }

    fun onRemoveItem(index: Int) {
        val current = _entryDraft.value
        if (index !in current.items.indices) return

        val updatedItems = current.items.toMutableList()
        val removed = updatedItems.removeAt(index)
        val newTotal = updatedItems.sumOf { it.amount }

        _entryDraft.value = current.copy(
            items = updatedItems,
            total = newTotal
        )
        _snackbarMessage.value = "'${removed.name}' કાઢી નાખી"
    }

    // Notes and Bank Deposit
    fun onNotesChanged(notes: String) {
        _entryDraft.value = _entryDraft.value.copy(notes = notes)
    }

    fun onBankDepositAmountChanged(amount: Long) {
        _entryDraft.value = _entryDraft.value.copy(bankDepositAmount = amount.coerceAtLeast(0L))
    }

    fun onBankDepositSlipUriChanged(uri: String?) {
        _entryDraft.value = _entryDraft.value.copy(bankDepositSlipUri = uri)
        if (uri != null) {
            _snackbarMessage.value = "ડિપોઝિટ સ્લિપનો ફોટો જોડાઈ ગયો"
        }
    }

    fun onUnlockForEdit() {
        val current = _entryDraft.value
        _entryDraft.value = current.copy(isEditingLocked = true)
        _snackbarMessage.value = "ફેરફાર મોડ સક્રિય - કરેલા ફેરફારો નોંધવામાં આવશે"
    }

    fun onSaveDay() {
        viewModelScope.launch {
            val current = _entryDraft.value
            val original = current.originalSavedEntry

            val accumulatedEdits = current.edits.toMutableList()

            if (original != null && (current.isLocked || current.isEditingLocked)) {
                val nowTime = GujaratiDateUtils.formatCurrentTime()
                val originalMap = original.items.associate { it.name to it.amount }

                current.items.forEach { newItem ->
                    val oldAmt = originalMap[newItem.name]
                    if (oldAmt != null && oldAmt != newItem.amount) {
                        accumulatedEdits.add(
                            EditLog(
                                field = newItem.name,
                                oldValue = oldAmt,
                                newValue = newItem.amount,
                                time = nowTime
                            )
                        )
                    } else if (oldAmt == null && newItem.amount > 0) {
                        accumulatedEdits.add(
                            EditLog(
                                field = newItem.name,
                                oldValue = 0L,
                                newValue = newItem.amount,
                                time = nowTime
                            )
                        )
                    }
                }

                if (original.bankDepositAmount != current.bankDepositAmount) {
                    accumulatedEdits.add(
                        EditLog(
                            field = "બેંક જમા",
                            oldValue = original.bankDepositAmount,
                            newValue = current.bankDepositAmount,
                            time = nowTime
                        )
                    )
                }
            }

            val savedEntry = DayEntry(
                date = current.date,
                dateDisplay = current.dateDisplay,
                items = current.items,
                total = current.total,
                locked = true,
                edits = accumulatedEdits,
                notes = current.notes,
                bankDepositAmount = current.bankDepositAmount,
                bankDepositSlipUri = current.bankDepositSlipUri,
                updatedAt = System.currentTimeMillis()
            )

            repository.saveDay(savedEntry)

            _entryDraft.value = current.copy(
                isLocked = true,
                isEditingLocked = false,
                edits = accumulatedEdits,
                originalSavedEntry = savedEntry
            )

            _snackbarMessage.value = "ડાયરી સફળતાપૂર્વક સાચવી અને લોક થઈ ગઈ!"
        }
    }

    // Cash Note Counter Methods (Screen 2)
    fun openNoteCounter() {
        _showNoteCounter.value = true
    }

    fun closeNoteCounter() {
        _showNoteCounter.value = false
    }

    fun onNoteCountChanged(denomination: Int, count: Int) {
        val current = _noteCounts.value.toMutableMap()
        current[denomination] = count.coerceAtLeast(0)
        _noteCounts.value = current
    }

    fun onClearNoteCounts() {
        _noteCounts.value = denominations.associateWith { 0 }
    }

    fun onApplyNotesToCash() {
        val totalCash = _noteCounts.value.entries.sumOf { (denom, count) ->
            denom.toLong() * count
        }

        val current = _entryDraft.value
        val itemsList = current.items.toMutableList()
        val cashIndex = itemsList.indexOfFirst { it.name.trim() == "રોકડ" }

        if (cashIndex != -1) {
            val oldItem = itemsList[cashIndex]
            itemsList[cashIndex] = oldItem.copy(amount = totalCash)
        } else {
            itemsList.add(0, DiaryItem(name = "રોકડ", amount = totalCash))
        }

        val newTotal = itemsList.sumOf { it.amount }
        _entryDraft.value = current.copy(
            items = itemsList,
            total = newTotal
        )

        _showNoteCounter.value = false
        _snackbarMessage.value = "રોકડ રકમ ₹$totalCash સફળતાપૂર્વક ઉમેરી દીધી!"
    }

    // Credit Ledger ("ઉધાર યાદી")
    fun onAddCredit(personName: String, amount: Long, phone: String, notes: String, startDate: String) {
        viewModelScope.launch {
            val entry = CreditEntry(
                personName = personName.trim(),
                amount = amount.coerceAtLeast(0L),
                phone = phone.trim(),
                notes = notes.trim(),
                startDate = startDate
            )
            repository.saveCreditEntry(entry)
            _snackbarMessage.value = "'${entry.personName}' નું ઉધાર ખાતું ઉમેરાયું"
        }
    }

    fun onDeleteCredit(id: String) {
        viewModelScope.launch {
            repository.deleteCreditEntry(id)
            _snackbarMessage.value = "ઉધાર ખાતું કાઢી નાખ્યું"
        }
    }

    fun onToggleCreditPaid(id: String, currentPaid: Boolean) {
        viewModelScope.launch {
            val newStatus = !currentPaid
            val paidDate = if (newStatus) GujaratiDateUtils.getTodayIsoDate() else null
            repository.updateCreditPaidStatus(id, newStatus, paidDate)
            _snackbarMessage.value = if (newStatus) "રકમ ચૂકવાઈ ગઈ તરીકે ચિહ્નિત થઈ" else "બાકી તરીકે ચિહ્નિત થઈ"
        }
    }

    // 4-Digit PIN Lock Methods
    fun onUnlockWithPin(enteredPin: String): Boolean {
        val correctPin = _settings.value.securityPin
        if (enteredPin == correctPin) {
            _isAppLocked.value = false
            return true
        }
        return false
    }

    fun onUpdatePinSecurity(enabled: Boolean, newPin: String) {
        val updated = _settings.value.copy(
            pinEnabled = enabled,
            securityPin = if (enabled) newPin else ""
        )
        _settings.value = updated
        repository.saveSettings(updated)
        _isAppLocked.value = false
        _snackbarMessage.value = if (enabled) "૪-અંકનો સુરક્ષા PIN સેટ થઈ ગયો" else "PIN લૉક બંધ કર્યો"
    }

    fun lockAppManually() {
        if (_settings.value.pinEnabled && _settings.value.securityPin.length == 4) {
            _isAppLocked.value = true
        }
    }

    // History and Report month switchers
    fun onSelectHistoryDate(isoDate: String) {
        _selectedHistoryDate.value = isoDate
    }

    fun onHistoryMonthPrev() {
        val (year, month) = _historyCalendarMonth.value
        if (month == 0) {
            _historyCalendarMonth.value = Pair(year - 1, 11)
        } else {
            _historyCalendarMonth.value = Pair(year, month - 1)
        }
    }

    fun onHistoryMonthNext() {
        val (year, month) = _historyCalendarMonth.value
        if (month == 11) {
            _historyCalendarMonth.value = Pair(year + 1, 0)
        } else {
            _historyCalendarMonth.value = Pair(year, month + 1)
        }
    }

    fun onReportMonthPrev() {
        val (year, month) = _reportMonth.value
        if (month == 0) {
            _reportMonth.value = Pair(year - 1, 11)
        } else {
            _reportMonth.value = Pair(year, month - 1)
        }
    }

    fun onReportMonthNext() {
        val (year, month) = _reportMonth.value
        if (month == 11) {
            _reportMonth.value = Pair(year + 1, 0)
        } else {
            _reportMonth.value = Pair(year, month + 1)
        }
    }

    // Period Comparisons for Reports Screen
    fun getMonthVsLastMonthComparison(allEntries: List<DayEntry>, year: Int, monthIndex: Int): PeriodComparison {
        val curPrefix = String.format("%04d-%02d", year, monthIndex + 1)
        val curTotal = allEntries.filter { it.date.startsWith(curPrefix) }.sumOf { it.total }

        val prevYear = if (monthIndex == 0) year - 1 else year
        val prevMonth = if (monthIndex == 0) 11 else monthIndex - 1
        val prevPrefix = String.format("%04d-%02d", prevYear, prevMonth + 1)
        val prevTotal = allEntries.filter { it.date.startsWith(prevPrefix) }.sumOf { it.total }

        val diff = curTotal - prevTotal
        val pct = if (prevTotal > 0) (diff.toDouble() / prevTotal.toDouble()) * 100.0 else 0.0

        return PeriodComparison(
            currentPeriodLabel = GujaratiDateUtils.formatMonthYear(year, monthIndex),
            currentPeriodTotal = curTotal,
            previousPeriodLabel = GujaratiDateUtils.formatMonthYear(prevYear, prevMonth),
            previousPeriodTotal = prevTotal,
            difference = diff,
            percentChange = pct
        )
    }

    fun getWeekVsLastWeekComparison(allEntries: List<DayEntry>): PeriodComparison {
        val entriesByDate = allEntries.associateBy { it.date }
        val cal = GujaratiDateUtils.getIndiaCalendar()

        var thisWeekTotal = 0L
        for (i in 0 until 7) {
            val iso = String.format("%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
            thisWeekTotal += entriesByDate[iso]?.total ?: 0L
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        var lastWeekTotal = 0L
        for (i in 0 until 7) {
            val iso = String.format("%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
            lastWeekTotal += entriesByDate[iso]?.total ?: 0L
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        val diff = thisWeekTotal - lastWeekTotal
        val pct = if (lastWeekTotal > 0) (diff.toDouble() / lastWeekTotal.toDouble()) * 100.0 else 0.0

        return PeriodComparison(
            currentPeriodLabel = "આ અઠવાડિયું (છેલ્લા 7 દિવસ)",
            currentPeriodTotal = thisWeekTotal,
            previousPeriodLabel = "ગત અઠવાડિયું",
            previousPeriodTotal = lastWeekTotal,
            difference = diff,
            percentChange = pct
        )
    }

    // Settings actions
    fun onToggleReminder(enabled: Boolean) {
        val updated = _settings.value.copy(reminderEnabled = enabled)
        _settings.value = updated
        repository.saveSettings(updated)
        _snackbarMessage.value = if (enabled) "રાત્રે 9:00 નું રિમાઇન્ડર ચાલુ કર્યું" else "રિમાઇન્ડર બંધ કર્યું"
    }

    fun onToggleDriveBackup(enabled: Boolean) {
        val updated = _settings.value.copy(driveBackup = enabled)
        _settings.value = updated
        repository.saveSettings(updated)
        _snackbarMessage.value = if (enabled) "Google Drive બેકઅપ ચાલુ કર્યું" else "Google Drive બેકઅપ બંધ કર્યું"
    }

    fun onAddDefaultItem(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val currentItems = _settings.value.defaultItems.toMutableList()
        if (!currentItems.contains(trimmed)) {
            currentItems.add(trimmed)
            val updated = _settings.value.copy(defaultItems = currentItems)
            _settings.value = updated
            repository.saveSettings(updated)
            _snackbarMessage.value = "'$trimmed' ડિફોલ્ટ યાદીમાં ઉમેરાઈ ગઈ"
        }
    }

    fun onRemoveDefaultItem(index: Int) {
        val currentItems = _settings.value.defaultItems.toMutableList()
        if (index in currentItems.indices) {
            val removed = currentItems.removeAt(index)
            val updated = _settings.value.copy(defaultItems = currentItems)
            _settings.value = updated
            repository.saveSettings(updated)
            _snackbarMessage.value = "'$removed' કાઢી નાખી"
        }
    }

    fun onRenameDefaultItem(index: Int, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        val currentItems = _settings.value.defaultItems.toMutableList()
        if (index in currentItems.indices) {
            currentItems[index] = trimmed
            val updated = _settings.value.copy(defaultItems = currentItems)
            _settings.value = updated
            repository.saveSettings(updated)
            _snackbarMessage.value = "નામ બદલાઈ ગયું: $trimmed"
        }
    }

    fun onMoveDefaultItem(fromIndex: Int, toIndex: Int) {
        val currentItems = _settings.value.defaultItems.toMutableList()
        if (fromIndex in currentItems.indices && toIndex in currentItems.indices) {
            val item = currentItems.removeAt(fromIndex)
            currentItems.add(toIndex, item)
            val updated = _settings.value.copy(defaultItems = currentItems)
            _settings.value = updated
            repository.saveSettings(updated)
        }
    }

    fun onResetDefaultItems() {
        val updated = _settings.value.copy(defaultItems = repository.defaultDefaultItems())
        _settings.value = updated
        repository.saveSettings(updated)
        _snackbarMessage.value = "ડિફોલ્ટ યાદી ફરીથી સેટ થઈ ગઈ"
    }

    suspend fun getJsonExport(): String {
        return repository.exportToJson()
    }

    suspend fun importJsonData(jsonString: String): Int {
        val count = repository.importFromJson(jsonString)
        if (count > 0) {
            _snackbarMessage.value = "$count રેકોર્ડ સફળતાપૂર્વક ઇમ્પોર્ટ થયા!"
            initializeDay(_entryDraft.value.date)
        } else {
            _snackbarMessage.value = "ઇમ્પોર્ટ નિષ્ફળ: અમાન્ય JSON ડેટા"
        }
        return count
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            initializeDay(GujaratiDateUtils.getTodayIsoDate())
            _snackbarMessage.value = "બધો ડેટા સાફ થઈ ગયો છે - નવી શરૂઆત"
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }
}
