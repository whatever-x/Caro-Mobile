package com.whatever.caro.core.remote.dto.card.request

import kotlinx.serialization.Serializable

// AUTO-GENERATED FROM SWAGGER — 직접 수정하지 마세요
@Serializable
data class CreateCardsRequest(
    /** 생성할 카드 묶음 목록 */
    val items: List<CreateCardItemDto>,
)
