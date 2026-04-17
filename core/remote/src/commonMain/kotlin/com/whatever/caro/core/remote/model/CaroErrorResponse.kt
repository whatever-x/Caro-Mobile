package com.whatever.caro.core.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class CaroErrorResponse(
    val code: String,
    val message: String,
    val debugMessage: String? = null,
    val description: String? = null,
)
