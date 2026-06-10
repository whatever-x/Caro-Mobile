package com.whatever.caro.core.remote.dto.deck.response

import kotlinx.serialization.Serializable

// AUTO-GENERATED FROM SWAGGER — 직접 수정하지 마세요
@Serializable
data class CreateDeckResponse(
    /** 덱 ID */
    val id: Long?,
    /** 덱 이름 */
    val deckName: String?,
    /** 덱 설명 */
    val deckDescription: String?,
)
