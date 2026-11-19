package com.whatever.caro.feature.home.banner

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun rememberBannerAutoAdvanceAllowed(): Boolean {
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager }

    fun allowed() =
        !manager.isTouchExplorationEnabled &&
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    var enabled by remember(context) { mutableStateOf(allowed()) }
    DisposableEffect(context, manager) {
        val listener = AccessibilityManager.TouchExplorationStateChangeListener { enabled = allowed() }
        val observer =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    enabled = allowed()
                }
            }
        manager.addTouchExplorationStateChangeListener(listener)
        context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        enabled = allowed()
        onDispose {
            manager.removeTouchExplorationStateChangeListener(listener)
            context.contentResolver.unregisterContentObserver(observer)
        }
    }
    return enabled
}
