package com.whatever.caro.feature.learning.report

import com.whatever.caro.core.data.repository.report.StudyReportRepository
import com.whatever.caro.core.data.util.suspendRunCatching
import com.whatever.caro.core.model.report.ReportDeckOption
import com.whatever.caro.core.model.report.ReportPeriod
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.learning.report.mvi.LearningReportIntent
import com.whatever.caro.feature.learning.report.mvi.LearningReportSideEffect
import com.whatever.caro.feature.learning.report.mvi.LearningReportState
import com.whatever.caro.feature.learning.report.mvi.ReportDateEditorState
import com.whatever.caro.feature.learning.report.mvi.ReportFilter
import com.whatever.caro.feature.learning.report.mvi.ReportSheetState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * 학습 리포트
 *
 * 일반 진입은 전체·최근 7일, 덱에서 들어오면 그 덱·최근 7일로 연다. 이전 선택을 저장해 다시 열지 않는다.
 * 조회 조건 시트에서 고른 값은 체크로 반영할 때만 리포트를 다시 불러온다. 새 조건을 반영하면 이전 요청은 취소한다.
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
                        preset = ReportPeriodPreset.RECENT_7,
                        period = ReportPeriods.presetPeriod(ReportPeriodPreset.RECENT_7, reportToday.today()),
                    ),
            ),
        exceptionFilter = exceptionFilter,
    ) {
    private var summaryJob: Job? = null
    private var cardsJob: Job? = null
    private var decksJob: Job? = null
    private var hasInitialized = false

    // 시트를 닫았다 다시 열어도 이미 불러온 덱 목록은 그대로 쓴다
    private var cachedDecks: ReportSheetState? = null

    override suspend fun handleIntent(intent: LearningReportIntent) {
        when (intent) {
            LearningReportIntent.Initialize -> {
                if (hasInitialized.not()) {
                    hasInitialized = true
                    loadReport()
                }
            }

            LearningReportIntent.ClickBack -> {
                postSideEffect(LearningReportSideEffect.NavigateBack)
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

            LearningReportIntent.OpenSheet -> {
                openSheet()
            }

            LearningReportIntent.DismissSheet -> {
                closeSheet()
            }

            LearningReportIntent.ApplySheet -> {
                applySheet()
            }

            is LearningReportIntent.SelectDraftPreset -> {
                selectDraftPreset(intent.preset)
            }

            LearningReportIntent.SelectDraftAllDecks -> {
                updateDraft { copy(deckId = null, deckName = null, deckDeleted = false) }
            }

            is LearningReportIntent.SelectDraftDeck -> {
                updateDraft { copy(deckId = intent.deck.deckId, deckName = intent.deck.name, deckDeleted = intent.deck.deleted) }
            }

            LearningReportIntent.LoadMoreDecks -> {
                val sheet = currentState.sheet ?: return
                if (sheet.isDecksLoading.not() && sheet.hasMoreDecks) loadDecks()
            }

            LearningReportIntent.CloseDateEditor -> {
                updateSheet { copy(dateEditor = null) }
            }

            is LearningReportIntent.ClickDate -> {
                updateDateEditor { selectDate(intent.date) }
            }

            is LearningReportIntent.MoveMonth -> {
                updateDateEditor { moveMonth(intent.offset) }
            }

            LearningReportIntent.ConfirmDateRange -> {
                confirmDateRange()
            }
        }
    }

    private fun applyFilter(filter: ReportFilter) {
        reduce { copy(filter = filter, cardRating = null) }
        loadReport()
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

    private fun openSheet() {
        val cached = cachedDecks
        val sheet =
            cached?.copy(draft = currentState.filter, dateEditor = null, hasDecksError = false, isDecksLoading = false)
                ?: ReportSheetState(draft = currentState.filter)
        reduce { copy(sheet = sheet) }
        if (sheet.hasLoadedDecks.not()) loadDecks()
    }

    private fun closeSheet() {
        decksJob?.cancel()
        cachedDecks = currentState.sheet?.copy(isDecksLoading = false)
        reduce { copy(sheet = null) }
    }

    private fun applySheet() {
        val sheet = currentState.sheet ?: return
        if (sheet.canApply.not()) return
        val draft = sheet.draft
        val current = currentState.filter
        closeSheet()
        // 같은 조건이면 닫기만 한다
        if (draft.deckId == current.deckId && draft.period == current.period && draft.preset == current.preset) return
        applyFilter(draft)
    }

    private fun selectDraftPreset(preset: ReportPeriodPreset) {
        val sheet = currentState.sheet ?: return
        val today = reportToday.today()
        if (preset == ReportPeriodPreset.CUSTOM) {
            val customPeriod = sheet.draft.period.takeIf { sheet.draft.preset == ReportPeriodPreset.CUSTOM }
            val baseDate = customPeriod?.endDate ?: today
            updateSheet {
                copy(
                    dateEditor =
                        ReportDateEditorState(
                            startDate = customPeriod?.startDate,
                            endDate = customPeriod?.endDate,
                            visibleMonth = LocalDate(baseDate.year, baseDate.month, 1),
                            today = today,
                        ),
                )
            }
            return
        }
        updateDraft { copy(preset = preset, period = ReportPeriods.presetPeriod(preset, today)) }
    }

    private fun confirmDateRange() {
        val editor = currentState.sheet?.dateEditor ?: return
        val start = editor.startDate ?: return
        val end = editor.endDate ?: return
        if (ReportPeriods.isSelectable(start, end, editor.today).not()) return
        updateSheet {
            copy(
                draft = draft.copy(preset = ReportPeriodPreset.CUSTOM, period = ReportPeriod(start, end)),
                dateEditor = null,
            )
        }
    }

    private fun loadDecks() {
        decksJob?.cancel()
        val cursor = currentState.sheet?.decksCursor
        updateSheet { copy(isDecksLoading = true, hasDecksError = false) }
        decksJob =
            launch {
                suspendRunCatching { repository.getDeckOptions(cursor = cursor) }
                    .onSuccess { page ->
                        updateSheet {
                            copy(
                                decks = (decks + page.items).distinctBy(ReportDeckOption::deckId).toImmutableList(),
                                decksCursor = page.nextCursor,
                                hasLoadedDecks = true,
                                isDecksLoading = false,
                            )
                        }
                    }.onFailure { throwable ->
                        updateSheet { copy(isDecksLoading = false, hasDecksError = true) }
                        throw throwable
                    }
            }
    }

    private fun updateSheet(block: ReportSheetState.() -> ReportSheetState) {
        reduce { copy(sheet = sheet?.block()) }
    }

    private fun updateDraft(block: ReportFilter.() -> ReportFilter) {
        updateSheet { copy(draft = draft.block()) }
    }

    private fun updateDateEditor(block: ReportDateEditorState.() -> ReportDateEditorState) {
        updateSheet { copy(dateEditor = dateEditor?.block()) }
    }
}

private fun ReportDateEditorState.selectDate(date: LocalDate): ReportDateEditorState {
    if (date > today) return this
    val start = startDate
    return when {
        start == null || endDate != null -> copy(startDate = date, endDate = null)

        // 시작일보다 앞선 날짜를 누르면 그 날짜부터 다시 고른다
        date < start -> copy(startDate = date, endDate = null)

        else -> copy(endDate = date)
    }
}

private fun ReportDateEditorState.moveMonth(offset: Int): ReportDateEditorState {
    val moved = visibleMonth.plus(DatePeriod(months = offset))
    val todayMonth = LocalDate(today.year, today.month, 1)
    return if (moved > todayMonth) this else copy(visibleMonth = moved)
}
