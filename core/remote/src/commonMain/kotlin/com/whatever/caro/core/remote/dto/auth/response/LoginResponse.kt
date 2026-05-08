package com.whatever.caro.core.remote.dto.auth.response

import kotlinx.serialization.Serializable

// FIXME : Swagger 업데이트 이후 확인
@Serializable
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
)
