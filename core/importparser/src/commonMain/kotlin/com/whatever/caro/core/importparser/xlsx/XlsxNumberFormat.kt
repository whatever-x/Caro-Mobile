package com.whatever.caro.core.importparser.xlsx

import com.whatever.caro.core.importparser.SpreadsheetDateStyle
import kotlin.math.roundToLong

/** 셀 숫자를 Excel 기본 서식대로 보이는 글자로 바꾼 결과. [raw]면 표 밖 서식이라 저장된 값을 그대로 쓴 것이다. */
internal data class FormattedNumber(
    val text: String,
    val raw: Boolean,
)

/** 일반, 천 단위, 퍼센트, 기본 날짜·시간 서식만 Excel 표시와 같게 만든다. 통화와 사용자 지정 서식은 저장된 값을 쓴다. */
internal class XlsxNumberFormat(
    private val date1904: Boolean,
    private val dateStyle: SpreadsheetDateStyle,
) {
    fun format(
        stored: String,
        numFmtId: Int,
    ): FormattedNumber {
        val number = DecimalNumber.parse(stored) ?: return FormattedNumber(stored, raw = true)
        val text =
            when (numFmtId) {
                GENERAL -> number.roundToSignificant(GENERAL_SIGNIFICANT_DIGITS).toPlainString()
                THOUSANDS -> number.roundToScale(0).toPlainString(fractionDigits = 0, grouping = true)
                THOUSANDS_DECIMAL -> number.roundToScale(2).toPlainString(fractionDigits = 2, grouping = true)
                PERCENT -> percent(number, fractionDigits = 0)
                PERCENT_DECIMAL -> percent(number, fractionDigits = 2)
                in DATE_TIME_FORMATS -> stored.toDoubleOrNull()?.let { serial -> dateTime(serial, numFmtId) }
                else -> null
            }
        return text?.let { FormattedNumber(it, raw = false) } ?: FormattedNumber(stored, raw = true)
    }

    private fun percent(
        number: DecimalNumber,
        fractionDigits: Int,
    ): String = number.shift(2).roundToScale(fractionDigits).toPlainString(fractionDigits = fractionDigits) + "%"

    private fun dateTime(
        serial: Double,
        numFmtId: Int,
    ): String? {
        if (serial < 0 || serial.isNaN() || serial > MAX_SERIAL) return null
        val totalSeconds = (serial * SECONDS_PER_DAY).roundToLong()
        val date = ExcelCalendar.dateOf(totalSeconds / SECONDS_PER_DAY, date1904)
        val time = ClockTime((totalSeconds % SECONDS_PER_DAY).toInt())
        return when (numFmtId) {
            DATE -> date.format(dateStyle)
            DAY_MONTH_YEAR -> "${date.day}-${date.monthName}-${date.shortYear}"
            DAY_MONTH -> "${date.day}-${date.monthName}"
            MONTH_YEAR -> "${date.monthName}-${date.shortYear}"
            TIME_12_MINUTES -> "${time.hour12}:${time.minutes} ${time.meridiem}"
            TIME_12_SECONDS -> "${time.hour12}:${time.minutes}:${time.seconds} ${time.meridiem}"
            TIME_24_MINUTES -> "${time.hour}:${time.minutes}"
            TIME_24_SECONDS -> "${time.hour}:${time.minutes}:${time.seconds}"
            else -> "${date.format(dateStyle)} ${time.hour}:${time.minutes}"
        }
    }

    private companion object {
        const val GENERAL = 0
        const val THOUSANDS = 3
        const val THOUSANDS_DECIMAL = 4
        const val PERCENT = 9
        const val PERCENT_DECIMAL = 10
        const val DATE = 14
        const val DAY_MONTH_YEAR = 15
        const val DAY_MONTH = 16
        const val MONTH_YEAR = 17
        const val TIME_12_MINUTES = 18
        const val TIME_12_SECONDS = 19
        const val TIME_24_MINUTES = 20
        const val TIME_24_SECONDS = 21
        const val DATE_TIME = 22
        val DATE_TIME_FORMATS = DATE..DATE_TIME
        const val GENERAL_SIGNIFICANT_DIGITS = 15
        const val SECONDS_PER_DAY = 86_400L

        // Excel이 날짜로 보여주는 마지막 일련번호(9999-12-31)
        const val MAX_SERIAL = 2_958_465.0
    }
}

private class ClockTime(
    secondOfDay: Int,
) {
    val hour = secondOfDay / SECONDS_PER_HOUR
    val minutes = (secondOfDay % SECONDS_PER_HOUR / SECONDS_PER_MINUTE).toString().padStart(2, '0')
    val seconds = (secondOfDay % SECONDS_PER_MINUTE).toString().padStart(2, '0')
    val hour12 = if (hour % HALF_DAY_HOURS == 0) HALF_DAY_HOURS else hour % HALF_DAY_HOURS
    val meridiem = if (hour < HALF_DAY_HOURS) "AM" else "PM"

    private companion object {
        const val SECONDS_PER_HOUR = 3600
        const val SECONDS_PER_MINUTE = 60
        const val HALF_DAY_HOURS = 12
    }
}
