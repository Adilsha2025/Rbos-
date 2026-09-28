package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object GujaratiDateUtils {

    val INDIA_TIME_ZONE: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")

    private val WEEKDAYS = arrayOf(
        "", // 0 unused
        "રવિવાર",    // Calendar.SUNDAY = 1
        "સોમવાર",    // Calendar.MONDAY = 2
        "મંગળવાર",   // Calendar.TUESDAY = 3
        "બુધવાર",    // Calendar.WEDNESDAY = 4
        "ગુરુવાર",   // Calendar.THURSDAY = 5
        "શુક્રવાર",   // Calendar.FRIDAY = 6
        "શનિવાર"     // Calendar.SATURDAY = 7
    )

    private val MONTHS = arrayOf(
        "જાન્યુઆરી",
        "ફેબ્રુઆરી",
        "માર્ચ",
        "એપ્રિલ",
        "મે",
        "જૂન",
        "જુલાઈ",
        "ઓગસ્ટ",
        "સપ્ટેમ્બર",
        "ઓક્ટોબર",
        "નવેમ્બર",
        "ડિસેમ્બર"
    )

    private fun getIsoDateFormat(): SimpleDateFormat {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = INDIA_TIME_ZONE
        }
    }

    private fun getTimeFormat(): SimpleDateFormat {
        return SimpleDateFormat("hh:mm a", Locale.US).apply {
            timeZone = INDIA_TIME_ZONE
        }
    }

    /**
     * Returns today's ISO date string in India time (Asia/Kolkata): "yyyy-MM-dd"
     */
    fun getTodayIsoDate(): String {
        return getIsoDateFormat().format(Date())
    }

    /**
     * Formats an ISO date string (e.g. "2026-09-28") into Gujarati:
     * "સોમવાર, 28 સપ્ટેમ્બર 2026" using India Time Zone
     */
    fun formatGujaratiDate(isoDate: String): String {
        return try {
            val format = getIsoDateFormat()
            val date = format.parse(isoDate) ?: Date()
            val cal = Calendar.getInstance(INDIA_TIME_ZONE).apply { time = date }
            val dayOfWeek = WEEKDAYS[cal.get(Calendar.DAY_OF_WEEK)]
            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
            val month = MONTHS[cal.get(Calendar.MONTH)]
            val year = cal.get(Calendar.YEAR)
            "$dayOfWeek, $dayOfMonth $month $year"
        } catch (_: Exception) {
            isoDate
        }
    }

    /**
     * Formats Year and Month (0-11) to "સપ્ટેમ્બર 2026"
     */
    fun formatMonthYear(year: Int, monthIndexZeroBased: Int): String {
        val safeMonth = monthIndexZeroBased.coerceIn(0, 11)
        return "${MONTHS[safeMonth]} $year"
    }

    /**
     * Formats current timestamp in India time as "02:30 PM"
     */
    fun formatCurrentTime(timestamp: Long = System.currentTimeMillis()): String {
        return getTimeFormat().format(Date(timestamp))
    }

    /**
     * Helper to get calendar days for a specific year and month (0-based) in India time
     */
    fun getDaysInMonth(year: Int, monthIndexZeroBased: Int): Int {
        val cal = Calendar.getInstance(INDIA_TIME_ZONE).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthIndexZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    /**
     * Returns starting day of week for the 1st of month (1=Sunday, 7=Saturday) in India time
     */
    fun getFirstDayOfWeek(year: Int, monthIndexZeroBased: Int): Int {
        val cal = Calendar.getInstance(INDIA_TIME_ZONE).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthIndexZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.get(Calendar.DAY_OF_WEEK)
    }

    fun buildIsoDate(year: Int, monthZeroBased: Int, day: Int): String {
        return String.format(Locale.US, "%04d-%02d-%02d", year, monthZeroBased + 1, day)
    }

    fun parseYearAndMonth(isoDate: String): Pair<Int, Int> {
        return try {
            val parts = isoDate.split("-")
            val year = parts[0].toInt()
            val month = parts[1].toInt() - 1
            Pair(year, month)
        } catch (_: Exception) {
            val cal = Calendar.getInstance(INDIA_TIME_ZONE)
            Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
        }
    }

    fun getIndiaCalendar(): Calendar {
        return Calendar.getInstance(INDIA_TIME_ZONE)
    }
}
