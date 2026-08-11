package com.whatever.caro.core.model.streak

data class Streak(
    val status: StreakStatus,
    val currentDays: Int,
)

enum class StreakStatus {
    NOT_STARTED,
    ACTIVE,
    BROKEN,
}
