package com.whatever.caro.feature.learning.report.mvi

import com.whatever.caro.core.model.learning.StudyRating
import com.whatever.caro.core.model.report.ReportCardRecord
import com.whatever.caro.core.model.report.ReportDeckOption
import com.whatever.caro.core.model.report.ReportPeriod
import com.whatever.caro.core.model.report.StudyReportSummary
import com.whatever.caro.core.viewmodel.contract.UiState
import com.whatever.caro.feature.learning.report.ReportPeriodPreset
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDate

/**
 * 조회 조건. deckId가 null이면 전체이고, period가 null이면 전체 기간이다.
 */
data class ReportFilter(
    val deckId: Long?,
    val deckName: String?,
    val deckDeleted: Boolean,
    val preset: ReportPeriodPreset,
    val period: ReportPeriod?,
)

data class LearningReportState(
    val filter: ReportFilter,
    val summary: StudyReportSummary? = null,
    val isSummaryLoading: Boolean = true,
    val hasSummaryError: Boolean = false,
    val cardRating: StudyRating? = null,
    val cards: ImmutableList<ReportCardRecord> = persistentListOf(),
    val cardsCursor: String? = null,
    val isCardsLoading: Boolean = false,
    val hasCardsError: Boolean = false,
    val sheet: ReportSheetState? = null,
) : UiState {
    val isDeckReport: Boolean
        get() = filter.deckId != null

    val hasMoreCards: Boolean
        get() = cardsCursor != null
}

/**
 * 조회 조건 시트에서 아직 반영하지 않은 선택. 체크를 눌러야 [LearningReportState.filter]에 반영된다.
 */
data class ReportSheetState(
    val draft: ReportFilter,
    val decks: ImmutableList<ReportDeckOption> = persistentListOf(),
    val decksCursor: String? = null,
    val hasLoadedDecks: Boolean = false,
    val isDecksLoading: Boolean = false,
    val hasDecksError: Boolean = false,
    val dateEditor: ReportDateEditorState? = null,
) {
    val canApply: Boolean
        get() = draft.preset != ReportPeriodPreset.CUSTOM || draft.period != null

    val hasMoreDecks: Boolean
        get() = hasLoadedDecks.not() || decksCursor != null
}

/**
 * 기간 직접 설정. 처음 누른 날짜가 시작일, 다음 날짜가 종료일이다.
 */
data class ReportDateEditorState(
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val visibleMonth: LocalDate,
    val today: LocalDate,
) {
    val canConfirm: Boolean
        get() = startDate != null && endDate != null
}
