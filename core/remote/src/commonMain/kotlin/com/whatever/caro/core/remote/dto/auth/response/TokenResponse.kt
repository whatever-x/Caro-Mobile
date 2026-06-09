package com.whatever.caro.core.remote.dto.auth.response

import kotlinx.serialization.Serializable

// AUTO-GENERATED FROM SWAGGER — 직접 수정하지 마세요
@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
)
