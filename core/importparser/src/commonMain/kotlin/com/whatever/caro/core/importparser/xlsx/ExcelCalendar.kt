package com.whatever.caro.core.importparser.xlsx

import com.whatever.caro.core.importparser.SpreadsheetDateStyle

internal data class ExcelDate(
    val year: Int,
    val month: Int,
    val day: Int,
) {
    val monthName: String get() = MONTH_NAMES[month - 1]
    val shortYear: String get() = (year % CENTURY).toString().padStart(2, '0')

    fun format(style: SpreadsheetDateStyle): String =
        when (style) {
            SpreadsheetDateStyle.YEAR_MONTH_DAY -> "$year-${month.twoDigits()}-${day.twoDigits()}"
            SpreadsheetDateStyle.MONTH_DAY_YEAR -> "$month/$day/$year"
        }

    private fun Int.twoDigits(): String = toString().padStart(2, '0')

    private companion object {
        const val CENTURY = 100
        val MONTH_NAMES = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    }
}

/** 1900 기준은 Lotus 호환 때문에 없는 날짜인 1900-02-29(일련번호 60)가 있어 60보다 작은 번호는 하루 늦게 센다. */
internal object ExcelCalendar {
    private const val FICTIONAL_LEAP_DAY_SERIAL = 60L
    private val EPOCH_1900_EARLY = daysFromCivil(1899, 12, 31)
    private val EPOCH_1900 = daysFromCivil(1899, 12, 30)
    private val EPOCH_1904 = daysFromCivil(1904, 1, 1)

    fun dateOf(
        serialDays: Long,
        date1904: Boolean,
    ): ExcelDate =
        when {
            date1904 -> civilFromDays(EPOCH_1904 + serialDays)
            serialDays == 0L -> ExcelDate(1900, 1, 0)
            serialDays == FICTIONAL_LEAP_DAY_SERIAL -> ExcelDate(1900, 2, 29)
            serialDays < FICTIONAL_LEAP_DAY_SERIAL -> civilFromDays(EPOCH_1900_EARLY + serialDays)
            else -> civilFromDays(EPOCH_1900 + serialDays)
        }

    // 1970-01-01 기준 일수와 그레고리력 날짜 사이 변환(Howard Hinnant의 공개 알고리즘)
    private const val DAYS_PER_ERA = 146_097L
    private const val YEARS_PER_ERA = 400L
    private const val EPOCH_SHIFT_DAYS = 719_468L
    private const val MONTHS_FROM_MARCH = 12
    private const val MARCH = 3
    private const val DAYS_IN_FIVE_MONTHS = 153
    private const val FIVE_MONTH_OFFSET = 2

    private fun daysFromCivil(
        year: Int,
        month: Int,
        day: Int,
    ): Long {
        val y = (if (month <= 2) year - 1 else year).toLong()
        val era = (if (y >= 0) y else y - (YEARS_PER_ERA - 1)) / YEARS_PER_ERA
        val yearOfEra = y - era * YEARS_PER_ERA
        val shiftedMonth = (month + if (month > 2) -MARCH else MONTHS_FROM_MARCH - MARCH)
        val dayOfYear = (DAYS_IN_FIVE_MONTHS * shiftedMonth + FIVE_MONTH_OFFSET) / 5 + day - 1
        val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
        return era * DAYS_PER_ERA + dayOfEra - EPOCH_SHIFT_DAYS
    }

    private fun civilFromDays(epochDays: Long): ExcelDate {
        val z = epochDays + EPOCH_SHIFT_DAYS
        val era = (if (z >= 0) z else z - (DAYS_PER_ERA - 1)) / DAYS_PER_ERA
        val dayOfEra = z - era * DAYS_PER_ERA
        val yearOfEra = (dayOfEra - dayOfEra / 1460 + dayOfEra / 36524 - dayOfEra / (DAYS_PER_ERA - 1)) / 365
        val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
        val shiftedMonth = (5 * dayOfYear + FIVE_MONTH_OFFSET) / DAYS_IN_FIVE_MONTHS
        val day = (dayOfYear - (DAYS_IN_FIVE_MONTHS * shiftedMonth + FIVE_MONTH_OFFSET) / 5 + 1).toInt()
        val month = (if (shiftedMonth < 10) shiftedMonth + MARCH else shiftedMonth - (MONTHS_FROM_MARCH - MARCH)).toInt()
        val year = (yearOfEra + era * YEARS_PER_ERA).toInt() + if (month <= 2) 1 else 0
        return ExcelDate(year, month, day)
    }
}
