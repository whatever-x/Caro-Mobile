package com.whatever.caro.feature.learning.report.mvi

import com.whatever.caro.core.model.learning.StudyRating
import com.whatever.caro.core.model.report.ReportBucket
import com.whatever.caro.core.model.report.ReportCardRecord
import com.whatever.caro.core.model.report.ReportDeckOption
import com.whatever.caro.core.model.report.ReportPeriod
import com.whatever.caro.core.model.report.StudyReportSummary
import com.whatever.caro.core.viewmodel.contract.UiState
import com.whatever.caro.feature.learning.report.ReportPeriodPreset
import com.whatever.caro.feature.learning.report.ReportQuickRange
import com.whatever.caro.feature.learning.report.ReportWeek
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDate

/**
 * 조회 조건. deckId가 null이면 전체이고, period가 null이면 첫 학습일부터 오늘까지다.
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
    val deckSheet: ReportDeckSheetState? = null,
    val dateEditor: ReportDateEditorState? = null,
    val monthDetail: ReportMonthDetail? = null,
    val backSteps: ImmutableList<ReportBackStep> = persistentListOf(),
    val customPeriod: ReportPeriod? = null,
) : UiState {
    val isDeckReport: Boolean
        get() = filter.deckId != null

    val hasMoreCards: Boolean
        get() = cardsCursor != null

    /** 뒤로 가기를 리포트 안에서 처리하는지. 아니면 리포트를 닫는다. */
    val handlesBack: Boolean
        get() = monthDetail != null || backSteps.isNotEmpty()
}

data class ReportDeckSheetState(
    val decks: ImmutableList<ReportDeckOption> = persistentListOf(),
    val decksCursor: String? = null,
    val hasLoadedDecks: Boolean = false,
    val isDecksLoading: Boolean = false,
    val hasDecksError: Boolean = false,
) {
    val hasMoreDecks: Boolean
        get() = hasLoadedDecks.not() || decksCursor != null
}

/**
 * 기간 직접 설정. 처음 누른 날짜가 시작일, 다음 날짜가 종료일이다. 날짜를 직접 바꾸면 빠른 선택이 풀린다.
 */
data class ReportDateEditorState(
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val visibleMonth: LocalDate,
    val today: LocalDate,
    val quickRange: ReportQuickRange? = null,
) {
    val canConfirm: Boolean
        get() = startDate != null && endDate != null
}

/**
 * 월 막대를 눌러 연 주차 목록. 그 달 기간을 한 번 더 불러와 일 단위 결과를 주로 더한다.
 */
data class ReportMonthDetail(
    val bucket: ReportBucket,
    val weeks: ImmutableList<ReportWeek>? = null,
    val hasError: Boolean = false,
)

/**
 * 막대나 주차를 눌러 기간을 바꾸기 전 상태. 뒤로 가기로 한 단계씩 돌아간다.
 */
data class ReportBackStep(
    val filter: ReportFilter,
    val openMonth: ReportBucket?,
)
