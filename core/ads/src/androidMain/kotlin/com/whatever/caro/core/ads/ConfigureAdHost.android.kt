package com.whatever.caro.core.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.koin.compose.koinInject

@Composable
actual fun ConfigureAdHost(allowConsent: Boolean) {
    val context = LocalContext.current
    val activity = context.activity()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val provider = koinInject<AndroidInterstitialProvider>()
    SideEffect { provider.bindHost(activity, lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED), allowConsent) }
    DisposableEffect(activity, lifecycle, allowConsent) {
        val observer =
            LifecycleEventObserver { _, _ ->
                provider.bindHost(activity, lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED), allowConsent)
            }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            provider.bindHost(null, false, false)
        }
    }
}

private fun Context.activity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.activity()
        else -> null
    }
