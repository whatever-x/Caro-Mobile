package com.whatever.caro.core.data.repository

interface FcmTokenRepository {
    suspend fun syncToken(token: String)
}
