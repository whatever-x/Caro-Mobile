package com.whatever.caro.core.remote.datasource.card

import com.whatever.caro.core.remote.dto.cardController.request.CreateCardsRequest
import com.whatever.caro.core.remote.dto.cardController.request.UpdateCardRequest
import com.whatever.caro.core.remote.dto.cardController.response.CardResponse
import com.whatever.caro.core.remote.dto.cardController.response.CreateCardsResponse
import com.whatever.caro.core.remote.dto.cardController.response.UpdateCardResponse

interface CardDataSource {
    suspend fun getCardsByDeck(deckId: Long): List<CardResponse>

    suspend fun createCards(
        deckId: Long,
        request: CreateCardsRequest,
    ): CreateCardsResponse

    suspend fun updateCard(
        cardId: Long,
        request: UpdateCardRequest,
    ): UpdateCardResponse
}
