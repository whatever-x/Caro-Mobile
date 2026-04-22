package com.whatever.caro.core.model.exception

sealed class CaroException(
    open val code: String,
    override val message: String,
    open val debugMessage: String,
) : Exception(message)
