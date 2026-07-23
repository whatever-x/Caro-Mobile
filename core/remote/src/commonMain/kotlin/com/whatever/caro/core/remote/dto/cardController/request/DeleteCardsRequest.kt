package com.whatever.caro.core.remote.dto.cardController.request

import kotlinx.serialization.Serializable

// AUTO-GENERATED FROM SWAGGER — 직접 수정하지 마세요
@Serializable
data class DeleteCardsRequest(
    /** 카드 ID 들 (최소 1개 - 최대 1000개) */
    val cardIds: Set<Long>,
)
