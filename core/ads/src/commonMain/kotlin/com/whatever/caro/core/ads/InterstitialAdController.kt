package com.whatever.caro.core.ads

import kotlinx.coroutines.flow.StateFlow

interface InterstitialAdController {
    val privacyOptionsRequired: StateFlow<Boolean>

    suspend fun initialize()

    fun preload()

    suspend fun showIfEligible(evaluatedCardCount: Int)

    suspend fun showPrivacyOptions()
}
