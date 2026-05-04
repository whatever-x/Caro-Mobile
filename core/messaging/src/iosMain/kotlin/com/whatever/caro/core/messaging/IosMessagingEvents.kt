package com.whatever.caro.core.messaging

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object IosMessagingEvents {
    private val mutableTokenFlow =
        MutableSharedFlow<String>(
            replay = 1,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    private val mutableMessageFlow =
        MutableSharedFlow<RemoteMessage>(
            replay = 1,
            extraBufferCapacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    internal val tokenFlow: SharedFlow<String> = mutableTokenFlow.asSharedFlow()
    internal val messageFlow: SharedFlow<RemoteMessage> = mutableMessageFlow.asSharedFlow()

    fun onTokenRefreshed(token: String) {
        mutableTokenFlow.tryEmit(token)
    }

    fun onMessageReceived(deckId: String?) {
        mutableMessageFlow.tryEmit(RemoteMessage(deckId = deckId))
    }
}
