package com.whatever.caro.core.remote.datasource.streak

import com.whatever.caro.core.remote.dto.streak.response.StreakResponse

interface StreakDataSource {
    suspend fun getStreak(): StreakResponse

    suspend fun syncStreak()
}
