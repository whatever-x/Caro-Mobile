package com.whatever.caro.core.data.repository.ads

interface InterstitialFrequencyRepository {
    suspend fun reserve(localDate: String): String?

    suspend fun confirmShown(
        reservationId: String,
        localDate: String,
    )

    suspend fun release(reservationId: String)
}
