package com.whatever.caro.core.remote.datasource.card

import com.whatever.caro.core.remote.dto.cardController.request.CreateCardsRequest
import com.whatever.caro.core.remote.dto.cardController.response.CreateCardsResponse

interface CardDataSource {
    suspend fun createCards(
        deckId: Long,
        request: CreateCardsRequest,
    ): CreateCardsResponse
}
