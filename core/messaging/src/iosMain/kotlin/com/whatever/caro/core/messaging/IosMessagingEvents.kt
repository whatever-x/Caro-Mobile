package com.whatever.caro.core.messaging

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object IosMessagingEvents {
    private val mutableTokenFlow = MutableStateFlow("")

    private val mutableMessages = Channel<RemoteMessage>(capacity = Channel.CONFLATED)

    internal val tokenFlow: StateFlow<String> = mutableTokenFlow.asStateFlow()
    internal val messages: ReceiveChannel<RemoteMessage> = mutableMessages

    fun onTokenRefreshed(token: String) {
        mutableTokenFlow.tryEmit(token)
    }

    fun onMessageReceived(deckId: String?) {
        mutableMessages.trySend(RemoteMessage(deckId = deckId))
    }
}
