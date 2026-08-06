package com.whatever.caro.composeApp

import androidx.compose.runtime.Composable

/**
 * 앱 종료 동작을 반환한다.
 *
 * Android 는 현재 Activity 를 종료하고, iOS 는 앱 강제 종료가 HIG 상 허용되지 않으므로
 * 아무 동작도 하지 않는다.
 */
@Composable
internal expect fun rememberAppExit(): () -> Unit
