package com.whatever.caro.feature.profile.mvi

import com.whatever.caro.core.model.auth.SocialLoginType
import com.whatever.caro.core.viewmodel.contract.UiState

data class SettingState(
    val isLoading: Boolean = false,
    val nickname: String = "",
    val emailAddress: String = "",
    val socialLoginType: SocialLoginType? = null,
    val appVersion: String = "",
) : UiState
