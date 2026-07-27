package com.whatever.caro.core.remote.dto.card.request

import kotlinx.serialization.Serializable

// AUTO-GENERATED FROM SWAGGER — 직접 수정하지 마세요
@Serializable
data class UpdateCardRequest(
    /** 수정할 카드 필드 */
    val fields: Map<String, String>,
)
