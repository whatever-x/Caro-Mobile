package com.whatever.caro.feature.learning.report.mvi

import com.whatever.caro.core.viewmodel.contract.UiSideEffect

sealed interface LearningReportSideEffect : UiSideEffect {
    data object NavigateBack : LearningReportSideEffect

    data class NavigateToHistory(
        val deckId: Long?,
        val deckName: String?,
        val startDate: String?,
        val endDate: String?,
    ) : LearningReportSideEffect

    data class NavigateToCardReviews(
        val deckId: Long,
        val cardId: Long,
        val startDate: String?,
        val endDate: String?,
    ) : LearningReportSideEffect
}
