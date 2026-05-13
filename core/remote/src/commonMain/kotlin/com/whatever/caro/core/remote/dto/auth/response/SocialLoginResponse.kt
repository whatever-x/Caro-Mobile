package com.whatever.caro.core.remote.dto.auth.response

import kotlinx.serialization.Serializable

@Serializable
data class SocialLoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val isRegistrationComplete: Boolean,
)
