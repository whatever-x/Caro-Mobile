package com.whatever.caro.feature.profile.mvi

import com.whatever.caro.core.viewmodel.contract.UiSideEffect
import com.whatever.caro.feature.profile.model.WebViewType

sealed interface SettingSideEffect : UiSideEffect {
    data class NavigateToEditNickName(
        val currentNickname: String,
    ) : SettingSideEffect

    data object NavigateToLogin : SettingSideEffect

    data class NavigateWebView(
        val type: WebViewType,
    ) : SettingSideEffect

    data object PopBackStack : SettingSideEffect
}
