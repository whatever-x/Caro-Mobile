package com.whatever.caro.core.remote.dto.cardController.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// AUTO-GENERATED FROM SWAGGER — 직접 수정하지 마세요
@Serializable
data class CreateCardItem(
    /** 카드 필드 */
    val fields: Map<String, String>,
    /** 카드 타입 */
    val cardType: CardTypeDto?,
) {
    @Serializable
    enum class CardTypeDto {
        @SerialName("BASIC")
        BASIC,
    }
}
