package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Thread-safe API 24+ date utility using ISO yyyy-MM-dd storage format.
 */
object DateUtils {
    private val enLocale = Locale.ENGLISH

    fun todayIso(): String = formatIso(Calendar.getInstance())

    fun yesterdayIso(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, -1)
        return formatIso(cal)
    }

    fun daysAgoIso(days: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, -days)
        return formatIso(cal)
    }

    fun currentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)

    /** 1-indexed month (1 = January .. 12 = December). */
    fun currentMonth(): Int = Calendar.getInstance().get(Calendar.MONTH) + 1

    fun formatIso(cal: Calendar): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", enLocale)
        return sdf.format(cal.time)
    }

    fun formatIsoFromMillis(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", enLocale)
        return sdf.format(Date(millis))
    }

    fun parseIsoToMillis(isoDate: String): Long {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", enLocale)
            sdf.isLenient = false
            sdf.parse(isoDate)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    fun formatDisplayDate(isoDate: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", enLocale)
            val formatter = SimpleDateFormat("dd MMM yyyy", enLocale)
            val date = parser.parse(isoDate)
            if (date != null) formatter.format(date) else isoDate
        } catch (_: Exception) {
            isoDate
        }
    }

    fun formatShortDate(isoDate: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", enLocale)
            val formatter = SimpleDateFormat("dd MMM", enLocale)
            val date = parser.parse(isoDate)
            if (date != null) formatter.format(date) else isoDate
        } catch (_: Exception) {
            isoDate
        }
    }

    fun formatFullTodayDisplay(): String {
        val formatter = SimpleDateFormat("EEEE, dd MMM yyyy", enLocale)
        return formatter.format(Date())
    }

    fun formatTimestamp(millis: Long): String {
        val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", enLocale)
        return formatter.format(Date(millis))
    }

    fun monthStartIso(year: Int, month: Int): String {
        return String.format(enLocale, "%04d-%02d-01", year, month)
    }

    fun monthEndIso(year: Int, month: Int): String {
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, (month - 1).coerceIn(0, 11))
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        return String.format(enLocale, "%04d-%02d-%02d", year, month, lastDay)
    }

    fun monthName(month: Int, marathi: Boolean = false): String {
        val enMonths = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        val mrMonths = listOf(
            "जानेवारी", "फेब्रुवारी", "मार्च", "एप्रिल", "मे", "जून",
            "जुलै", "ऑगस्ट", "सप्टेंबर", "ऑक्टोबर", "नोव्हेंबर", "डिसेंबर"
        )
        val idx = (month - 1).coerceIn(0, 11)
        return if (marathi) mrMonths[idx] else enMonths[idx]
    }

    fun monthYearLabel(year: Int, month: Int, marathi: Boolean = false): String {
        return "${monthName(month, marathi)} $year"
    }

    fun isValidIsoDate(value: String): Boolean {
        if (!Regex("""^\d{4}-\d{2}-\d{2}$""").matches(value)) return false
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", enLocale)
            sdf.isLenient = false
            sdf.parse(value) != null
        } catch (_: Exception) {
            false
        }
    }

    fun isoDateForDayInMonth(year: Int, month: Int, day: Int): String {
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, (month - 1).coerceIn(0, 11))
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val safeDay = day.coerceIn(1, maxDay)
        return String.format(enLocale, "%04d-%02d-%02d", year, month, safeDay)
    }
}
