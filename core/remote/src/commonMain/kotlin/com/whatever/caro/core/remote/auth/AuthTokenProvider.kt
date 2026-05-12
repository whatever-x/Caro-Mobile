package com.whatever.caro.core.remote.auth

interface AuthTokenProvider {
    suspend fun getAccessToken(): String?

    suspend fun getRefreshToken(): String?

    suspend fun refresh(): String

    suspend fun clearTokens()
}