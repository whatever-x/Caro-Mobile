package com.whatever.caro.core.datastore.auth

interface TokenLocalDataSource {
    suspend fun getAccessToken(): String?

    suspend fun getRefreshToken(): String?

    suspend fun saveTokens(
        accessToken: String,
        refreshToken: String,
    )

    suspend fun clear()
}
