package com.whatever.caro.feature.learning.report.history

import com.whatever.caro.core.data.repository.report.StudyReportRepository
import com.whatever.caro.core.data.util.suspendRunCatching
import com.whatever.caro.core.model.report.ReportPeriod
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.learning.report.history.mvi.LearningHistoryIntent
import com.whatever.caro.feature.learning.report.history.mvi.LearningHistorySideEffect
import com.whatever.caro.feature.learning.report.history.mvi.LearningHistoryState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.datetime.LocalDate

/**
 * 리포트에서 고른 덱과 기간의 학습 기록. "전체 기간 보기"는 기간만 바꾸고 덱은 그대로 둔다.
 */
class LearningHistoryViewModel(
    deckId: Long?,
    deckName: String?,
    startDate: String?,
    endDate: String?,
    private val repository: StudyReportRepository,
    exceptionFilter: ExceptionFilter,
) : BaseViewModel<LearningHistoryState, LearningHistoryIntent, LearningHistorySideEffect>(
        initialState =
            LearningHistoryState(
                deckId = deckId,
                deckName = deckName,
                period = periodOf(startDate, endDate),
            ),
        exceptionFilter = exceptionFilter,
    ) {
    private var loadJob: Job? = null
    private var hasInitialized = false

    override suspend fun handleIntent(intent: LearningHistoryIntent) {
        when (intent) {
            LearningHistoryIntent.Initialize -> {
                if (hasInitialized.not()) {
                    hasInitialized = true
                    load(reset = true)
                }
            }

            LearningHistoryIntent.ClickBack -> {
                postSideEffect(LearningHistorySideEffect.NavigateBack)
            }

            LearningHistoryIntent.ClickAllPeriod -> {
                reduce { copy(period = null) }
                load(reset = true)
            }

            LearningHistoryIntent.LoadMore -> {
                val state = currentState
                if (state.isLoading.not() && state.hasError.not() && state.hasMore) load(reset = false)
            }

            LearningHistoryIntent.Retry -> {
                load(reset = currentState.sessions.isEmpty())
            }
        }
    }

    private fun load(reset: Boolean) {
        loadJob?.cancel()
        val state = currentState
        val cursor = if (reset) null else state.cursor
        reduce {
            copy(
                sessions = if (reset) persistentListOf() else sessions,
                cursor = if (reset) null else this.cursor,
                hasLoaded = if (reset) false else hasLoaded,
                isLoading = true,
                hasError = false,
            )
        }
        loadJob =
            launch {
                suspendRunCatching { repository.getSessions(deckId = state.deckId, period = state.period, cursor = cursor) }
                    .onSuccess { page ->
                        reduce {
                            copy(
                                sessions = (sessions + page.items).distinctBy { it.sessionId }.toImmutableList(),
                                cursor = page.nextCursor,
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

internal fun periodOf(
    startDate: String?,
    endDate: String?,
): ReportPeriod? {
    val start = startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
    val end = endDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
    return ReportPeriod(startDate = start, endDate = end)
}
