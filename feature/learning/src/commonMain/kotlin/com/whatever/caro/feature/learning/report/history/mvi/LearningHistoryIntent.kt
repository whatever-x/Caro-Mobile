package com.whatever.caro.feature.learning.report.history.mvi

import com.whatever.caro.core.viewmodel.contract.UiIntent

sealed interface LearningHistoryIntent : UiIntent {
    data object Initialize : LearningHistoryIntent

    data object ClickBack : LearningHistoryIntent

    data object ClickAllPeriod : LearningHistoryIntent

    data object LoadMore : LearningHistoryIntent

    data object Retry : LearningHistoryIntent
}
