package com.whatever.caro.feature.setting.mvi

import com.whatever.caro.core.model.auth.SocialLoginType
import com.whatever.caro.core.viewmodel.contract.UiState

data class SettingState(
    val isLoading: Boolean = true,
    val nickname: String = "",
    val emailAddress: String = "",
    val socialLoginType: SocialLoginType = SocialLoginType.NONE,
    val accountDeleteDialogVisible: Boolean = false,
) : UiState {
    val isUserInfoVisible: Boolean
        get() = isLoading || nickname.isNotBlank()
}
