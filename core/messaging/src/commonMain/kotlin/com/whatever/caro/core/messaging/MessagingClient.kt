package com.whatever.caro.core.messaging

import kotlinx.coroutines.flow.SharedFlow

interface MessagingClient {
    val tokenFlow: SharedFlow<String>

    val messageFlow: SharedFlow<RemoteMessage>

    suspend fun currentToken(): String?

    suspend fun deleteToken()

    suspend fun subscribeToTopic(topic: String)

    suspend fun unsubscribeFromTopic(topic: String)
}
