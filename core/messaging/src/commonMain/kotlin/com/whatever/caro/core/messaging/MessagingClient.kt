package com.whatever.caro.core.messaging

import kotlinx.coroutines.flow.SharedFlow

interface MessagingClient {
    val tokenFlow: SharedFlow<String>

    val messageFlow: SharedFlow<RemoteMessage>
}
