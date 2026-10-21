package com.whatever.caro.feature.learning.report.card.mvi

import com.whatever.caro.core.viewmodel.contract.UiSideEffect

sealed interface CardReviewHistorySideEffect : UiSideEffect {
    data object NavigateBack : CardReviewHistorySideEffect
}
