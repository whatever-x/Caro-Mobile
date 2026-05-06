package com.whatever.caro.core.messaging

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.StateFlow

interface MessagingClient {
    val tokenFlow: StateFlow<String>

    val messages: ReceiveChannel<RemoteMessage>
}
