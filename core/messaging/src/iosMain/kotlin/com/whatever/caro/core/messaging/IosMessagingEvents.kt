package com.whatever.caro.core.messaging

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object IosMessagingEvents {
    private val mutableTokenFlow = MutableStateFlow("")

    private val mutableMessages = Channel<CloudMessage>(capacity = Channel.CONFLATED)

    internal val tokenFlow: StateFlow<String> = mutableTokenFlow.asStateFlow()
    internal val messages: ReceiveChannel<CloudMessage> = mutableMessages

    fun onTokenRefreshed(token: String) {
        mutableTokenFlow.tryEmit(token)
    }

    /**
     * iOS에서는 각 데이터 추출을 Xcode에서 진행
     * 메소드 파라미터로 확장 필요, CloudMessage 객체를 샏성해서 mutableMessages로 send
     * */
    fun onMessageReceived(deckId: String?) {}
}
