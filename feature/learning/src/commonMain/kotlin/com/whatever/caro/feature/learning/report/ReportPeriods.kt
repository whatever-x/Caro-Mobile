package com.whatever.caro.feature.learning.report

import com.whatever.caro.core.model.report.ReportPeriod
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
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
 * 조회 기간 선택. ALL은 "전체 기간 보기"로만 고른다.
 */
enum class ReportPeriodPreset { RECENT_7, RECENT_30, CUSTOM, ALL }

internal object ReportPeriods {
    private const val RECENT_SHORT_DAYS = 7
    private const val RECENT_LONG_DAYS = 30

    /**
     * 오늘을 포함한 최근 N일. 직접 설정과 전체 기간은 null을 돌려준다.
     */
    fun presetPeriod(
        preset: ReportPeriodPreset,
        today: LocalDate,
    ): ReportPeriod? =
        when (preset) {
            ReportPeriodPreset.RECENT_7 -> recent(today, RECENT_SHORT_DAYS)
            ReportPeriodPreset.RECENT_30 -> recent(today, RECENT_LONG_DAYS)
            ReportPeriodPreset.CUSTOM, ReportPeriodPreset.ALL -> null
        }

    /**
     * 직접 설정한 범위가 조회할 수 있는지 본다. 하루만 고를 수 있고 미래 날짜와 뒤집힌 범위는 안 된다.
     */
    fun isSelectable(
        startDate: LocalDate?,
        endDate: LocalDate?,
        today: LocalDate,
    ): Boolean = startDate != null && endDate != null && startDate <= endDate && endDate <= today

    private fun recent(
        today: LocalDate,
        days: Int,
    ): ReportPeriod = ReportPeriod(startDate = today.minus(DatePeriod(days = days - 1)), endDate = today)
}
