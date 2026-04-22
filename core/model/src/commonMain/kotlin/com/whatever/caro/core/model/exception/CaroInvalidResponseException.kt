package com.whatever.caro.core.model.exception

data class CaroInvalidResponseException(
    override val code: String,
    override val message: String,
    override val debugMessage: String,
) : CaroException(code, message, debugMessage)
