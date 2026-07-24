package com.whatever.caro.core.data.repository.streak

import com.whatever.caro.core.model.streak.Streak

interface StreakRepository {
    suspend fun getStreak(): Streak
}
