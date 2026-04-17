package com.whatever.caro.core.remote.model

data class NetworkException(
    val code: String,
    override val message: String,
    val debugMessage: String,
    val description: String?,
) : Exception(message)
