package com.whatever.caro.core.datastore.datasource

interface LocalReviewRequestDataSource {
    suspend fun tryReserveRequest(
        nowEpochMilliseconds: Long,
        minimumIntervalMilliseconds: Long,
    ): Boolean
}
