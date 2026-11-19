package com.whatever.caro.feature.learning.report.card

import com.whatever.caro.core.data.repository.report.StudyReportRepository
import com.whatever.caro.core.data.util.suspendRunCatching
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.learning.report.card.mvi.CardReviewHistoryIntent
import com.whatever.caro.feature.learning.report.card.mvi.CardReviewHistorySideEffect
import com.whatever.caro.feature.learning.report.card.mvi.CardReviewHistoryState
import com.whatever.caro.feature.learning.report.history.periodOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job

/**
 * 덱 리포트에서 고른 카드의 평가 이력. 카드 내용은 지금 내용이고 삭제된 카드는 본문 없이 보여준다.
 */
class CardReviewHistoryViewModel(
    private val deckId: Long,
    private val cardId: Long,
    startDate: String?,
    endDate: String?,
    private val repository: StudyReportRepository,
    exceptionFilter: ExceptionFilter,
) : BaseViewModel<CardReviewHistoryState, CardReviewHistoryIntent, CardReviewHistorySideEffect>(
        initialState = CardReviewHistoryState(period = periodOf(startDate, endDate)),
        exceptionFilter = exceptionFilter,
    ) {
    private var loadJob: Job? = null
    private var hasInitialized = false

    override suspend fun handleIntent(intent: CardReviewHistoryIntent) {
        when (intent) {
            CardReviewHistoryIntent.Initialize -> {
                if (hasInitialized.not()) {
                    hasInitialized = true
                    load(reset = true)
                }
            }

            CardReviewHistoryIntent.ClickBack -> {
                postSideEffect(CardReviewHistorySideEffect.NavigateBack)
            }

            CardReviewHistoryIntent.ClickAllPeriod -> {
                reduce { copy(period = null) }
                load(reset = true)
            }

            CardReviewHistoryIntent.LoadMore -> {
                val state = currentState
                if (state.isLoading.not() && state.hasError.not() && state.hasMore) load(reset = false)
            }

            CardReviewHistoryIntent.Retry -> {
                load(reset = currentState.reviews.isEmpty())
            }
        }
    }

    private fun load(reset: Boolean) {
        loadJob?.cancel()
        val state = currentState
        val cursor = if (reset) null else state.cursor
        reduce {
            copy(
                reviews = if (reset) persistentListOf() else reviews,
                cursor = if (reset) null else this.cursor,
                hasLoaded = if (reset) false else hasLoaded,
                isLoading = true,
                hasError = false,
            )
        }
        loadJob =
            launch {
                suspendRunCatching { repository.getCardReviews(deckId = deckId, cardId = cardId, period = state.period, cursor = cursor) }
                    .onSuccess { result ->
                        reduce {
                            copy(
                                card = result.card,
                                reviews = (reviews + result.page.items).distinctBy { it.reviewId }.toImmutableList(),
                                cursor = result.page.nextCursor,
                                hasLoaded = true,
                                isLoading = false,
                            )
                        }
                    }.onFailure { throwable ->
                        reduce { copy(isLoading = false, hasError = true) }
                        throw throwable
                    }
            }
    }
}
