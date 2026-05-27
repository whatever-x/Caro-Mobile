package com.whatever.caro.core.model.auth

interface AuthSessionEventPublisher {
    suspend fun publish(event: AuthSessionEvent)
}
