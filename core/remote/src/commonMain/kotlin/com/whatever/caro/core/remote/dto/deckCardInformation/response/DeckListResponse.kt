package com.whatever.caro.core.remote.dto.deckCardInformation.response

import com.whatever.caro.core.remote.dto.studySession.response.StudySessionProgressResponseDto
import kotlinx.serialization.Serializable

// AUTO-GENERATED FROM SWAGGER — 직접 수정하지 마세요
@Serializable
data class DeckListResponse(
    val deckId: Long?,
    val name: String?,
    val description: String?,
    val cardCount: Int?,
    val progress: StudySessionProgressResponseDto?,
)
