package com.whatever.caro.core.data.repository.review

import com.whatever.caro.core.datastore.datasource.LocalReviewRequestDataSource

internal class ReviewRequestRepositoryImpl(
    private val localReviewRequestDataSource: LocalReviewRequestDataSource,
) : ReviewRequestRepository {
    override suspend fun tryReserveRequest(
        nowEpochMilliseconds: Long,
        minimumIntervalMilliseconds: Long,
    ): Boolean = localReviewRequestDataSource.tryReserveRequest(nowEpochMilliseconds, minimumIntervalMilliseconds)
}
