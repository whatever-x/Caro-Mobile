package com.whatever.caro.feature.profile.edit.mvi

import com.whatever.caro.core.viewmodel.contract.UiSideEffect

sealed interface EditProfileSideEffect : UiSideEffect {
    data object NavigateBack : EditProfileSideEffect

    data object ShowNicknameCheckError : EditProfileSideEffect

    data object ShowRandomNicknameError : EditProfileSideEffect

    data object ShowUpdateNicknameError : EditProfileSideEffect

    data object ShowNetworkError : EditProfileSideEffect
}
