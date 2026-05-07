package com.whatever.caro.feature.splash

import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.feature.splash.mvi.SplashIntent
import com.whatever.caro.feature.splash.mvi.SplashSideEffect
import com.whatever.caro.feature.splash.mvi.SplashState
import kotlinx.coroutines.delay

class SplashViewModel :
    BaseViewModel<SplashState, SplashIntent, SplashSideEffect>(
        initialState = SplashState(),
    ) {
    override suspend fun handleIntent(intent: SplashIntent) {
        when (intent) {
            SplashIntent.Initialize -> initialize()
        }
    }

    private suspend fun initialize() {
        delay(MINIMUM_SPLASH_DURATION_MILLIS)
        reduce { copy(isInitializing = false) }
        postSideEffect(SplashSideEffect.NavigateLogin)
    }

    companion object {
        private const val MINIMUM_SPLASH_DURATION_MILLIS = 800L
    }
}
