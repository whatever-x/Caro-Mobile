package com.whatever.caro.core.model.exception

sealed class CaroAuthException(
    message: String,
    debugMessage: String,
    throwable: Throwable? = null,
) : CaroException(message, debugMessage, throwable) {
    data class TokenExpired(
        override val debugMessage: String,
        override val throwable: Throwable? = null,
    ) : CaroAuthException(
            message = "Token Expired",
            debugMessage = debugMessage,
            throwable = throwable,
        ),
        SilentlyHandledException

    data class TokenEmpty(
        override val debugMessage: String,
        override val throwable: Throwable? = null,
    ) : CaroAuthException(
            message = "Token is Empty",
            debugMessage = debugMessage,
            throwable = throwable,
        )
}
