@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.whatever.caro.feature.setting.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.Foundation.NSURL.Companion.URLWithString
import platform.UIKit.UIApplication
import platform.UIKit.UISceneDidActivateNotification
import platform.UIKit.UISceneDidEnterBackgroundNotification
import kotlin.coroutines.resume

@Composable
internal actual fun InquiryLifecycleEffect(
    onBackground: () -> Unit,
    onForeground: () -> Unit,
) {
    val controller = LocalUIViewController.current
    val onBackgroundState = rememberUpdatedState(onBackground)
    val onForegroundState = rememberUpdatedState(onForeground)
    DisposableEffect(controller) {
        val stopObserving =
            observeInquirySceneLifecycle(
                currentScene = { controller.view.window?.windowScene },
                onBackground = { onBackgroundState.value() },
                onForeground = { onForegroundState.value() },
            )
        onDispose { stopObserving() }
    }
}

// A scene is resolved when an event arrives: composition may precede attachment to a window.
internal fun observeInquirySceneLifecycle(
    currentScene: () -> Any?,
    onBackground: () -> Unit,
    onForeground: () -> Unit,
    notificationCenter: NSNotificationCenter = NSNotificationCenter.defaultCenter,
): () -> Unit {
    val backgroundObserver =
        notificationCenter.addObserverForName(
            name = UISceneDidEnterBackgroundNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { notification ->
            val scene = currentScene()
            if (scene != null && notification?.`object` == scene) onBackground()
        }
    val foregroundObserver =
        notificationCenter.addObserverForName(
            name = UISceneDidActivateNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { notification ->
            val scene = currentScene()
            if (scene != null && notification?.`object` == scene) onForeground()
        }
    return {
        notificationCenter.removeObserver(backgroundObserver)
        notificationCenter.removeObserver(foregroundObserver)
    }
}

@Composable
internal actual fun rememberOpenInquiry(): suspend (String) -> Boolean =
    remember {
        { url ->
            withContext(Dispatchers.Main.immediate) {
                openInquiryUrl(url) { nsUrl, onResult ->
                    UIApplication.sharedApplication.openURL(nsUrl, emptyMap<Any?, Any>(), onResult)
                }
            }
        }
    }

@Composable
internal actual fun rememberShouldPreserveInquiryOnDispose(): () -> Boolean = remember { { false } }

internal suspend fun openInquiryUrl(
    url: String,
    openUrl: (NSURL, (Boolean) -> Unit) -> Unit,
): Boolean {
    val nsUrl = URLWithString(url) ?: return false
    return suspendCancellableCoroutine { continuation ->
        // UIKit completes on the main thread; keep duplicate/late callbacks from resuming twice.
        var completed = false
        openUrl(nsUrl) { opened ->
            if (!completed) {
                completed = true
                if (continuation.isActive) continuation.resume(opened)
            }
        }
    }
}
