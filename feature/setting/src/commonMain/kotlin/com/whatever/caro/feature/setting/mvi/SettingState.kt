package com.whatever.caro.feature.setting.mvi

import com.whatever.caro.core.model.auth.SocialLoginType
import com.whatever.caro.core.viewmodel.contract.UiState

// data class SettingState(
//    val isLoading: Boolean = false,
//    val nickname: String = "",
//    val emailAddress: String = "",
//    val socialLoginType: SocialLoginType? = null,
// ) : UiState

data class SettingState(
    val isLoading: Boolean = false,
    val nickname: String = "승우",
    val emailAddress: String = "rsw1452@gmail.com",
    val socialLoginType: SocialLoginType? = SocialLoginType.GOOGLE,
) : UiState
