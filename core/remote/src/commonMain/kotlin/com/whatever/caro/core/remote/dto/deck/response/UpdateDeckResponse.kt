package com.whatever.caro.core.remote.dto.deck.response

import kotlinx.serialization.Serializable

// AUTO-GENERATED FROM SWAGGER — 직접 수정하지 마세요
@Serializable
data class UpdateDeckResponse(
    val id: Long? = null,
    val deckName: String? = null,
    val deckDescription: String? = null,
)
