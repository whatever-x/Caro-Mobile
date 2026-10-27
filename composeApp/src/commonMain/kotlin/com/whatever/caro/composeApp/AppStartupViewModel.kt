package com.whatever.caro.composeApp

import com.whatever.caro.core.ads.AdInitializationResult
import com.whatever.caro.core.ads.InterstitialAdController
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.core.viewmodel.contract.UiIntent
import com.whatever.caro.core.viewmodel.contract.UiSideEffect
import com.whatever.caro.core.viewmodel.contract.UiState
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException

internal data class AppStartupState(
    val notificationFinished: Boolean = false,
    val adHostAvailable: Boolean = false,
    val consentFormShowing: Boolean = false,
    val hostGeneration: Long = 0,
    val attemptId: Long = 0,
    val adInitialization: AdInitialization = AdInitialization.WAITING,
) : UiState

internal enum class AdInitialization {
    WAITING,
    INITIALIZING,
    INITIALIZED,
    DEFERRED,
    DISABLED,
    RETRYABLE_FAILURE,
}

internal sealed interface AppStartupIntent : UiIntent {
    data object NotificationFinished : AppStartupIntent

    data class AdHostChanged(
        val available: Boolean,
        val formShowing: Boolean = false,
    ) : AppStartupIntent
}

internal sealed interface AppStartupSideEffect : UiSideEffect

internal class AppStartupViewModel(
    private val adsController: InterstitialAdController,
    exceptionFilter: ExceptionFilter,
) : BaseViewModel<AppStartupState, AppStartupIntent, AppStartupSideEffect>(AppStartupState(), exceptionFilter) {
    override suspend fun handleIntent(intent: AppStartupIntent) {
        val hostBecameAvailable =
            when (intent) {
                AppStartupIntent.NotificationFinished -> {
                    if (currentState.notificationFinished) return
                    reduce { copy(notificationFinished = true) }
                    false
                }

                is AppStartupIntent.AdHostChanged -> {
                    if (currentState.adHostAvailable == intent.available && currentState.consentFormShowing == intent.formShowing) return
                    val becameAvailable = !currentState.adHostAvailable && intent.available
                    reduce {
                        copy(
                            adHostAvailable = intent.available,
                            consentFormShowing = intent.formShowing,
                            hostGeneration = hostGeneration + if (!adHostAvailable && intent.available) 1 else 0,
                        )
                    }
                    becameAvailable
                }
            }
        initializeIfEligible(retryFailure = hostBecameAvailable)
    }

    private suspend fun initializeIfEligible(retryFailure: Boolean) {
        val state = currentState
        if (!state.notificationFinished || !state.adHostAvailable || state.consentFormShowing) return
        if (state.adInitialization == AdInitialization.RETRYABLE_FAILURE && !retryFailure) return
        if (state.adInitialization !in
            setOf(AdInitialization.WAITING, AdInitialization.DEFERRED, AdInitialization.RETRYABLE_FAILURE)
        ) {
            return
        }

        val attempt = state.attemptId + 1
        reduce { copy(attemptId = attempt, adInitialization = AdInitialization.INITIALIZING) }
        val result =
            try {
                adsController.initialize()
            } catch (exception: CancellationException) {
                if (currentState.attemptId == attempt) {
                    reduce { copy(adInitialization = AdInitialization.DEFERRED) }
                }
                throw exception
            } catch (exception: Exception) {
                Napier.w(throwable = exception) { "Ad initialization failed" }
                AdInitializationResult.RETRYABLE_FAILURE
            }
        if (currentState.attemptId != attempt) return
        val initialization =
            when (result) {
                AdInitializationResult.INITIALIZED -> AdInitialization.INITIALIZED
                AdInitializationResult.DEFERRED -> AdInitialization.DEFERRED
                AdInitializationResult.DISABLED -> AdInitialization.DISABLED
                AdInitializationResult.RETRYABLE_FAILURE -> AdInitialization.RETRYABLE_FAILURE
            }
        reduce { copy(adInitialization = initialization) }
        if (result == AdInitializationResult.DEFERRED && currentState.hostGeneration > state.hostGeneration) {
            initializeIfEligible(retryFailure = false)
        }
    }
}
