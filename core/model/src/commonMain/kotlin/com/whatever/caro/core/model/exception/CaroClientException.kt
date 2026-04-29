package com.whatever.caro.core.model.exception

data class CaroClientException(
    override val code: String,
    override val message: String,
    override val debugMessage: String,
    override val throwable: Throwable? = null,
) : CaroException(code, message, debugMessage, throwable)