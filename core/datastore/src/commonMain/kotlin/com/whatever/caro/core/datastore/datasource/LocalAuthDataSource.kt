package com.whatever.caro.core.datastore.datasource

import kotlinx.coroutines.flow.Flow

data class AuthTokens(
    val accessToken: String?,
    val refreshToken: String?,
    val generation: Long = 0,
    val isRegistrationComplete: Boolean? = null,
)

interface LocalAuthDataSource {
    fun observeRegisteredAccessToken(): Flow<String?>

    fun observeRegisteredSession(): Flow<AuthTokens?>

    suspend fun fetchTokens(): AuthTokens

    suspend fun saveSession(
        accessToken: String,
        refreshToken: String,
        isRegistrationComplete: Boolean,
    )

    suspend fun replaceTokensIfCurrent(
        expected: AuthTokens,
        accessToken: String,
        refreshToken: String,
    ): Boolean

    suspend fun clearIfCurrent(expected: AuthTokens): Boolean

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
