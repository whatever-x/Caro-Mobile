package com.whatever.caro.core.remote.dto.deck.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// AUTO-GENERATED FROM SWAGGER — 직접 수정하지 마세요
@Serializable
data class DeckCardResponse(
    val cardId: Long?,
    val fields: Map<String, String>?,
    val badge: BadgeDto?,
    val reviewCount: Int?,
) {
    @Serializable
    enum class BadgeDto {
        @SerialName("NEW")
        NEW,

        @SerialName("REVIEW")
        REVIEW,

        @SerialName("HARD")
        HARD,
    }
}
