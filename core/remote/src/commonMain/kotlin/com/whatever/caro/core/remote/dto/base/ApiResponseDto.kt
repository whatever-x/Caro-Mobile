package com.whatever.caro.core.remote.dto.base

import com.whatever.caro.core.remote.dto.error.ErrorDetailDto
import kotlinx.serialization.Serializable

// AUTO-GENERATED FROM SWAGGER — 직접 수정하지 마세요
@Serializable
data class ApiResponseDto<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorDetailDto? = null,
)
