package com.whatever.caro.core.remote.auth

data class AuthTokenSnapshot(
    val accessToken: String?,
    val generation: Long,
    val isRegistrationComplete: Boolean = true,
)

interface AuthTokenProvider {
    suspend fun getSessionSnapshot(): AuthTokenSnapshot

    suspend fun getAccessToken(): String?

    suspend fun getRefreshToken(): String?

    suspend fun refresh(expectedGeneration: Long): String

    suspend fun clearTokens()
}
