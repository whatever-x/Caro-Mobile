package com.whatever.caro.core.datastore.datasource

interface LocalInterstitialFrequencyDataSource {
    suspend fun reserve(localDate: String): String?

    suspend fun confirmShown(
        reservationId: String,
        localDate: String,
    )

    suspend fun release(reservationId: String)
}
