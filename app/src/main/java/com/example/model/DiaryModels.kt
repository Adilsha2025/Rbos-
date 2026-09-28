package com.example.model

import java.util.UUID

data class DiaryItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val amount: Long = 0L
)

data class ExpenseEntry(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Long = 0L
)

data class EditLog(
    val id: String = UUID.randomUUID().toString(),
    val field: String,
    val oldValue: Long,
    val newValue: Long,
    val time: String
)

data class DayEntry(
    val date: String, // ISO format "yyyy-MM-dd" in Asia/Kolkata
    val dateDisplay: String, // Gujarati display string e.g. "સોમવાર, 28 સપ્ટેમ્બર 2026"
    val items: List<DiaryItem>,
    val total: Long,
    val locked: Boolean = false,
    val edits: List<EditLog> = emptyList(),
    val notes: String = "",
    val bankDepositAmount: Long = 0L,
    val bankDepositSlipUri: String? = null,
    val commission: Long = 0L,
    val expenses: List<ExpenseEntry> = emptyList(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val totalExpenses: Long
        get() = expenses.sumOf { it.amount }

    val dailyProfit: Long
        get() = commission - totalExpenses
}

data class CreditEntry(
    val id: String = UUID.randomUUID().toString(),
    val personName: String,
    val amount: Long,
    val phone: String = "",
    val startDate: String, // ISO format "yyyy-MM-dd"
    val notes: String = "",
    val isPaid: Boolean = false,
    val paidDate: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class AppSettings(
    val defaultItems: List<String> = listOf(
        "ID માં",
        "રોકડ",
        "મારી પાસે રહેલ ખાતા",
        "ખાતા બુક બાકી",
        "ગુલાબશા પાસે",
        "સનાતનભાઈ પાસે લેવાના"
    ),
    val reminderTime: String = "21:00",
    val reminderEnabled: Boolean = true,
    val driveBackup: Boolean = false,
    val pinEnabled: Boolean = false,
    val securityPin: String = "",
    val isDarkMode: Boolean? = null, // null = system default, true = Dark, false = Light
    val closingReminderEnabled: Boolean = true // Reminders on 30 Sep and 31 Mar closing dates
)

data class CashDenomination(
    val value: Int,
    val count: Int = 0
) {
    val total: Long get() = value.toLong() * count
}

data class DiaryScanResult(
    val items: List<DiaryItem> = emptyList(),
    val commission: Long = 0L,
    val expenses: List<ExpenseEntry> = emptyList(),
    val bankDeposit: Long = 0L,
    val notes: String = "",
    val imageUri: String? = null
)
