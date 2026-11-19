package com.whatever.caro.core.data.repository.ads

import com.whatever.caro.core.datastore.datasource.LocalInterstitialFrequencyDataSource

internal class InterstitialFrequencyRepositoryImpl(
    private val localInterstitialFrequencyDataSource: LocalInterstitialFrequencyDataSource,
) : InterstitialFrequencyRepository {
    override suspend fun reserve(localDate: String): String? = localInterstitialFrequencyDataSource.reserve(localDate)

    override suspend fun confirmShown(
        reservationId: String,
        localDate: String,
    ) {
        localInterstitialFrequencyDataSource.confirmShown(reservationId, localDate)
    }

    override suspend fun release(reservationId: String) {
        localInterstitialFrequencyDataSource.release(reservationId)
    }
}
