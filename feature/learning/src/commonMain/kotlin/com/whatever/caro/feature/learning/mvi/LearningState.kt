package com.whatever.caro.feature.learning.mvi

import com.whatever.caro.core.model.study.StudyCard
import com.whatever.caro.core.model.study.StudyEvaluation
import com.whatever.caro.core.viewmodel.contract.UiState

data class LearningState(
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val sessionId: Long = 0,
    val studiedBefore: Int = 0,
    val totalCount: Int = 0,
    val cards: List<StudyCard> = emptyList(),
    val index: Int = 0,
    val isFlipped: Boolean = false,
    val evaluations: List<StudyEvaluation> = emptyList(),
    val showStopDialog: Boolean = false,
    val isCompleted: Boolean = false,
    val isRestDay: Boolean = false,
    val errorMessage: String? = null,
) : UiState {
    val currentCard: StudyCard? get() = cards.getOrNull(index)
    val progress: Int get() = studiedBefore + index + if (isCompleted) 1 else 0
}
