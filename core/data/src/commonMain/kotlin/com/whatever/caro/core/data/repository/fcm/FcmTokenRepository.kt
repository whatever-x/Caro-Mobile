package com.whatever.caro.core.data.repository.fcm

import kotlinx.coroutines.flow.Flow

interface FcmTokenRepository {
    suspend fun syncTokens(tokens: Flow<String>)

    suspend fun resumeRegistration()

    suspend fun unregisterToken()
}
