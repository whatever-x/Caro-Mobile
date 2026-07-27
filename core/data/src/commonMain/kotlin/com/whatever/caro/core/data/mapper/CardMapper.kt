package com.whatever.caro.core.data.mapper

import com.whatever.caro.core.model.card.Card
import com.whatever.caro.core.model.card.CardContent
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

private fun Map<String, String>?.toCardContent(): CardContent =
    CardContent(
        front = this?.get(FIELD_FRONT).orEmpty(),
        back = this?.get(FIELD_BACK).orEmpty(),
    )
