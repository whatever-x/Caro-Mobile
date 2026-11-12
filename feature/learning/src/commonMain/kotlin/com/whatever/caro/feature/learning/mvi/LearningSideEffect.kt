package com.whatever.caro.feature.learning.mvi

import com.whatever.caro.core.viewmodel.contract.UiSideEffect

sealed interface LearningSideEffect : UiSideEffect {
    data object PopBackStack : LearningSideEffect

    data object NavigateToHome : LearningSideEffect

    data object ShowDailyCompletionAd : LearningSideEffect
}
