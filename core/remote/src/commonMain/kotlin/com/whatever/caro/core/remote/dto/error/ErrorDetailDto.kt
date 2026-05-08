package com.whatever.caro.core.remote.dto.error

import kotlinx.serialization.Serializable

@Serializable
data class ErrorDetailDto(
    val code: String,
    val message: String,
    val debugMessage: String? = null,
    val description: String? = null,
    val traceId: String? = null,
    val fieldErrors: List<FieldErrorDto>? = null,
)
