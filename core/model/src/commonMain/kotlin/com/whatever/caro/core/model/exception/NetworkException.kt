package com.whatever.caro.core.model.exception

sealed class NetworkException(
    message: String,
    debugMessage: String,
    throwable: Throwable? = null,
) : CaroException(message, debugMessage, throwable) {
    data class Connection(
        override val debugMessage: String,
        override val throwable: Throwable? = null,
    ) : NetworkException(
            message = "Network Error",
            debugMessage = debugMessage,
            throwable = throwable,
        )

    data class Timeout(
        override val debugMessage: String,
        override val throwable: Throwable? = null,
    ) : NetworkException(
            message = "Network Timeout Error",
            debugMessage = debugMessage,
            throwable = throwable,
        )
}
