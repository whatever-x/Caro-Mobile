package com.whatever.caro.feature.learning.report.card.mvi

import com.whatever.caro.core.viewmodel.contract.UiIntent

sealed interface CardReviewHistoryIntent : UiIntent {
    data object Initialize : CardReviewHistoryIntent

    data object ClickBack : CardReviewHistoryIntent

    data object ClickAllPeriod : CardReviewHistoryIntent

    data object LoadMore : CardReviewHistoryIntent

    data object Retry : CardReviewHistoryIntent
}
