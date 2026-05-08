package com.whatever.caro.core.remote.dto.error

import kotlinx.serialization.Serializable

@Serializable
data class FieldErrorDto(
    val field: String? = null,
    val message: String? = null,
)
