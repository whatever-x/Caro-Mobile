package com.whatever.caro.core.ads

import kotlinx.coroutines.flow.StateFlow

enum class AdInitializationResult {
    INITIALIZED,
    DEFERRED,
    DISABLED,
    RETRYABLE_FAILURE,
}

interface InterstitialAdController {
    val privacyOptionsRequired: StateFlow<Boolean>

    suspend fun initialize(): AdInitializationResult

    fun preload()

    suspend fun showIfEligible(evaluatedCardCount: Int)

    suspend fun showPrivacyOptions()
}
