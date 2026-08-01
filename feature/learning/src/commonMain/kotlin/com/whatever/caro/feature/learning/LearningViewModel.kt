package com.whatever.caro.feature.learning

import com.whatever.caro.core.data.repository.deck.DeckRepository
import com.whatever.caro.core.data.repository.study.StudySessionRepository
import com.whatever.caro.core.model.exception.CaroException
import com.whatever.caro.core.model.learning.DailyStudyStartResult
import com.whatever.caro.core.model.learning.LearningMode
import com.whatever.caro.core.model.learning.StudyCard
import com.whatever.caro.core.model.learning.StudyEvaluation
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.core.viewmodel.ExceptionFilter
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

    override fun handleClientException(throwable: Throwable) {
        reduce {
            copy(
                isLoading = false,
                isSubmitting = false,
                showStopDialog = false,
                errorMessage = if (throwable is CaroException) throwable.message else null,
                isShowErrorDialog = true,
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
            LearningIntent.ClickNavigateToHome -> postSideEffect(LearningSideEffect.NavigateToHome)
        }
    }

    private fun confirmError() {
        if (!currentState.isShowErrorDialog) return
        reduce { copy(isShowErrorDialog = false) }
        postSideEffect(LearningSideEffect.PopBackStack)
    }

    private suspend fun confirmStop() {
        if (isStopConfirmed) return
        isStopConfirmed = true
        val evaluations = currentState.evaluations
        try {
            if (mode == LearningMode.DAILY && evaluations.isNotEmpty()) {
                reduce { copy(isSubmitting = true) }
                repository.submit(
                    sessionId = currentState.sessionId,
                    evaluations = evaluations,
                    idempotencyKey = newUuid(),
                )
                reduce { copy(isSubmitting = false) }
            }
            postSideEffect(LearningSideEffect.PopBackStack)
        } catch (throwable: Throwable) {
            isStopConfirmed = false
            throw throwable
        }
    }

    private fun requestStop() {
        if (currentState.isShowErrorDialog) return
        if (currentState.currentCard != null && !currentState.isCompleted) {
            reduce { copy(showStopDialog = true) }
        } else {
            postSideEffect(LearningSideEffect.PopBackStack)
        }
    }

    private suspend fun load() {
        reduce { copy(isLoading = true, errorMessage = null) }
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
        when (
            val result =
                repository.startDaily(
                    deckId = deckId,
                    idempotencyKey = newUuid(),
                )
        ) {
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
        if (currentState.isSubmitting || currentState.isShowErrorDialog) return
        val card = currentState.currentCard ?: return
        val all =
            currentState.evaluations + StudyEvaluation(card.id, intent.rating, elapsedCardTimeMs())
        if (currentState.index == currentState.cards.lastIndex) {
            if (mode == LearningMode.ALL) {
                reduce { copy(evaluations = all, isCompleted = true) }
            } else {
                reduce { copy(evaluations = all, isSubmitting = true) }
                val ratingCounts =
                    repository.submit(
                        sessionId = currentState.sessionId,
                        evaluations = all,
                        idempotencyKey = newUuid(),
                    )
                reduce {
                    copy(
                        isSubmitting = false,
                        isCompleted = true,
                        ratingCounts = ratingCounts,
                    )
                }
            }
        } else {
            reduce { copy(index = index + 1, isFlipped = false, evaluations = all) }
            startCardTimer()
        }
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

    @OptIn(ExperimentalUuidApi::class)
    private fun newUuid(): String = Uuid.random().toString()
}
