package com.whatever.caro.core.model.exception

data class CaroUnknownException(
    override val debugMessage: String,
    override val throwable: Throwable? = null,
) : CaroException(
        message = "Unknown Error",
        debugMessage = debugMessage,
        throwable = throwable,
    )
