package com.whatever.caro.core.remote.dto.auth.request

import kotlinx.serialization.Serializable

@Serializable
data class TokenRefreshRequest(
    val accessToken: String,
    val refreshToken: String,
)
