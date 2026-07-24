package com.whatever.caro.core.data.mapper

import com.whatever.caro.core.model.streak.Streak
import com.whatever.caro.core.model.streak.StreakStatus
import com.whatever.caro.core.remote.dto.streak.response.StreakResponse

internal fun StreakResponse.toStreakModel(): Streak {
    val status =
        when (status) {
            StreakResponse.StatusDto.ACTIVE -> StreakStatus.ACTIVE

            StreakResponse.StatusDto.BROKEN -> StreakStatus.BROKEN

            StreakResponse.StatusDto.NOT_STARTED,
            null,
            -> StreakStatus.NOT_STARTED
        }
    return Streak(
        status = status,
        currentDays =
            if (status == StreakStatus.ACTIVE) {
                currentStreak?.coerceAtLeast(0) ?: 0
            } else {
                0
            },
    )
}
