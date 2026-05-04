package com.whatever.caro.feature.splash

import com.whatever.caro.core.messaging.MessagingClient
import com.whatever.caro.core.messaging.RemoteMessage
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.feature.splash.mvi.SplashIntent
import com.whatever.caro.feature.splash.mvi.SplashSideEffect
import com.whatever.caro.feature.splash.mvi.SplashState
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull

class SplashViewModel(
    private val messagingClient: MessagingClient,
) : BaseViewModel<SplashState, SplashIntent, SplashSideEffect>(
        initialState = SplashState(),
    ) {
    override suspend fun handleIntent(intent: SplashIntent) = Unit

    fun start() {
        launch {
            val message =
                withTimeoutOrNull(SPLASH_DELAY_MS) {
                    messagingClient.messageFlow.firstOrNull()
                }
            val deckId = message?.extractDeckId()
            if (deckId != null) {
                postSideEffect(SplashSideEffect.NavigateHome(deckId = deckId))
            } else {
                postSideEffect(SplashSideEffect.NavigateLogin)
            }
            reduce { copy(isLoading = false) }
        }
    }

    private fun RemoteMessage.extractDeckId(): String? = data[DECK_ID_KEY]

    companion object {
        private const val SPLASH_DELAY_MS = 1_000L
        private const val DECK_ID_KEY = "deck_id"
    }
}
