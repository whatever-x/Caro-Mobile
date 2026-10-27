package com.whatever.caro.core.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.koin.compose.koinInject

@Composable
actual fun ConfigureAdHost(
    allowConsent: Boolean,
    onAvailabilityChanged: (available: Boolean, formShowing: Boolean) -> Unit,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val provider = koinInject<IosInterstitialProvider>()
    val formShowing by provider.consentFormShowing.collectAsState()
    val notifyAvailability by rememberUpdatedState(onAvailabilityChanged)
    SideEffect {
        notifyAvailability(provider.bindHost(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED), allowConsent), formShowing)
    }
    DisposableEffect(lifecycle, allowConsent) {
        val observer =
            LifecycleEventObserver { _, _ ->
                notifyAvailability(
                    provider.bindHost(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED), allowConsent),
                    provider.consentFormShowing.value,
                )
            }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            provider.bindHost(false, false)
            notifyAvailability(false, provider.consentFormShowing.value)
        }
    }
}
