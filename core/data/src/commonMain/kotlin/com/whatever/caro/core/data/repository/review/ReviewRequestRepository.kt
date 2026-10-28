package com.whatever.caro.core.data.repository.review

interface ReviewRequestRepository {
    /** Atomically persists an app-wide request reservation when the interval has elapsed. */
    suspend fun tryReserveRequest(
        nowEpochMilliseconds: Long,
        minimumIntervalMilliseconds: Long,
    ): Boolean
}
