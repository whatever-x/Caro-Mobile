package com.whatever.caro.core.ui.keyboard

import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Composable
actual fun rememberImeReadyAwaiter(): suspend () -> Unit {
    val view = LocalView.current
    return remember(view) { { view.awaitImeControllable() } }
}

private suspend fun View.awaitImeControllable() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
    val controller = windowInsetsController ?: return
    suspendCancellableCoroutine { continuation ->
        val listener =
            object : WindowInsetsController.OnControllableInsetsChangedListener {
                override fun onControllableInsetsChanged(
                    controller: WindowInsetsController,
                    typeMask: Int,
                ) {
                    if (typeMask and WindowInsets.Type.ime() == 0) return
                    controller.removeOnControllableInsetsChangedListener(this)
                    if (continuation.isActive) continuation.resume(Unit)
                }
            }
        // 등록 즉시 현재 제어 가능 타입으로 한 번 호출된다.
        controller.addOnControllableInsetsChangedListener(listener)
        continuation.invokeOnCancellation { controller.removeOnControllableInsetsChangedListener(listener) }
    }
}
