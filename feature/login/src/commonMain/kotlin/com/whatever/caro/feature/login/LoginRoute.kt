package com.whatever.caro.feature.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import caromobile.feature.login.generated.resources.Res
import caromobile.feature.login.generated.resources.login_cancelled
import caromobile.feature.login.generated.resources.login_error
import com.whatever.caro.core.designsystem.components.LocalSnackbarHostState
import com.whatever.caro.core.model.auth.SocialLoginType
import com.whatever.caro.core.navigator.contract.NavCommand.To
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.navigator.entries.HomeEntry
import com.whatever.caro.core.navigator.entries.Payload
import com.whatever.caro.feature.login.model.AppleUser
import com.whatever.caro.feature.login.model.GoogleUser
import com.whatever.caro.feature.login.model.LoginError
import com.whatever.caro.feature.login.model.SocialAuthenticator
import com.whatever.caro.feature.login.mvi.LoginIntent
import com.whatever.caro.feature.login.mvi.LoginSideEffect
import com.whatever.caro.feature.login.provider.AppleAuthProvider
import com.whatever.caro.feature.login.provider.GoogleAuthProvider
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun LoginRoute(
    viewModel: LoginViewModel,
    navDispatcher: NavigationDispatcher,
    googleAuthenticator: SocialAuthenticator<GoogleUser> = koinInject<GoogleAuthProvider>().get(),
    appleAuthenticator: SocialAuthenticator<AppleUser> = koinInject<AppleAuthProvider>().get(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    val loginErrorMessage = stringResource(Res.string.login_error)
    val loginCancelledMessage = stringResource(Res.string.login_cancelled)
    val socialLoginAuth: (SocialLoginType) -> Unit =
        remember {
            { type ->
                coroutineScope.launch {
                    when (type) {
                        SocialLoginType.GOOGLE -> {
                            val result = googleAuthenticator.authenticate()
                            viewModel.intent(LoginIntent.ClickGoogleLoginButton(result))
                        }

                        SocialLoginType.APPLE -> {
                            val result = appleAuthenticator.authenticate()
                            viewModel.intent(LoginIntent.ClickAppleLoginButton(result))
                        }
                    }
                }
            }
        }

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                is LoginSideEffect.NavigateHome -> {
                    navDispatcher.emit(
                        command =
                            To(
                                key =
                                    HomeEntry(
                                        payload =
                                            Payload(
                                                id = 1,
                                                name = "test",
                                            ),
                                    ),
                            ),
                    )
                }

                is LoginSideEffect.ShowErrorToast -> {
                    // TODO : 오류 메세지도 전부 design system?
                    val message =
                        when (sideEffect.error) {
                            LoginError.UNKNOWN -> loginErrorMessage
                            LoginError.USER_CANCELLED -> loginCancelledMessage
                        }
                    snackbarHost.showSnackbar(message = message)
                }
            }
        }
    }

    LoginScreen(
        state = state,
        onIntent = viewModel::intent,
        onLaunch = { socialLoginType -> socialLoginAuth(socialLoginType) },
    )
}
