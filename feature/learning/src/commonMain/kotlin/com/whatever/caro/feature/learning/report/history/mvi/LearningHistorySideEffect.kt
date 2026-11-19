package com.whatever.caro.feature.learning.report.history.mvi

import com.whatever.caro.core.viewmodel.contract.UiSideEffect

sealed interface LearningHistorySideEffect : UiSideEffect {
    data object NavigateBack : LearningHistorySideEffect
}
