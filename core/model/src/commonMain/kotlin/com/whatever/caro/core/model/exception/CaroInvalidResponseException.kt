package com.whatever.caro.core.model.exception

data class CaroInvalidResponseException(
    override val debugMessage: String,
    override val throwable: Throwable? = null,
) : CaroException(
        message = "Invalid Response Error",
        debugMessage = debugMessage,
        throwable = throwable,
    )
