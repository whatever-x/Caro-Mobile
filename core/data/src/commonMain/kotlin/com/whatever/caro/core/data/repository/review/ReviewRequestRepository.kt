package com.whatever.caro.core.data.repository.review

interface ReviewRequestRepository {
    /** Atomically claims the app-wide interval, preserving the previous timestamp for this ID. */
    suspend fun tryReserveRequest(
        reservationId: String,
        nowEpochMilliseconds: Long,
        minimumIntervalMilliseconds: Long,
    ): Boolean

    /** Finalizes only the matching reservation after an SDK invocation was attempted. */
    suspend fun commitRequest(
        reservationId: String,
        attemptedAtEpochMilliseconds: Long,
    ): Boolean

    /** Restores the previous timestamp only while this ID still owns the reservation. */
    suspend fun cancelRequest(reservationId: String): Boolean
}
