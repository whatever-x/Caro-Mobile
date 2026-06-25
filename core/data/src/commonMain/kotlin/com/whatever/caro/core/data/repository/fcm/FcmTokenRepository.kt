package com.whatever.caro.core.data.repository.fcm

interface FcmTokenRepository {
    suspend fun syncToken(token: String)
}
