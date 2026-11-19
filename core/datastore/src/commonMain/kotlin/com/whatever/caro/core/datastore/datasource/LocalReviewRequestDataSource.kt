package com.whatever.caro.core.datastore.datasource

interface LocalReviewRequestDataSource {
    suspend fun tryReserveRequest(
        reservationId: String,
        nowEpochMilliseconds: Long,
        minimumIntervalMilliseconds: Long,
    ): Boolean

    suspend fun commitRequest(
        reservationId: String,
        attemptedAtEpochMilliseconds: Long,
    ): Boolean

    suspend fun cancelRequest(reservationId: String): Boolean
}
