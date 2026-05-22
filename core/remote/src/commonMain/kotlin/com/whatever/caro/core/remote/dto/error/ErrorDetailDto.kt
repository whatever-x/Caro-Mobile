package com.whatever.caro.core.remote.dto.error

import kotlinx.serialization.Serializable

@Serializable
data class ErrorDetailDto(
    val code: String,
    val message: String,
    val traceId: String?,
    val fieldErrors: List<FieldErrorDto>?,
)
