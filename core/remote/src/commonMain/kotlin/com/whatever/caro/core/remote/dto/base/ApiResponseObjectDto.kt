package com.whatever.caro.core.remote.dto.base

import com.whatever.caro.core.remote.dto.error.ErrorDetailDto
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponseObjectDto<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorDetailDto? = null,
)
