package com.whatever.caro.feature.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.login_snackbar_cancel
import caromobile.core.designsystem.generated.resources.login_snackbar_error
import com.whatever.caro.core.designsystem.components.CaroSnackbarStyle
import com.whatever.caro.core.model.auth.SocialLoginType
import com.whatever.caro.core.navigator.contract.NavCommand.To
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.navigator.entries.CreateProfileEntry
import com.whatever.caro.core.navigator.entries.HomeEntry
import com.whatever.caro.core.ui.snackbar.SnackBarMessage
import com.whatever.caro.core.ui.snackbar.SnackbarController
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
    snackbarController: SnackbarController,
    googleAuthenticator: SocialAuthenticator<GoogleUser> = koinInject<GoogleAuthProvider>().get(),
    appleAuthenticator: SocialAuthenticator<AppleUser> = koinInject<AppleAuthProvider>().get(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val loginErrorMessage = stringResource(Res.string.login_snackbar_error)
    val loginCancelledMessage = stringResource(Res.string.login_snackbar_cancel)
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

                        SocialLoginType.NONE -> {}
                    }
                }
            }
        }

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                is LoginSideEffect.NavigateHome -> {
                    navDispatcher.emit(command = To(key = HomeEntry))
                }

                is LoginSideEffect.ShowErrorSnackbar -> {
                    val message =
                        when (sideEffect.error) {
                            LoginError.UNKNOWN -> loginErrorMessage
                            LoginError.USER_CANCELLED -> loginCancelledMessage
                        }
                    snackbarController.show(
                        SnackBarMessage(
                            message = message,
                            style = CaroSnackbarStyle.Error,
                        ),
                    )
                }

                LoginSideEffect.NavigateCreateProfile -> {
                    navDispatcher.emit(command = To(key = CreateProfileEntry))
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
