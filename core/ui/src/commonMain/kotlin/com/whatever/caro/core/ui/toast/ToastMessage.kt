package com.whatever.caro.core.ui.toast

/**
 * 이미 해석된 토스트 문자열을 운반하는 계약 타입. 문자열 지역화는 호출부(Route)에서 처리한다.
 * 현재는 message만 존재하지만, 추후 action같은 부가적인 내용이 추가될 수 있어 class로 납둠
 */
data class ToastMessage(
    val message: String,
)
