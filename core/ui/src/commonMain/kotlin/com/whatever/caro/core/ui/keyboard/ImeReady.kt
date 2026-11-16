package com.whatever.caro.core.ui.keyboard

import androidx.compose.runtime.Composable

/**
 * 키보드를 띄워도 되는 시점(window 가 IME 제어권을 가진 상태)까지 대기하는 함수를 반환한다.
 *
 * Android 11 에서는 바텀시트 등 다른 window 가 닫힌 직후처럼 IME 제어권이 없을 때 키보드를 띄우면
 * 표시 요청이 남아, 이후 숨겨도 키보드가 다시 올라온다. 진입 시 자동 포커스 전에 호출한다.
 */
@Composable
expect fun rememberImeReadyAwaiter(): suspend () -> Unit
