package com.whatever.caro.core.remote.dto.streak.response

import kotlinx.serialization.Serializable

// AUTO-GENERATED FROM SWAGGER — 직접 수정하지 마세요
@Serializable
data class StreakResponse(
    /** 오늘 기준 현재 연속 학습일 수 */
    val currentStreak: Int?,
)
