package com.whatever.caro.core.model.auth

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
)
