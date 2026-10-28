package com.whatever.caro.core.model.auth

sealed interface AuthSessionEvent {
    data class Expired(
        val generation: Long,
    ) : AuthSessionEvent
}
