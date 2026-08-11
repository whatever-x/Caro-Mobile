package com.whatever.caro.feature.learning

import com.whatever.caro.core.data.repository.deck.DeckRepository
import com.whatever.caro.core.data.repository.study.StudySessionRepository
import com.whatever.caro.core.model.exception.CaroServerException
import com.whatever.caro.core.model.exception.NetworkException
import com.whatever.caro.core.model.learning.DailyStudyStartResult
import com.whatever.caro.core.model.learning.LearningMode
import com.whatever.caro.core.model.learning.StudyCard
import com.whatever.caro.core.model.learning.StudyEvaluation
import com.whatever.caro.core.model.learning.StudyRatingCounts
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.learning.mvi.LearningError
import com.whatever.caro.feature.learning.mvi.LearningIntent
import com.whatever.caro.feature.learning.mvi.LearningSideEffect
import com.whatever.caro.feature.learning.mvi.LearningState
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class LearningViewModel(
    private val deckId: Long,
    private val mode: LearningMode,
    private val repository: StudySessionRepository,
    private val deckRepository: DeckRepository,
    exceptionFilter: ExceptionFilter,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) : BaseViewModel<LearningState, LearningIntent, LearningSideEffect>(
        LearningState(mode = mode),
        exceptionFilter,
    ) {
    private var cardStartedAt: TimeMark? = null
    private var isStopConfirmed = false

    /** 실패한 서버 호출을 그대로 다시 실행하기 위한 진입점. 오류 다이얼로그의 재시도가 이 값을 호출한다. */
    private var retryAction: (suspend () -> Unit)? = null

    /** 재시도해도 서버가 같은 요청으로 인식하도록 성공 전까지 유지하는 멱등키. */
    private var pendingIdempotencyKey: String? = null

    override fun handleClientException(throwable: Throwable) {
        reduce {
            copy(
                isLoading = false,
                isSubmitting = false,
                showStopDialog = false,
                error = throwable.toLearningError(),
            )
        }
    }

    override suspend fun handleIntent(intent: LearningIntent) {
        when (intent) {
            LearningIntent.Load -> load()
            LearningIntent.FlipCard -> reduce { copy(isFlipped = !isFlipped) }
            is LearningIntent.Evaluate -> evaluate(intent)
            LearningIntent.RequestStop -> requestStop()
            LearningIntent.DismissStop -> reduce { copy(showStopDialog = false) }
            LearningIntent.ConfirmStop -> confirmStop()
            LearningIntent.ConfirmError -> confirmError()
            LearningIntent.RetryError -> retryError()
            LearningIntent.ClickNavigateToHome -> postSideEffect(LearningSideEffect.NavigateToHome)
        }
    }

    private fun confirmError() {
        if (currentState.error == null) return
        reduce { copy(error = null) }
        postSideEffect(LearningSideEffect.PopBackStack)
    }

    private suspend fun retryError() {
        if (currentState.error == null) return
        val action = retryAction ?: return
        reduce { copy(error = null) }
        action()
    }

    private suspend fun confirmStop() {
        if (isStopConfirmed) return
        isStopConfirmed = true
        if (mode != LearningMode.DAILY || currentState.evaluations.isEmpty()) {
            postSideEffect(LearningSideEffect.PopBackStack)
            return
        }
        stopAndSubmit()
    }

    private suspend fun stopAndSubmit() {
        retryAction = ::stopAndSubmit
        reduce { copy(isSubmitting = true) }
        submitEvaluations()
        reduce { copy(isSubmitting = false, showStopDialog = false) }
        postSideEffect(LearningSideEffect.PopBackStack)
    }

    private fun requestStop() {
        if (currentState.error != null) return
        if (currentState.currentCard != null && !currentState.isCompleted) {
            reduce { copy(showStopDialog = true) }
        } else {
            postSideEffect(LearningSideEffect.PopBackStack)
        }
    }

    private suspend fun load() {
        retryAction = ::load
        reduce { copy(isLoading = true, error = null) }
        if (mode == LearningMode.ALL) {
            val cards =
                deckRepository
                    .getDeckCards(deckId)
                    .map { StudyCard(it.id, it.content.front, it.content.back) }
            reduce {
                copy(
                    isLoading = false,
                    totalCount = cards.size,
                    cards = cards,
                    isCompleted = cards.isEmpty(),
                )
            }
            startCardTimer()
            return
        }
        val result =
            repository.startDaily(
                deckId = deckId,
                idempotencyKey = idempotencyKey(),
            )
        pendingIdempotencyKey = null
        when (result) {
            is DailyStudyStartResult.Started -> {
                val session = result.session
                reduce {
                    copy(
                        isLoading = false,
                        sessionId = session.sessionId,
                        studiedBefore = session.studiedCardCount,
                        totalCount = session.totalCardCount,
                        cards = session.cards,
                    )
                }
                startCardTimer()
            }

            is DailyStudyStartResult.Completed -> {
                reduce {
                    copy(
                        isLoading = false,
                        totalCount = result.totalCardCount,
                        isCompleted = true,
                    )
                }
            }

            DailyStudyStartResult.RestDay -> {
                reduce { copy(isLoading = false) }
                postSideEffect(LearningSideEffect.PopBackStack)
            }
        }
    }

    private suspend fun evaluate(intent: LearningIntent.Evaluate) {
        if (currentState.isSubmitting || currentState.error != null) return
        val card = currentState.currentCard ?: return
        val all =
            currentState.evaluations + StudyEvaluation(card.id, intent.rating, elapsedCardTimeMs())
        if (currentState.index != currentState.cards.lastIndex) {
            reduce { copy(index = index + 1, isFlipped = false, evaluations = all) }
            startCardTimer()
            return
        }
        if (mode == LearningMode.ALL) {
            reduce { copy(evaluations = all, isCompleted = true) }
            return
        }
        reduce { copy(evaluations = all) }
        completeSession()
    }

    private suspend fun completeSession() {
        retryAction = ::completeSession
        reduce { copy(isSubmitting = true) }
        val ratingCounts = submitEvaluations()
        reduce {
            copy(
                isSubmitting = false,
                isCompleted = true,
                ratingCounts = ratingCounts,
            )
        }
    }

    private suspend fun submitEvaluations(): StudyRatingCounts {
        val ratingCounts =
            repository.submit(
                sessionId = currentState.sessionId,
                evaluations = currentState.evaluations,
                idempotencyKey = idempotencyKey(),
            )
        pendingIdempotencyKey = null
        return ratingCounts
    }

    private fun startCardTimer() {
        cardStartedAt = currentState.currentCard?.let { timeSource.markNow() }
    }

    private fun elapsedCardTimeMs(): Int =
        cardStartedAt
            ?.elapsedNow()
            ?.inWholeMilliseconds
            ?.coerceIn(0L, Int.MAX_VALUE.toLong())
            ?.toInt()
            ?: 0

    private fun idempotencyKey(): String = pendingIdempotencyKey ?: newUuid().also { pendingIdempotencyKey = it }

    @OptIn(ExperimentalUuidApi::class)
    private fun newUuid(): String = Uuid.random().toString()
}

private fun Throwable.toLearningError(): LearningError =
    when (this) {
        is CaroServerException -> LearningError.Server(message)
        is NetworkException -> LearningError.Network
        else -> LearningError.Unknown
    }
