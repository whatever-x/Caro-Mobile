package com.whatever.caro.core.remote.dto.studySession.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// AUTO-GENERATED FROM SWAGGER — 직접 수정하지 마세요
@Serializable
data class EvaluatedCardRequest(
    val cardId: Long?,
    val rating: RatingDto?,
    val timeMs: Int?,
) {
    @Serializable
    enum class RatingDto {
        @SerialName("AGAIN")
        AGAIN,

        @SerialName("FAIR")
        FAIR,

        @SerialName("EASY")
        EASY,
    }
}
