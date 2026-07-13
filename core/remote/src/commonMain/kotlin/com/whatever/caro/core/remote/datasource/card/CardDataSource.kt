package com.whatever.caro.core.remote.datasource.card

import com.whatever.caro.core.remote.dto.cardController.request.CreateCardsRequest
import com.whatever.caro.core.remote.dto.cardController.response.CardResponse
import com.whatever.caro.core.remote.dto.cardController.response.CreateCardsResponse
import com.whatever.caro.core.remote.dto.cardController.response.DeleteCardResponse

interface CardDataSource {
    suspend fun createCards(
        deckId: Long,
        request: CreateCardsRequest,
    ): CreateCardsResponse

    suspend fun getCards(deckId: Long): List<CardResponse>

    suspend fun deleteCard(cardId: Long): DeleteCardResponse
}
