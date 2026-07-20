package com.whatever.caro.core.remote.dto.studySession.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// AUTO-GENERATED FROM SWAGGER — 직접 수정하지 마세요
@Serializable
data class EvaluationResponse(
    val evaluatedCardIds: List<Long>?,
    val failedCardIds: List<Long>?,
    val sessionStatus: SessionStatusDto?,
    /** 세션 전체 누적 등급별 카운트(중단 이전 평가 포함). again=모르겠어요, fair=애매해요, easy=쉬워요 */
    val ratingCounts: RatingCountsDto?,
) {
    @Serializable
    enum class SessionStatusDto {
        @SerialName("ACTIVE")
        ACTIVE,

        @SerialName("COMPLETED")
        COMPLETED,

        @SerialName("STOPPED")
        STOPPED,
    }
}
