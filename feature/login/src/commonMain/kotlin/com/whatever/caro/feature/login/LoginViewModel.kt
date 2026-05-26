package com.whatever.caro.feature.login

import com.whatever.caro.core.data.repository.AuthRepository
import com.whatever.caro.core.model.auth.SocialLoginType
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.feature.login.model.AppleUser
import com.whatever.caro.feature.login.model.GoogleUser
import com.whatever.caro.feature.login.model.LoginError
import com.whatever.caro.feature.login.model.SocialLoginResult
import com.whatever.caro.feature.login.mvi.LoginIntent
import com.whatever.caro.feature.login.mvi.LoginSideEffect
import com.whatever.caro.feature.login.mvi.LoginState
import io.github.aakira.napier.Napier

class LoginViewModel(
    private val authRepository: AuthRepository,
) : BaseViewModel<LoginState, LoginIntent, LoginSideEffect>(
        initialState = LoginState(),
    ) {
    override fun handleClientException(throwable: Throwable) {
        Napier.e(throwable = throwable) { "login failed: ${throwable.message}" }
        reduce { copy(isLoading = false) }
        postSideEffect(LoginSideEffect.ShowErrorToast(error = LoginError.UNKNOWN))
    }

    override suspend fun handleIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.ClickGoogleLoginButton -> loginWithGoogle(intent)
            is LoginIntent.ClickAppleLoginButton -> loginWithApple(intent)
        }
    }

    private fun loginWithGoogle(intent: LoginIntent.ClickGoogleLoginButton) {
        when (intent.result) {
            SocialLoginResult.Failed -> {
                postSideEffect(LoginSideEffect.ShowErrorToast(error = LoginError.UNKNOWN))
            }

            SocialLoginResult.UserCancelled -> {
                postSideEffect(LoginSideEffect.ShowErrorToast(error = LoginError.USER_CANCELLED))
            }

            is SocialLoginResult.Success<GoogleUser> -> {
                requestLogin(
                    provider = SocialLoginType.GOOGLE,
                    idToken = intent.result.authResult.idToken,
                )
            }
        }
    }

    private fun loginWithApple(intent: LoginIntent.ClickAppleLoginButton) {
        when (intent.result) {
            SocialLoginResult.Failed -> {
                postSideEffect(LoginSideEffect.ShowErrorToast(error = LoginError.UNKNOWN))
            }

            SocialLoginResult.UserCancelled -> {
                postSideEffect(LoginSideEffect.ShowErrorToast(error = LoginError.USER_CANCELLED))
            }

            is SocialLoginResult.Success<AppleUser> -> {
                requestLogin(
                    provider = SocialLoginType.APPLE,
                    idToken = intent.result.authResult.idToken,
                )
            }
        }
    }

    private fun requestLogin(
        provider: SocialLoginType,
        idToken: String,
    ) {
        launch {
            reduce { copy(isLoading = true) }
            authRepository.loginWithSocial(provider = provider, idToken = idToken)
            reduce { copy(isLoading = false) }
            postSideEffect(LoginSideEffect.NavigateHome)
        }
    }
}
