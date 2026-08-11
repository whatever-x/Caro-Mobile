package com.whatever.caro.core.model.exception

data class CaroServerException(
    val code: String,
    override val message: String,
    override val debugMessage: String,
    override val throwable: Throwable? = null,
) : CaroException(message, debugMessage, throwable)
