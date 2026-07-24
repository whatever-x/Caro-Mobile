package com.whatever.caro.core.data.repository.streak

import com.whatever.caro.core.data.mapper.toStreakModel
import com.whatever.caro.core.model.streak.Streak
import com.whatever.caro.core.remote.datasource.streak.StreakDataSource

internal class StreakRepositoryImpl(
    private val streakDataSource: StreakDataSource,
) : StreakRepository {
    override suspend fun getStreak(): Streak = streakDataSource.getStreak().toStreakModel()
}
