package com.whatever.caro.core.messaging

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

internal object MessagingEventBus {
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

    val tokenFlow: SharedFlow<String> = mutableTokenFlow.asSharedFlow()
    val messageFlow: SharedFlow<RemoteMessage> = mutableMessageFlow.asSharedFlow()

    fun publishToken(token: String) {
        mutableTokenFlow.tryEmit(token)
    }

    fun publishMessage(message: RemoteMessage) {
        mutableMessageFlow.tryEmit(message)
    }
}
