package com.whatever.caro.feature.setting.platform

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal actual fun InquiryLifecycleEffect(
    onBackground: () -> Unit,
    onForeground: () -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val onBackgroundState = rememberUpdatedState(onBackground)
    val onForegroundState = rememberUpdatedState(onForeground)
    DisposableEffect(activity) {
        val lifecycle = (activity as? LifecycleOwner)?.lifecycle
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> if (activity?.isChangingConfigurations == false) onBackgroundState.value()
                    Lifecycle.Event.ON_RESUME -> onForegroundState.value()
                    else -> Unit
                }
            }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }
}

@Composable
internal actual fun rememberOpenInquiry(): suspend (String) -> Boolean {
    val context = LocalContext.current
    return remember(context) { { url -> openInquiryUrl(context, url) } }
}

@Composable
internal actual fun rememberShouldPreserveInquiryOnDispose(): () -> Boolean {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    return remember(activity) { { activity?.isChangingConfigurations == true } }
}

internal suspend fun openInquiryUrl(
    context: Context,
    url: String,
): Boolean =
    withContext(Dispatchers.Main.immediate) {
        val activity = context.findActivity() ?: return@withContext false
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }
    }

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
