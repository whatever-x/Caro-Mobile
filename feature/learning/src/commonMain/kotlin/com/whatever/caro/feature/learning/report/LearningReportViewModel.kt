package com.whatever.caro.feature.learning.report

import com.whatever.caro.core.data.repository.report.StudyReportRepository
import com.whatever.caro.core.data.util.suspendRunCatching
import com.whatever.caro.core.model.report.ReportBucket
import com.whatever.caro.core.model.report.ReportBucketUnit
import com.whatever.caro.core.model.report.ReportDeckOption
import com.whatever.caro.core.model.report.ReportPeriod
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.learning.report.mvi.LearningReportIntent
import com.whatever.caro.feature.learning.report.mvi.LearningReportSideEffect
import com.whatever.caro.feature.learning.report.mvi.LearningReportState
import com.whatever.caro.feature.learning.report.mvi.ReportBackStep
import com.whatever.caro.feature.learning.report.mvi.ReportDateEditorState
import com.whatever.caro.feature.learning.report.mvi.ReportDeckSheetState
import com.whatever.caro.feature.learning.report.mvi.ReportFilter
import com.whatever.caro.feature.learning.report.mvi.ReportMonthDetail
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * 학습 리포트
 *
 * 일반 진입은 전체 덱, 덱에서 들어오면 그 덱으로 열고 기간은 모두 "전체"다. 이전 선택을 저장해 다시 열지 않는다.
 * 기간 칩, 덱 선택, 기간 직접 설정은 고르는 즉시 다시 불러오고 새 조건을 반영하면 이전 요청은 취소한다.
 * 월 막대는 주차 목록을 열고, 주차나 주 막대를 누르면 기간을 그 주로 바꾼다. 이렇게 바꾼 기간은 뒤로 가기로 한 단계씩 돌아간다.
 */
class LearningReportViewModel(
    deckId: Long?,
    private val repository: StudyReportRepository,
    private val reportToday: ReportToday,
    exceptionFilter: ExceptionFilter,
) : BaseViewModel<LearningReportState, LearningReportIntent, LearningReportSideEffect>(
        initialState =
            LearningReportState(
                filter =
                    ReportFilter(
                        deckId = deckId,
                        deckName = null,
                        deckDeleted = false,
                        preset = ReportPeriodPreset.ALL,
                        period = null,
                    ),
            ),
        exceptionFilter = exceptionFilter,
    ) {
    private var summaryJob: Job? = null
    private var cardsJob: Job? = null
    private var decksJob: Job? = null
    private var monthJob: Job? = null
    private var hasInitialized = false

    // 시트를 닫았다 다시 열어도 이미 불러온 덱 목록은 그대로 쓴다
    private var cachedDecks: ReportDeckSheetState? = null

    override suspend fun handleIntent(intent: LearningReportIntent) {
        when (intent) {
            LearningReportIntent.Initialize -> {
                if (hasInitialized.not()) {
                    hasInitialized = true
                    loadReport()
                }
            }

            LearningReportIntent.ClickBack -> {
                goBack()
            }

            LearningReportIntent.ClickRetry -> {
                loadReport()
            }

            LearningReportIntent.ClickAllPeriod -> {
                applyFilter(currentState.filter.copy(preset = ReportPeriodPreset.ALL, period = null))
            }

            LearningReportIntent.ClickHistory -> {
                val filter = currentState.filter
                postSideEffect(
                    LearningReportSideEffect.NavigateToHistory(
                        deckId = filter.deckId,
                        deckName = filter.deckName,
                        startDate = filter.period?.startDate?.toString(),
                        endDate = filter.period?.endDate?.toString(),
                    ),
                )
            }

            is LearningReportIntent.ClickCard -> {
                val filter = currentState.filter
                val deckId = filter.deckId ?: return
                postSideEffect(
                    LearningReportSideEffect.NavigateToCardReviews(
                        deckId = deckId,
                        cardId = intent.cardId,
                        startDate = filter.period?.startDate?.toString(),
                        endDate = filter.period?.endDate?.toString(),
                    ),
                )
            }

            is LearningReportIntent.SelectCardRating -> {
                if (intent.rating == currentState.cardRating) return
                reduce { copy(cardRating = intent.rating) }
                loadCards(reset = true)
            }

            LearningReportIntent.LoadMoreCards -> {
                val state = currentState
                if (state.isCardsLoading.not() && state.hasCardsError.not() && state.hasMoreCards) {
                    loadCards(reset = false)
                }
            }

            LearningReportIntent.RetryCards -> {
                loadCards(reset = currentState.cards.isEmpty())
            }

            is LearningReportIntent.SelectPreset -> {
                selectPreset(intent.preset)
            }

            is LearningReportIntent.ClickBar -> {
                clickBar(intent.bucket)
            }

            is LearningReportIntent.ClickWeek -> {
                val month = currentState.monthDetail?.bucket ?: return
                drillDown(period = intent.week.period, openMonth = month)
            }

            LearningReportIntent.ClickMonthAll -> {
                val month = currentState.monthDetail?.bucket ?: return
                drillDown(period = month.toPeriod(), openMonth = month)
            }

            LearningReportIntent.RetryMonthDetail -> {
                currentState.monthDetail?.let { openMonth(it.bucket) }
            }

            LearningReportIntent.OpenDeckSheet -> {
                openDeckSheet()
            }

            LearningReportIntent.DismissDeckSheet -> {
                closeDeckSheet()
            }

            LearningReportIntent.SelectAllDecks -> {
                selectDeck(deckId = null, deckName = null, deckDeleted = false)
            }

            is LearningReportIntent.SelectDeck -> {
                selectDeck(deckId = intent.deck.deckId, deckName = intent.deck.name, deckDeleted = intent.deck.deleted)
            }

            LearningReportIntent.LoadMoreDecks -> {
                val sheet = currentState.deckSheet ?: return
                if (sheet.isDecksLoading.not() && sheet.hasMoreDecks) loadDecks()
            }

            LearningReportIntent.DismissDateEditor -> {
                reduce { copy(dateEditor = null) }
            }

            is LearningReportIntent.ClickDate -> {
                updateDateEditor { selectDate(intent.date) }
            }

            is LearningReportIntent.MoveMonth -> {
                updateDateEditor { moveMonth(intent.offset) }
            }

            is LearningReportIntent.SelectQuickRange -> {
                val period = ReportPeriods.quickPeriod(intent.range, reportToday.today())
                updateDateEditor {
                    copy(
                        startDate = period.startDate,
                        endDate = period.endDate,
                        visibleMonth = period.endDate.firstOfMonth(),
                        quickRange = intent.range,
                    )
                }
            }

            LearningReportIntent.ConfirmDateRange -> {
                confirmDateRange()
            }
        }
    }

    /**
     * 주차 목록이 열려 있으면 닫고, 막대로 바꾼 기간이 있으면 그 전으로 돌아가고, 없으면 리포트를 닫는다.
     */
    private fun goBack() {
        val state = currentState
        val step = state.backSteps.lastOrNull()
        when {
            state.monthDetail != null -> {
                closeMonth()
            }

            step != null -> {
                reduce { copy(backSteps = backSteps.dropLast(1).toImmutableList()) }
                showFilter(step.filter)
                step.openMonth?.let(::openMonth)
            }

            else -> {
                postSideEffect(LearningReportSideEffect.NavigateBack)
            }
        }
    }

    /**
     * 사용자가 고른 조건을 반영한다. 막대로 쌓인 단계는 지운다.
     */
    private fun applyFilter(filter: ReportFilter) {
        reduce { copy(backSteps = persistentListOf()) }
        showFilter(filter)
    }

    private fun showFilter(filter: ReportFilter) {
        closeMonth()
        reduce { copy(filter = filter, cardRating = null) }
        loadReport()
    }

    private fun drillDown(
        period: ReportPeriod,
        openMonth: ReportBucket?,
    ) {
        val step = ReportBackStep(filter = currentState.filter, openMonth = openMonth)
        reduce { copy(backSteps = (backSteps + step).toImmutableList()) }
        showFilter(currentState.filter.copy(preset = ReportPeriodPreset.CUSTOM, period = period))
    }

    private fun selectPreset(preset: ReportPeriodPreset) {
        val filter = currentState.filter
        if (preset == ReportPeriodPreset.CUSTOM) {
            val customPeriod = filter.period.takeIf { filter.preset == ReportPeriodPreset.CUSTOM }
            val today = reportToday.today()
            reduce {
                copy(
                    dateEditor =
                        ReportDateEditorState(
                            startDate = customPeriod?.startDate,
                            endDate = customPeriod?.endDate,
                            visibleMonth = (customPeriod?.endDate ?: today).firstOfMonth(),
                            today = today,
                        ),
                )
            }
            return
        }
        if (preset == filter.preset) return
        applyFilter(filter.copy(preset = preset, period = ReportPeriods.presetPeriod(preset, reportToday.today())))
    }

    private fun clickBar(bucket: ReportBucket) {
        when (currentState.summary?.range?.bucketUnit) {
            ReportBucketUnit.MONTH -> {
                if (currentState.monthDetail?.bucket == bucket) closeMonth() else openMonth(bucket)
            }

            ReportBucketUnit.WEEK -> {
                drillDown(period = bucket.toPeriod(), openMonth = null)
            }

            // 일 막대는 누를 수 없다
            ReportBucketUnit.DAY, null -> {
                Unit
            }
        }
    }

    private fun openMonth(bucket: ReportBucket) {
        monthJob?.cancel()
        val deckId = currentState.filter.deckId
        val period = bucket.toPeriod()
        reduce { copy(monthDetail = ReportMonthDetail(bucket = bucket)) }
        monthJob =
            launch {
                suspendRunCatching { repository.getSummary(deckId = deckId, period = period) }
                    .onSuccess { summary ->
                        // 한 달은 31일 이하라 서버가 항상 일 단위 막대로 응답한다.
                        val weeks = ReportPeriods.splitWeeks(period, summary.buckets).toImmutableList()
                        reduce { copy(monthDetail = monthDetail?.copy(weeks = weeks)) }
                    }.onFailure { throwable ->
                        reduce { copy(monthDetail = monthDetail?.copy(hasError = true)) }
                        throw throwable
                    }
            }
    }

    private fun closeMonth() {
        monthJob?.cancel()
        reduce { copy(monthDetail = null) }
    }

    private fun loadReport() {
        summaryJob?.cancel()
        val filter = currentState.filter
        reduce { copy(summary = null, isSummaryLoading = true, hasSummaryError = false) }
        summaryJob =
            launch {
                suspendRunCatching { repository.getSummary(deckId = filter.deckId, period = filter.period) }
                    .onSuccess { summary ->
                        reduce {
                            copy(
                                summary = summary,
                                isSummaryLoading = false,
                                filter =
                                    this.filter.copy(
                                        deckName = summary.scope.deckName,
                                        deckDeleted = summary.scope.deleted,
                                    ),
                            )
                        }
                    }.onFailure { throwable ->
                        reduce { copy(isSummaryLoading = false, hasSummaryError = true) }
                        throw throwable
                    }
            }
        if (filter.deckId != null) {
            loadCards(reset = true)
        } else {
            cardsJob?.cancel()
            reduce { copy(cards = persistentListOf(), cardsCursor = null, isCardsLoading = false, hasCardsError = false) }
        }
    }

    private fun loadCards(reset: Boolean) {
        cardsJob?.cancel()
        val state = currentState
        val deckId = state.filter.deckId ?: return
        val cursor = if (reset) null else state.cardsCursor
        reduce {
            copy(
                cards = if (reset) persistentListOf() else cards,
                cardsCursor = if (reset) null else cardsCursor,
                isCardsLoading = true,
                hasCardsError = false,
            )
        }
        cardsJob =
            launch {
                suspendRunCatching {
                    repository.getCards(
                        deckId = deckId,
                        period = state.filter.period,
                        lastRating = state.cardRating,
                        cursor = cursor,
                    )
                }.onSuccess { page ->
                    reduce {
                        copy(
                            cards = (cards + page.items).distinctBy { it.card.cardId }.toImmutableList(),
                            cardsCursor = page.nextCursor,
                            isCardsLoading = false,
                        )
                    }
                }.onFailure { throwable ->
                    reduce { copy(isCardsLoading = false, hasCardsError = true) }
                    throw throwable
                }
            }
    }

    private fun openDeckSheet() {
        val sheet = cachedDecks?.copy(hasDecksError = false, isDecksLoading = false) ?: ReportDeckSheetState()
        reduce { copy(deckSheet = sheet) }
        if (sheet.hasLoadedDecks.not()) loadDecks()
    }

    private fun closeDeckSheet() {
        decksJob?.cancel()
        cachedDecks = currentState.deckSheet?.copy(isDecksLoading = false)
        reduce { copy(deckSheet = null) }
    }

    /**
     * 덱을 고르면 기간은 그대로 두고 바로 적용한다. 같은 덱이면 닫기만 한다.
     */
    private fun selectDeck(
        deckId: Long?,
        deckName: String?,
        deckDeleted: Boolean,
    ) {
        closeDeckSheet()
        val filter = currentState.filter
        if (deckId == filter.deckId) return
        applyFilter(filter.copy(deckId = deckId, deckName = deckName, deckDeleted = deckDeleted))
    }

    private fun confirmDateRange() {
        val editor = currentState.dateEditor ?: return
        val start = editor.startDate ?: return
        val end = editor.endDate ?: return
        if (ReportPeriods.isSelectable(start, end, editor.today).not()) return
        reduce { copy(dateEditor = null) }
        applyFilter(currentState.filter.copy(preset = ReportPeriodPreset.CUSTOM, period = ReportPeriod(start, end)))
    }

    private fun loadDecks() {
        decksJob?.cancel()
        val cursor = currentState.deckSheet?.decksCursor
        updateDeckSheet { copy(isDecksLoading = true, hasDecksError = false) }
        decksJob =
            launch {
                suspendRunCatching { repository.getDeckOptions(cursor = cursor) }
                    .onSuccess { page ->
                        updateDeckSheet {
                            copy(
                                // 삭제한 덱은 목록에 두지 않는다
                                decks =
                                    (
                                        decks +
                                            page.items.filterNot(
                                                ReportDeckOption::deleted,
                                            )
                                    ).distinctBy(ReportDeckOption::deckId).toImmutableList(),
                                decksCursor = page.nextCursor,
                                hasLoadedDecks = true,
                                isDecksLoading = false,
                            )
                        }
                    }.onFailure { throwable ->
                        updateDeckSheet { copy(isDecksLoading = false, hasDecksError = true) }
                        throw throwable
                    }
            }
    }

    private fun updateDeckSheet(block: ReportDeckSheetState.() -> ReportDeckSheetState) {
        reduce { copy(deckSheet = deckSheet?.block()) }
    }

    private fun updateDateEditor(block: ReportDateEditorState.() -> ReportDateEditorState) {
        reduce { copy(dateEditor = dateEditor?.block()) }
    }
}

private fun ReportBucket.toPeriod(): ReportPeriod = ReportPeriod(startDate = startDate, endDate = endDate)

private fun LocalDate.firstOfMonth(): LocalDate = LocalDate(year, month, 1)

private fun ReportDateEditorState.selectDate(date: LocalDate): ReportDateEditorState {
    if (date > today) return this
    val start = startDate
    return when {
        start == null || endDate != null -> copy(startDate = date, endDate = null, quickRange = null)

        // 시작일보다 앞선 날짜를 누르면 그 날짜부터 다시 고른다
        date < start -> copy(startDate = date, endDate = null, quickRange = null)

        else -> copy(endDate = date, quickRange = null)
    }
}

private fun ReportDateEditorState.moveMonth(offset: Int): ReportDateEditorState {
    val moved = visibleMonth.plus(DatePeriod(months = offset))
    return if (moved > today.firstOfMonth()) this else copy(visibleMonth = moved)
}
