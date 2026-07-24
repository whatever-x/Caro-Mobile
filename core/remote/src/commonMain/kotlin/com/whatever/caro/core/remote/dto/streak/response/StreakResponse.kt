package com.whatever.caro.core.remote.dto.streak.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// AUTO-GENERATED FROM SWAGGER — 직접 수정하지 마세요
@Serializable
data class StreakResponse(
    /** 연속 학습 상태 */
    val status: StatusDto?,
    /** 오늘 기준 현재 연속 학습일 수, status가 ACTIVE가 아니면 항상 0이다 */
    val currentStreak: Int?,
) {
    @Serializable
    enum class StatusDto {
        @SerialName("NOT_STARTED")
        NOT_STARTED,

        @SerialName("ACTIVE")
        ACTIVE,

        @SerialName("BROKEN")
        BROKEN,
    }
}
