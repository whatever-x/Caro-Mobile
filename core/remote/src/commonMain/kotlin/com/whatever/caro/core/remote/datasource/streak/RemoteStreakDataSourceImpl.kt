package com.whatever.caro.core.remote.datasource.streak

import com.whatever.caro.core.remote.api.StreakApi
import com.whatever.caro.core.remote.dto.streak.response.StreakResponse

internal class RemoteStreakDataSourceImpl(
    private val streakApi: StreakApi,
) : StreakDataSource {
    override suspend fun getStreak(): StreakResponse = streakApi.requestStreak()

    override suspend fun syncStreak() = streakApi.requestSyncStreak()
}
