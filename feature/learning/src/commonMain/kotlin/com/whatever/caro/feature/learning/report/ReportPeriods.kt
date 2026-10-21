package com.whatever.caro.feature.learning.report

import com.whatever.caro.core.model.report.ReportBucket
import com.whatever.caro.core.model.report.ReportPeriod
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * 리포트에서 쓰는 오늘 날짜. 기기 시간대 기준이다.
 */
fun interface ReportToday {
    fun today(): LocalDate
}

internal object SystemReportToday : ReportToday {
    @OptIn(ExperimentalTime::class)
    override fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
}

/**
 * 리포트의 기간 칩. ALL은 첫 학습일부터 오늘까지다.
 */
enum class ReportPeriodPreset { ALL, RECENT_7, CUSTOM }

/**
 * 기간 직접 설정의 빠른 선택. 모두 오늘까지 포함한다.
 */
enum class ReportQuickRange { THIS_WEEK, THIS_MONTH, RECENT_30 }

/**
 * 월 막대를 주차로 나눈 한 줄. [number]는 그 달의 몇째 주인지다.
 */
data class ReportWeek(
    val number: Int,
    val period: ReportPeriod,
    val reviewCount: Long,
)

internal object ReportPeriods {
    private const val RECENT_SHORT_DAYS = 7
    private const val RECENT_LONG_DAYS = 30
    private const val DAYS_PER_WEEK = 7

    /**
     * 오늘을 포함한 최근 7일. 전체와 직접 설정은 null을 돌려준다.
     */
    fun presetPeriod(
        preset: ReportPeriodPreset,
        today: LocalDate,
    ): ReportPeriod? =
        when (preset) {
            ReportPeriodPreset.RECENT_7 -> recent(today, RECENT_SHORT_DAYS)
            ReportPeriodPreset.ALL, ReportPeriodPreset.CUSTOM -> null
        }

    /**
     * 이번 주는 월요일부터, 이번 달은 1일부터, 최근 30일은 오늘 포함 30일. 모두 오늘에서 끝난다.
     */
    fun quickPeriod(
        range: ReportQuickRange,
        today: LocalDate,
    ): ReportPeriod =
        when (range) {
            ReportQuickRange.THIS_WEEK -> ReportPeriod(today.minus(DatePeriod(days = today.dayOfWeek.isoDayNumber - 1)), today)
            ReportQuickRange.THIS_MONTH -> ReportPeriod(LocalDate(today.year, today.month, 1), today)
            ReportQuickRange.RECENT_30 -> recent(today, RECENT_LONG_DAYS)
        }

    /**
     * 직접 설정한 범위가 조회할 수 있는지 본다. 하루만 고를 수 있고 미래 날짜와 뒤집힌 범위는 안 된다.
     */
    fun isSelectable(
        startDate: LocalDate?,
        endDate: LocalDate?,
        today: LocalDate,
    ): Boolean = startDate != null && endDate != null && startDate <= endDate && endDate <= today

    /**
     * 일 단위 막대를 월요일에 시작하는 주로 더한다. 주는 [period] 경계에서 자르고, 번호는 그 달 1일이 든 주가 1주차다.
     */
    fun splitWeeks(
        period: ReportPeriod,
        dayBuckets: List<ReportBucket>,
    ): List<ReportWeek> {
        val firstOfMonth = LocalDate(period.startDate.year, period.startDate.month, 1)
        val firstMonday = firstOfMonth.minus(DatePeriod(days = firstOfMonth.dayOfWeek.isoDayNumber - 1))
        val weeks = mutableListOf<ReportWeek>()
        var start = period.startDate
        while (start <= period.endDate) {
            val sunday = start.plus(DatePeriod(days = DayOfWeek.SUNDAY.isoDayNumber - start.dayOfWeek.isoDayNumber))
            val end = minOf(sunday, period.endDate)
            weeks +=
                ReportWeek(
                    number = (firstMonday.daysUntil(start) / DAYS_PER_WEEK) + 1,
                    period = ReportPeriod(start, end),
                    reviewCount = dayBuckets.filter { it.startDate in start..end }.sumOf { it.reviewCount },
                )
            start = end.plus(DatePeriod(days = 1))
        }
        return weeks
    }

    private fun recent(
        today: LocalDate,
        days: Int,
    ): ReportPeriod = ReportPeriod(startDate = today.minus(DatePeriod(days = days - 1)), endDate = today)
}
