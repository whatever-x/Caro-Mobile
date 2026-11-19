package com.whatever.caro.feature.learning.report.mvi

import com.whatever.caro.core.model.learning.StudyRating
import com.whatever.caro.core.model.report.ReportBucket
import com.whatever.caro.core.model.report.ReportDeckOption
import com.whatever.caro.core.viewmodel.contract.UiIntent
import com.whatever.caro.feature.learning.report.ReportPeriodPreset
import com.whatever.caro.feature.learning.report.ReportQuickRange
import com.whatever.caro.feature.learning.report.ReportWeek
import kotlinx.datetime.LocalDate

sealed interface LearningReportIntent : UiIntent {
    data object Initialize : LearningReportIntent

    data object ClickBack : LearningReportIntent

    data object ClickRetry : LearningReportIntent

    data object ClickAllPeriod : LearningReportIntent

    data object ClickHistory : LearningReportIntent

    data class ClickCard(
        val cardId: Long,
    ) : LearningReportIntent

    data class SelectCardRating(
        val rating: StudyRating?,
    ) : LearningReportIntent

    data object LoadMoreCards : LearningReportIntent

    data object RetryCards : LearningReportIntent

    data class SelectPreset(
        val preset: ReportPeriodPreset,
    ) : LearningReportIntent

    // 차트
    data class ClickBar(
        val bucket: ReportBucket,
    ) : LearningReportIntent

    data class ClickWeek(
        val week: ReportWeek,
    ) : LearningReportIntent

    data object ClickMonthAll : LearningReportIntent

    data object RetryMonthDetail : LearningReportIntent

    // 덱 선택 시트
    data object OpenDeckSheet : LearningReportIntent

    data object DismissDeckSheet : LearningReportIntent

    data object SelectAllDecks : LearningReportIntent

    data class SelectDeck(
        val deck: ReportDeckOption,
    ) : LearningReportIntent

    data object LoadMoreDecks : LearningReportIntent

    // 기간 직접 설정 시트
    data object DismissDateEditor : LearningReportIntent

    data class ClickDate(
        val date: LocalDate,
    ) : LearningReportIntent

    data class MoveMonth(
        val offset: Int,
    ) : LearningReportIntent

    data class SelectQuickRange(
        val range: ReportQuickRange,
    ) : LearningReportIntent

    data object ConfirmDateRange : LearningReportIntent
}
