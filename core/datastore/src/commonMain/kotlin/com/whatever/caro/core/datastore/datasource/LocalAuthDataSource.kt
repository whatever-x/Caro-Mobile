package com.whatever.caro.core.datastore.datasource

interface LocalAuthDataSource {
    suspend fun fetchAccessToken(): String?

    suspend fun fetchRefreshToken(): String?

    suspend fun saveTokens(
        accessToken: String,
        refreshToken: String,
    )

    suspend fun fetchRegistrationComplete(): Boolean?

    suspend fun saveRegistrationComplete(isComplete: Boolean)

    suspend fun clear()
}
