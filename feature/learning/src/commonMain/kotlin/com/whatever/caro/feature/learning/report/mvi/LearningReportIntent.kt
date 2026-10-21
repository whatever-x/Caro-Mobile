package com.whatever.caro.feature.learning.report.mvi

import com.whatever.caro.core.model.learning.StudyRating
import com.whatever.caro.core.model.report.ReportDeckOption
import com.whatever.caro.core.viewmodel.contract.UiIntent
import com.whatever.caro.feature.learning.report.ReportPeriodPreset
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

    // 조회 조건 시트
    data object OpenSheet : LearningReportIntent

    data object DismissSheet : LearningReportIntent

    data object ApplySheet : LearningReportIntent

    data class SelectDraftPreset(
        val preset: ReportPeriodPreset,
    ) : LearningReportIntent

    data object SelectDraftAllDecks : LearningReportIntent

    data class SelectDraftDeck(
        val deck: ReportDeckOption,
    ) : LearningReportIntent

    data object LoadMoreDecks : LearningReportIntent

    // 기간 직접 설정
    data object CloseDateEditor : LearningReportIntent

    data class ClickDate(
        val date: LocalDate,
    ) : LearningReportIntent

    data class MoveMonth(
        val offset: Int,
    ) : LearningReportIntent

    data object ConfirmDateRange : LearningReportIntent
}
