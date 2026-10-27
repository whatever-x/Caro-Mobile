package com.whatever.caro.core.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.koin.compose.koinInject

@Composable
actual fun ConfigureAdHost(allowConsent: Boolean) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val provider = koinInject<IosInterstitialProvider>()
    SideEffect { provider.bindHost(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED), allowConsent) }
    DisposableEffect(lifecycle, allowConsent) {
        val observer =
            LifecycleEventObserver { _, _ ->
                provider.bindHost(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED), allowConsent)
            }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            provider.bindHost(false, false)
        }
    }
}
