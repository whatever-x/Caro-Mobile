package com.whatever.caro.core.model.exception

sealed class CaroException(
    override val message: String,
    open val debugMessage: String,
    open val throwable: Throwable? = null,
) : Exception(message, throwable)
