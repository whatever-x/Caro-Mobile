package com.whatever.caro.feature.importer.platform

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

private const val DEFAULT_SCALE = 1f

@Composable
internal actual fun rememberReduceMotionEnabled(): Boolean {
    val resolver = LocalContext.current.applicationContext.contentResolver
    var isEnabled by remember(resolver) { mutableStateOf(readReduceMotion(resolver)) }

    DisposableEffect(resolver) {
        val observer =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    isEnabled = readReduceMotion(resolver)
                }
            }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.TRANSITION_ANIMATION_SCALE), false, observer)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return isEnabled
}

// 안드로이드에는 동작 줄이기 전용 설정이 없어 개발자 옵션과 접근성의 애니메이션 제거가 바꾸는 배율이 0인지로 판단한다.
private fun readReduceMotion(resolver: ContentResolver): Boolean =
    Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, DEFAULT_SCALE) == 0f ||
        Settings.Global.getFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE, DEFAULT_SCALE) == 0f
