package com.whatever.caro.core.data.auth

import com.whatever.caro.core.model.auth.AuthSessionEvent
import com.whatever.caro.core.model.auth.AuthSessionEventBus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

internal class AuthSessionEventBusImpl : AuthSessionEventBus {
    private val channel = Channel<AuthSessionEvent>(capacity = Channel.CONFLATED)

    override val events = channel.receiveAsFlow()

    override suspend fun publish(event: AuthSessionEvent) {
        channel.send(event)
    }
}
