package com.whatever.caro.feature.splash

import com.whatever.caro.core.messaging.MessagingClient
import com.whatever.caro.core.viewmodel.BaseViewModel
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull

class SplashViewModel(
    private val messagingClient: MessagingClient,
) : BaseViewModel<SplashState, SplashIntent, SplashSideEffect>(
        initialState = SplashState(),
    ) {
    override suspend fun handleIntent(intent: SplashIntent) = Unit

    // FIXME : 추후에 초기화 로직을 구현, 현재는 권한에 따라서만 처리
    fun start(permissionsController: PermissionsController) {
        launch {
            ensureNotificationPermission(permissionsController)

            val deckId =
                withTimeoutOrNull(SPLASH_DELAY_MS) {
                    messagingClient.messageFlow.firstOrNull()
                }?.deckId
            if (deckId != null) {
                postSideEffect(SplashSideEffect.NavigateHome(deckId = deckId))
            } else {
                postSideEffect(SplashSideEffect.NavigateLogin)
            }
            reduce { copy(isLoading = false) }
        }
    }

    private suspend fun ensureNotificationPermission(controller: PermissionsController) {
        // TODO : 알림정책 필요
        if (controller.getPermissionState(Permission.REMOTE_NOTIFICATION) == PermissionState.Granted) return
        try {
            controller.providePermission(Permission.REMOTE_NOTIFICATION)
        } catch (_: DeniedAlwaysException) {
            postSideEffect(SplashSideEffect.ShowNotificationPermissionDeniedAlwaysDialog)
        } catch (_: DeniedException) {
            postSideEffect(SplashSideEffect.ShowNotificationPermissionDeniedDialog)
        } catch (_: RequestCanceledException) {
        }
    }

    companion object {
        private const val SPLASH_DELAY_MS = 1_000L
    }
}
