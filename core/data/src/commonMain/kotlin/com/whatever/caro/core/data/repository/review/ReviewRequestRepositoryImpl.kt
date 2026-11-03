package com.whatever.caro.core.data.repository.review

import com.whatever.caro.core.datastore.datasource.LocalReviewRequestDataSource

internal class ReviewRequestRepositoryImpl(
    private val localReviewRequestDataSource: LocalReviewRequestDataSource,
) : ReviewRequestRepository {
    override suspend fun tryReserveRequest(
        reservationId: String,
        nowEpochMilliseconds: Long,
        minimumIntervalMilliseconds: Long,
    ): Boolean = localReviewRequestDataSource.tryReserveRequest(reservationId, nowEpochMilliseconds, minimumIntervalMilliseconds)

    override suspend fun commitRequest(
        reservationId: String,
        attemptedAtEpochMilliseconds: Long,
    ): Boolean = localReviewRequestDataSource.commitRequest(reservationId, attemptedAtEpochMilliseconds)

    override suspend fun cancelRequest(reservationId: String): Boolean = localReviewRequestDataSource.cancelRequest(reservationId)
}
