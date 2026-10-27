package com.whatever.caro.core.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.koin.compose.koinInject

@Composable
actual fun ConfigureAdHost(
    allowConsent: Boolean,
    onAvailabilityChanged: (available: Boolean, formShowing: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val activity = context.activity()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val provider = koinInject<AndroidInterstitialProvider>()
    val formShowing by provider.consentFormShowing.collectAsState()
    val notifyAvailability by rememberUpdatedState(onAvailabilityChanged)
    SideEffect {
        notifyAvailability(
            provider.bindHost(activity, lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED), allowConsent),
            formShowing,
        )
    }
    DisposableEffect(activity, lifecycle, allowConsent) {
        val observer =
            LifecycleEventObserver { _, _ ->
                notifyAvailability(
                    provider.bindHost(activity, lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED), allowConsent),
                    provider.consentFormShowing.value,
                )
            }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            provider.bindHost(null, false, false)
            notifyAvailability(false, provider.consentFormShowing.value)
        }
    }
}

private fun Context.activity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.activity()
        else -> null
    }
