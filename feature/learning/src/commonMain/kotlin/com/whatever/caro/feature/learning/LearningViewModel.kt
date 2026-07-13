package com.whatever.caro.feature.learning

import com.whatever.caro.core.data.repository.card.CardRepository
import com.whatever.caro.core.data.repository.study.StudySessionRepository
import com.whatever.caro.core.model.study.StudyCard
import com.whatever.caro.core.model.study.StudyEvaluation
import com.whatever.caro.core.model.study.StudySession
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.learning.model.LearningMode
import com.whatever.caro.feature.learning.mvi.LearningIntent
import com.whatever.caro.feature.learning.mvi.LearningSideEffect
import com.whatever.caro.feature.learning.mvi.LearningState

class LearningViewModel(
    private val deckId: Long,
    private val mode: LearningMode,
    private val repository: StudySessionRepository,
    private val cardRepository: CardRepository,
    exceptionFilter: ExceptionFilter,
) : BaseViewModel<LearningState, LearningIntent, LearningSideEffect>(LearningState(), exceptionFilter) {
    init {
        intent(LearningIntent.Load)
    }

    override suspend fun handleIntent(intent: LearningIntent) {
        when (intent) {
            LearningIntent.Load -> load()
            LearningIntent.FlipCard -> reduce { copy(isFlipped = !isFlipped) }
            is LearningIntent.Evaluate -> evaluate(intent)
            LearningIntent.RequestStop -> requestStop()
            LearningIntent.DismissStop -> reduce { copy(showStopDialog = false) }
            LearningIntent.ConfirmStop, LearningIntent.Close -> postSideEffect(LearningSideEffect.NavigateBack)
        }
    }

    private fun requestStop() {
        if (currentState.currentCard != null && !currentState.isCompleted) {
            reduce { copy(showStopDialog = true) }
        } else {
            postSideEffect(LearningSideEffect.NavigateBack)
        }
    }

    private suspend fun load() {
        reduce { copy(isLoading = true, errorMessage = null) }
        if (mode == LearningMode.ALL) {
            val cards = cardRepository.getCards(deckId).map { StudyCard(it.id, it.content.front, it.content.back) }
            reduce { copy(isLoading = false, totalCount = cards.size, cards = cards, isCompleted = cards.isEmpty()) }
            return
        }
        when (val session = repository.startDaily(deckId)) {
            is StudySession.InProgress -> {
                reduce {
                    copy(
                        isLoading = false,
                        sessionId = session.sessionId,
                        studiedBefore = session.studiedCardCount,
                        totalCount = session.totalCardCount,
                        cards = session.cards,
                    )
                }
            }

            is StudySession.Completed -> {
                reduce { copy(isLoading = false, totalCount = session.totalCardCount, isCompleted = true) }
            }

            StudySession.RestDay -> {
                reduce { copy(isLoading = false, isRestDay = true) }
            }
        }
    }

    private suspend fun evaluate(intent: LearningIntent.Evaluate) {
        val card = currentState.currentCard ?: return
        val all = currentState.evaluations + StudyEvaluation(card.id, intent.rating, intent.timeMs)
        if (currentState.index == currentState.cards.lastIndex) {
            if (mode == LearningMode.ALL) {
                reduce { copy(evaluations = all, isCompleted = true) }
            } else {
                reduce { copy(evaluations = all, isSubmitting = true) }
                repository.submit(currentState.sessionId, all)
                reduce { copy(isSubmitting = false, isCompleted = true) }
            }
        } else {
            reduce { copy(index = index + 1, isFlipped = false, evaluations = all) }
        }
    }

    override fun handleClientException(throwable: Throwable) {
        reduce { copy(isLoading = false, isSubmitting = false, errorMessage = throwable.message ?: "학습 정보를 불러오지 못했어요") }
    }
}
