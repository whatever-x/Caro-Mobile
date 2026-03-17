package com.whatever.caro.feature.login

import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.feature.login.model.AppleUser
import com.whatever.caro.feature.login.model.GoogleUser
import com.whatever.caro.feature.login.model.LoginError
import com.whatever.caro.feature.login.model.SocialLoginResult
import com.whatever.caro.feature.login.mvi.LoginIntent
import com.whatever.caro.feature.login.mvi.LoginSideEffect
import com.whatever.caro.feature.login.mvi.LoginState
import io.github.aakira.napier.Napier
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class LoginViewModel : BaseViewModel<LoginState, LoginIntent, LoginSideEffect>(
    initialState = LoginState(),
) {
    override fun handleClientException(throwable: Throwable) {
        TODO()
    }

    override suspend fun handleIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.ClickGoogleLoginButton -> loginWithGoogle(intent)
            is LoginIntent.ClickAppleLoginButton -> loginWithApple(intent)
        }
    }

    private fun loginWithGoogle(
        intent: LoginIntent.ClickGoogleLoginButton
    ) {
        when (intent.result) {
            SocialLoginResult.Failed -> postSideEffect(LoginSideEffect.ShowErrorToast(error = LoginError.UNKNOWN))
            is SocialLoginResult.Success<GoogleUser> -> Napier.e { "idToken : ${intent.result.authResult.idToken}" }
            SocialLoginResult.UserCancelled -> postSideEffect(LoginSideEffect.ShowErrorToast(error = LoginError.USER_CANCELLED))
        }
    }

    private fun loginWithApple(
        intent: LoginIntent.ClickAppleLoginButton
    ) {
        when (intent.result) {
            SocialLoginResult.Failed -> postSideEffect(LoginSideEffect.ShowErrorToast(error = LoginError.UNKNOWN))
            is SocialLoginResult.Success<AppleUser> -> Napier.e { "idToken : ${intent.result.authResult.idToken}" }
            SocialLoginResult.UserCancelled -> postSideEffect(LoginSideEffect.ShowErrorToast(error = LoginError.USER_CANCELLED))
        }
    }
}
