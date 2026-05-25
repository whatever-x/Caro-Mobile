package com.whatever.caro.core.datastore.datasource

interface LocalAuthDataSource {
    suspend fun fetchAccessToken(): String?

    suspend fun fetchRefreshToken(): String?

    suspend fun saveTokens(
        accessToken: String,
        refreshToken: String,
    )

    suspend fun clear()
}
