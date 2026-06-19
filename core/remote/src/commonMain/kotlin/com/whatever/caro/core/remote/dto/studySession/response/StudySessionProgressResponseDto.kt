package com.whatever.caro.core.remote.dto.studySession.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// AUTO-GENERATED FROM SWAGGER — 직접 수정하지 마세요
@Serializable
data class StudySessionProgressResponseDto(
    val state: StateDto?,
    val sessionId: Long?,
    val studiedCardCount: Int?,
    val totalCardCount: Int?,
) {
    @Serializable
    enum class StateDto {
        @SerialName("NOT_STARTED")
        NOT_STARTED,

        @SerialName("IN_PROGRESS")
        IN_PROGRESS,

        @SerialName("COMPLETED")
        COMPLETED,

        @SerialName("REST_DAY")
        REST_DAY,
    }
}
