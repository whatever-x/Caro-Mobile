package com.whatever.caro.core.ui.toast

import kotlinx.coroutines.flow.Flow

/**
 * 앱 전역 토스트 디스패처. 어디서든 [show] 로 토스트를 보내면 앱 루트의 단일 수집자가 호스트를 구동한다.
 * 구현/바인딩은 조립 지점(composeApp)이 소유하며, 이 모듈은 계약만 제공한다.
 */
interface ToastController {
    val messages: Flow<ToastMessage>

    suspend fun show(message: ToastMessage)
}
