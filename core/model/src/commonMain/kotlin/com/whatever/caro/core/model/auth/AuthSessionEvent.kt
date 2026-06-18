package com.whatever.caro.core.model.auth

sealed interface AuthSessionEvent {
    data object Expired : AuthSessionEvent
}
