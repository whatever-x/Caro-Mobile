package com.whatever.caro.core.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class CaroBaseResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: CaroErrorResponse? = null,
)
