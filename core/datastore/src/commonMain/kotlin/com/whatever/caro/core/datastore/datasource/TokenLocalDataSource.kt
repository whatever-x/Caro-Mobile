package com.whatever.caro.core.datastore.datasource

interface TokenLocalDataSource {
    suspend fun fetchAccessToken(): String?

    suspend fun fetchRefreshToken(): String?

    suspend fun saveTokens(
        accessToken: String,
        refreshToken: String,
    )

    suspend fun clear()
}