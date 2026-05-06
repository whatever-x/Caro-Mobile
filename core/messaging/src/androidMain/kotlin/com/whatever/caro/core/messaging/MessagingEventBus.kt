package com.whatever.caro.core.messaging

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal object MessagingEventBus {
    private val mutableTokenFlow = MutableStateFlow("")

    private val mutableMessages = Channel<RemoteMessage>(capacity = Channel.CONFLATED)

    val tokenFlow: StateFlow<String> = mutableTokenFlow.asStateFlow()
    val messages: ReceiveChannel<RemoteMessage> = mutableMessages

    fun publishToken(token: String) {
        mutableTokenFlow.tryEmit(token)
    }

    fun publishMessage(message: RemoteMessage) {
        mutableMessages.trySend(message)
    }
}
