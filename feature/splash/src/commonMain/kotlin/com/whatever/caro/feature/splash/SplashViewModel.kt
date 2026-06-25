package com.whatever.caro.feature.splash

import com.whatever.caro.core.data.repository.auth.AuthRepository
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.splash.mvi.SplashIntent
import com.whatever.caro.feature.splash.mvi.SplashSideEffect
import com.whatever.caro.feature.splash.mvi.SplashState
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.RequestCanceledException
import dev.icerock.moko.permissions.notifications.REMOTE_NOTIFICATION
import kotlinx.coroutines.delay

class SplashViewModel(
    private val authRepository: AuthRepository,
    exceptionFilter: ExceptionFilter,
) : BaseViewModel<SplashState, SplashIntent, SplashSideEffect>(
        initialState = SplashState(),
        exceptionFilter = exceptionFilter,
    ) {

    override fun handleClientException(throwable: Throwable) {
        super.handleClientException(throwable)
        postSideEffect(SplashSideEffect.NavigateLogin)
    }

    override suspend fun handleIntent(intent: SplashIntent) {
        when (intent) {
            SplashIntent.Initialize -> initialize()
        }
    }

    private suspend fun initialize() {
        delay(MINIMUM_SPLASH_DURATION_MILLIS)
        authRepository.refreshToken()
        reduce { copy(isInitializing = false) }
        postSideEffect(SplashSideEffect.NavigateHome)
    }

    private suspend fun ensureNotificationPermission(controller: PermissionsController) {
        if (controller.getPermissionState(Permission.REMOTE_NOTIFICATION) == PermissionState.Granted) return
        try {
            controller.providePermission(Permission.REMOTE_NOTIFICATION)
        } catch (_: DeniedAlwaysException) {
            // TODO : 권한 거부 기록 있음
        } catch (_: DeniedException) {
            // TODO : 권한이 거부 됨
        } catch (_: RequestCanceledException) {
            // TODO : 권한이 취소 됨
        }
    }

    companion object {
        private const val MINIMUM_SPLASH_DURATION_MILLIS = 800L
    }
}
