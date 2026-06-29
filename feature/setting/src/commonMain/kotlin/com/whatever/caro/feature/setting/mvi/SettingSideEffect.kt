package com.whatever.caro.feature.setting.mvi

import com.whatever.caro.core.viewmodel.contract.UiSideEffect
import com.whatever.caro.feature.setting.model.SnackbarType
import com.whatever.caro.feature.setting.model.WebViewType

sealed interface SettingSideEffect : UiSideEffect {
    data class NavigateToEditNickName(
        val currentNickname: String,
    ) : SettingSideEffect

    data object NavigateToLogin : SettingSideEffect

    data class NavigateWebView(
        val type: WebViewType,
    ) : SettingSideEffect

    data class ShowSnackbar(
        val type: SnackbarType,
    ) : SettingSideEffect

    data object PopBackStack : SettingSideEffect
}
