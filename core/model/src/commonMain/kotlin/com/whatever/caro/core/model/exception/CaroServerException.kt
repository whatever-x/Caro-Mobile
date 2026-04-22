package com.whatever.caro.core.model.exception

data class CaroServerException(
    override val code: String,
    override val message: String,
    override val debugMessage: String,
    val description: String? = null,
) : CaroException(code, message, debugMessage)
