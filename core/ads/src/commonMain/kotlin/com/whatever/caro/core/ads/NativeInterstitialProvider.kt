package com.whatever.caro.core.ads

import kotlinx.coroutines.flow.StateFlow

internal interface NativeInterstitialProvider {
    val privacyOptionsRequired: StateFlow<Boolean>

    suspend fun initialize()

    fun preload()

    fun takeReadyAd(): ReadyInterstitial?

    suspend fun showPrivacyOptions()
}

internal fun interface ReadyInterstitial {
    /** Returns false only when the SDK confirms that presentation did not happen. */
    suspend fun show(onShown: () -> Unit): Boolean
}
