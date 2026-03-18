package com.whatever.caro.feature.login.mvi

import com.whatever.caro.core.viewmodel.contract.UiIntent
import com.whatever.caro.feature.login.model.AppleUser
import com.whatever.caro.feature.login.model.GoogleUser
import com.whatever.caro.feature.login.model.SocialLoginResult

sealed interface LoginIntent : UiIntent {
    data class ClickGoogleLoginButton(
        val result: SocialLoginResult<GoogleUser>,
    ) : LoginIntent

    data class ClickAppleLoginButton(
        val result: SocialLoginResult<AppleUser>,
    ) : LoginIntent
}
