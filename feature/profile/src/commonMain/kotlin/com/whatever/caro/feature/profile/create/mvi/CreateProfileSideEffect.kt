package com.whatever.caro.feature.profile.create.mvi

import com.whatever.caro.core.viewmodel.contract.UiSideEffect

sealed interface CreateProfileSideEffect : UiSideEffect {
    data object NavigateBack : CreateProfileSideEffect

    data object NavigateHome : CreateProfileSideEffect
}
