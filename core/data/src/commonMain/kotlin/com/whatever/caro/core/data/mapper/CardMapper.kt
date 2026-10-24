package com.whatever.caro.core.data.mapper

import com.whatever.caro.core.model.card.Card
import com.whatever.caro.core.model.card.CardContent
import com.whatever.caro.core.remote.dto.card.request.CreateCardItemDto
import com.whatever.caro.core.remote.dto.card.request.CreateCardItemDto.CardTypeDto
import com.whatever.caro.core.remote.dto.card.request.CreateCardsRequest
import com.whatever.caro.core.remote.dto.card.response.CardResponse

private const val FIELD_FRONT = "front"
private const val FIELD_BACK = "back"

internal fun CardResponse.toModel(): Card? {
    val id = cardId ?: return null
    return Card(
        id = id,
        content = fields.toCardContent(),
    )
}

internal fun CardContent.toFields(): Map<String, String> =
    mapOf(
        FIELD_FRONT to front,
        FIELD_BACK to back,
    )

/** 같은 카드 목록은 항상 같은 순서·같은 필드 순서의 요청이 되어야 멱등 재시도의 본문 바이트가 같아진다. */
internal fun List<CardContent>.toCreateCardsRequest(): CreateCardsRequest =
    CreateCardsRequest(
        items =
            map { card ->
                CreateCardItemDto(
                    fields = card.toFields(),
                    cardType = CardTypeDto.BASIC,
                )
            },
    )

private fun Map<String, String>?.toCardContent(): CardContent =
    CardContent(
        front = this?.get(FIELD_FRONT).orEmpty(),
        back = this?.get(FIELD_BACK).orEmpty(),
    )
