package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.DayEntry
import com.example.model.DiaryItem
import com.example.model.EditLog
import com.example.model.ExpenseEntry
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "day_entries")
data class DayEntryEntity(
    @PrimaryKey
    val date: String, // "yyyy-MM-dd"
    val dateDisplay: String,
    val itemsJson: String,
    val total: Long,
    val locked: Boolean,
    val editsJson: String,
    val notes: String = "",
    val bankDepositAmount: Long = 0L,
    val bankDepositSlipUri: String? = null,
    val commission: Long = 0L,
    val expensesJson: String = "[]",
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): DayEntry {
        val itemsList = mutableListOf<DiaryItem>()
        try {
            val jsonArray = JSONArray(itemsJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                itemsList.add(
                    DiaryItem(
                        id = obj.optString("id"),
                        name = obj.getString("name"),
                        amount = obj.getLong("amount")
                    )
                )
            }
        } catch (_: Exception) {}

        val editsList = mutableListOf<EditLog>()
        try {
            val jsonArray = JSONArray(editsJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                editsList.add(
                    EditLog(
                        id = obj.optString("id"),
                        field = obj.getString("field"),
                        oldValue = obj.getLong("oldValue"),
                        newValue = obj.getLong("newValue"),
                        time = obj.getString("time")
                    )
                )
            }
        } catch (_: Exception) {}

        val expensesList = mutableListOf<ExpenseEntry>()
        try {
            val jsonArray = JSONArray(expensesJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                expensesList.add(
                    ExpenseEntry(
                        id = obj.optString("id"),
                        title = obj.getString("title"),
                        amount = obj.getLong("amount")
                    )
                )
            }
        } catch (_: Exception) {}

        return DayEntry(
            date = date,
            dateDisplay = dateDisplay,
            items = itemsList,
            total = total,
            locked = locked,
            edits = editsList,
            notes = notes,
            bankDepositAmount = bankDepositAmount,
            bankDepositSlipUri = bankDepositSlipUri,
            commission = commission,
            expenses = expensesList,
            isDeleted = isDeleted,
            deletedAt = deletedAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(entry: DayEntry): DayEntryEntity {
            val itemsArray = JSONArray()
            entry.items.forEach { item ->
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("name", item.name)
                obj.put("amount", item.amount)
                itemsArray.put(obj)
            }

            val editsArray = JSONArray()
            entry.edits.forEach { edit ->
                val obj = JSONObject()
                obj.put("id", edit.id)
                obj.put("field", edit.field)
                obj.put("oldValue", edit.oldValue)
                obj.put("newValue", edit.newValue)
                obj.put("time", edit.time)
                editsArray.put(obj)
            }

            val expensesArray = JSONArray()
            entry.expenses.forEach { exp ->
                val obj = JSONObject()
                obj.put("id", exp.id)
                obj.put("title", exp.title)
                obj.put("amount", exp.amount)
                expensesArray.put(obj)
            }

            return DayEntryEntity(
                date = entry.date,
                dateDisplay = entry.dateDisplay,
                itemsJson = itemsArray.toString(),
                total = entry.total,
                locked = entry.locked,
                editsJson = editsArray.toString(),
                notes = entry.notes,
                bankDepositAmount = entry.bankDepositAmount,
                bankDepositSlipUri = entry.bankDepositSlipUri,
                commission = entry.commission,
                expensesJson = expensesArray.toString(),
                isDeleted = entry.isDeleted,
                deletedAt = entry.deletedAt,
                updatedAt = entry.updatedAt
            )
        }
    }
}
