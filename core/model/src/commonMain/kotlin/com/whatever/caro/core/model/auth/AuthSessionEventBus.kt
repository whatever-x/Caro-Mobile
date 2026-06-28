package com.whatever.caro.core.model.auth

import kotlinx.coroutines.flow.Flow

interface AuthSessionEventBus {
    val events: Flow<AuthSessionEvent>

    suspend fun publish(event: AuthSessionEvent)
}
