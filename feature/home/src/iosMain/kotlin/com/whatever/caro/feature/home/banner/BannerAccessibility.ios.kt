package com.whatever.caro.feature.home.banner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled
import platform.UIKit.UIAccessibilityIsVoiceOverRunning
import platform.UIKit.UIAccessibilityReduceMotionStatusDidChangeNotification
import platform.UIKit.UIAccessibilityVoiceOverStatusDidChangeNotification

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun rememberBannerAutoAdvanceAllowed(): Boolean {
    fun allowed() = !UIAccessibilityIsVoiceOverRunning() && !UIAccessibilityIsReduceMotionEnabled()
    var enabled by remember { mutableStateOf(allowed()) }
    DisposableEffect(Unit) {
        val center = NSNotificationCenter.defaultCenter
        val observers =
            listOf(
                UIAccessibilityVoiceOverStatusDidChangeNotification,
                UIAccessibilityReduceMotionStatusDidChangeNotification,
            ).map { name ->
                center.addObserverForName(name, null, NSOperationQueue.mainQueue) { enabled = allowed() }
            }
        enabled = allowed()
        onDispose { observers.forEach(center::removeObserver) }
    }
    return enabled
}
