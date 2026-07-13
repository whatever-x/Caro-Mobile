package com.whatever.caro.feature.learning.mvi

import com.whatever.caro.core.model.study.StudyRating
import com.whatever.caro.core.viewmodel.contract.UiIntent

sealed interface LearningIntent : UiIntent {
    data object Load : LearningIntent

    data object FlipCard : LearningIntent

    data class Evaluate(
        val rating: StudyRating,
        val timeMs: Int = 0,
    ) : LearningIntent

    data object RequestStop : LearningIntent

    data object DismissStop : LearningIntent

    data object ConfirmStop : LearningIntent

    data object Close : LearningIntent
}
