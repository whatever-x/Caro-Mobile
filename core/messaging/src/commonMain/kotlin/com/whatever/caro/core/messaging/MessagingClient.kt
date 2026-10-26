package com.whatever.caro.core.messaging

import kotlinx.coroutines.flow.StateFlow

interface MessagingClient {
    val tokenFlow: StateFlow<String>
}
