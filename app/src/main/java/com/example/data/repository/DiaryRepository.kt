package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.firestore.FirestoreSyncManager
import com.example.data.local.AppDatabase
import com.example.data.local.CreditEntryEntity
import com.example.data.local.DayEntryEntity
import com.example.model.AppSettings
import com.example.model.CreditEntry
import com.example.model.DayEntry
import com.example.model.DiaryItem
import com.example.model.EditLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class DiaryRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val dayDao = db.dayEntryDao()
    private val creditDao = db.creditDao()
    private val prefs: SharedPreferences =
        context.getSharedPreferences("diary_prefs", Context.MODE_PRIVATE)

    val allDays: Flow<List<DayEntry>> = dayDao.getAllDays().map { list ->
        list.map { it.toDomain() }
    }

    val allCredits: Flow<List<CreditEntry>> = creditDao.getAllCreditEntries().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getDay(date: String): DayEntry? {
        return dayDao.getDay(date)?.toDomain()
    }

    suspend fun getPreviousSavedDay(date: String): DayEntry? {
        return dayDao.getPreviousSavedDay(date)?.toDomain()
    }

    suspend fun getMostRecentDay(): DayEntry? {
        return dayDao.getMostRecentDay()?.toDomain()
    }

    suspend fun saveDay(entry: DayEntry) {
        val entity = DayEntryEntity.fromDomain(entry)
        dayDao.insertOrUpdate(entity)
        // Attempt cloud Firestore sync
        FirestoreSyncManager.syncDayEntry(entry)
    }

    suspend fun deleteDay(date: String) {
        dayDao.deleteDay(date)
    }

    // Credit Entries operations
    suspend fun saveCreditEntry(entry: CreditEntry) {
        creditDao.insertOrUpdate(CreditEntryEntity.fromDomain(entry))
    }

    suspend fun deleteCreditEntry(id: String) {
        creditDao.deleteCreditEntry(id)
    }

    suspend fun updateCreditPaidStatus(id: String, isPaid: Boolean, paidDate: String?) {
        creditDao.updatePaidStatus(id, isPaid, paidDate)
    }

    fun getSettings(): AppSettings {
        val defaultItemsString = prefs.getString("default_items", null)
        val defaultList = if (defaultItemsString != null) {
            try {
                val array = JSONArray(defaultItemsString)
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    list.add(array.getString(i))
                }
                list
            } catch (_: Exception) {
                defaultDefaultItems()
            }
        } else {
            defaultDefaultItems()
        }

        return AppSettings(
            defaultItems = defaultList,
            reminderTime = prefs.getString("reminder_time", "21:00") ?: "21:00",
            reminderEnabled = prefs.getBoolean("reminder_enabled", true),
            driveBackup = prefs.getBoolean("drive_backup", false),
            pinEnabled = prefs.getBoolean("pin_enabled", false),
            securityPin = prefs.getString("security_pin", "") ?: ""
        )
    }

    fun saveSettings(settings: AppSettings) {
        val array = JSONArray()
        settings.defaultItems.forEach { array.put(it) }
        prefs.edit()
            .putString("default_items", array.toString())
            .putString("reminder_time", settings.reminderTime)
            .putBoolean("reminder_enabled", settings.reminderEnabled)
            .putBoolean("drive_backup", settings.driveBackup)
            .putBoolean("pin_enabled", settings.pinEnabled)
            .putString("security_pin", settings.securityPin)
            .apply()
    }

    fun defaultDefaultItems(): List<String> = listOf(
        "ID માં",
        "રોકડ",
        "મારી પાસે રહેલ ખાતા",
        "ખાતા બુક બાકી",
        "ગુલાબશા પાસે",
        "સનાતનભાઈ પાસે લેવાના"
    )

    suspend fun exportToJson(): String {
        val root = JSONObject()
        val daysArray = JSONArray()

        return try {
            val cursor = db.openHelper.readableDatabase.query("SELECT * FROM day_entries ORDER BY date DESC")
            while (cursor.moveToNext()) {
                val date = cursor.getString(cursor.getColumnIndexOrThrow("date"))
                val dateDisplay = cursor.getString(cursor.getColumnIndexOrThrow("dateDisplay"))
                val itemsJson = cursor.getString(cursor.getColumnIndexOrThrow("itemsJson"))
                val total = cursor.getLong(cursor.getColumnIndexOrThrow("total"))
                val locked = cursor.getInt(cursor.getColumnIndexOrThrow("locked")) == 1
                val editsJson = cursor.getString(cursor.getColumnIndexOrThrow("editsJson"))
                val notes = cursor.getString(cursor.getColumnIndexOrThrow("notes")) ?: ""
                val bankDepositAmount = cursor.getLong(cursor.getColumnIndexOrThrow("bankDepositAmount"))
                val bankDepositSlipUri = cursor.getString(cursor.getColumnIndexOrThrow("bankDepositSlipUri"))
                val updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updatedAt"))

                val dayObj = JSONObject().apply {
                    put("date", date)
                    put("dateDisplay", dateDisplay)
                    put("items", JSONArray(itemsJson))
                    put("total", total)
                    put("locked", locked)
                    put("edits", JSONArray(editsJson))
                    put("notes", notes)
                    put("bankDepositAmount", bankDepositAmount)
                    put("bankDepositSlipUri", bankDepositSlipUri ?: "")
                    put("updatedAt", updatedAt)
                }
                daysArray.put(dayObj)
            }
            cursor.close()

            // Also export credits
            val creditsArray = JSONArray()
            val creditCursor = db.openHelper.readableDatabase.query("SELECT * FROM credit_entries ORDER BY createdAt DESC")
            while (creditCursor.moveToNext()) {
                val cObj = JSONObject().apply {
                    put("id", creditCursor.getString(creditCursor.getColumnIndexOrThrow("id")))
                    put("personName", creditCursor.getString(creditCursor.getColumnIndexOrThrow("personName")))
                    put("amount", creditCursor.getLong(creditCursor.getColumnIndexOrThrow("amount")))
                    put("phone", creditCursor.getString(creditCursor.getColumnIndexOrThrow("phone")) ?: "")
                    put("startDate", creditCursor.getString(creditCursor.getColumnIndexOrThrow("startDate")))
                    put("notes", creditCursor.getString(creditCursor.getColumnIndexOrThrow("notes")) ?: "")
                    put("isPaid", creditCursor.getInt(creditCursor.getColumnIndexOrThrow("isPaid")) == 1)
                    put("paidDate", creditCursor.getString(creditCursor.getColumnIndexOrThrow("paidDate")) ?: "")
                }
                creditsArray.put(cObj)
            }
            creditCursor.close()

            root.put("app", "રોજની ડાયરી")
            root.put("version", 2)
            root.put("exportTime", System.currentTimeMillis())
            root.put("days", daysArray)
            root.put("credits", creditsArray)
            root.toString(2)
        } catch (e: Exception) {
            JSONObject().put("error", e.message).toString()
        }
    }

    suspend fun importFromJson(jsonString: String): Int {
        var count = 0
        try {
            val root = JSONObject(jsonString)
            if (root.has("days")) {
                val daysArray = root.getJSONArray("days")
                for (i in 0 until daysArray.length()) {
                    val dayObj = daysArray.getJSONObject(i)
                    val date = dayObj.getString("date")
                    val dateDisplay = dayObj.optString("dateDisplay", "")
                    val total = dayObj.getLong("total")
                    val locked = dayObj.optBoolean("locked", false)
                    val updatedAt = dayObj.optLong("updatedAt", System.currentTimeMillis())
                    val itemsArray = dayObj.getJSONArray("items")
                    val editsArray = dayObj.optJSONArray("edits") ?: JSONArray()
                    val notes = dayObj.optString("notes", "")
                    val bankDepositAmount = dayObj.optLong("bankDepositAmount", 0L)
                    val bankDepositSlipUri = dayObj.optString("bankDepositSlipUri", null)

                    val entity = DayEntryEntity(
                        date = date,
                        dateDisplay = dateDisplay,
                        itemsJson = itemsArray.toString(),
                        total = total,
                        locked = locked,
                        editsJson = editsArray.toString(),
                        notes = notes,
                        bankDepositAmount = bankDepositAmount,
                        bankDepositSlipUri = if (bankDepositSlipUri.isNullOrEmpty()) null else bankDepositSlipUri,
                        updatedAt = updatedAt
                    )
                    dayDao.insertOrUpdate(entity)
                    count++
                }
            }

            if (root.has("credits")) {
                val creditsArray = root.getJSONArray("credits")
                for (i in 0 until creditsArray.length()) {
                    val cObj = creditsArray.getJSONObject(i)
                    val credit = CreditEntry(
                        id = cObj.getString("id"),
                        personName = cObj.getString("personName"),
                        amount = cObj.getLong("amount"),
                        phone = cObj.optString("phone", ""),
                        startDate = cObj.getString("startDate"),
                        notes = cObj.optString("notes", ""),
                        isPaid = cObj.optBoolean("isPaid", false),
                        paidDate = cObj.optString("paidDate", null)
                    )
                    creditDao.insertOrUpdate(CreditEntryEntity.fromDomain(credit))
                }
            }
        } catch (_: Exception) {}
        return count
    }

    suspend fun cleanSampleDataIfExists() {
        val sampleDates = listOf("2026-09-25", "2026-09-26", "2026-09-27")
        sampleDates.forEach { date ->
            dayDao.deleteDay(date)
        }
    }

    suspend fun clearAllData() {
        dayDao.deleteAll()
        creditDao.deleteAll()
    }
}
