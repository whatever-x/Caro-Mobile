package com.whatever.caro.core.remote.datasource.card

import com.whatever.caro.core.remote.dto.cardController.request.CreateCardsRequest
import com.whatever.caro.core.remote.dto.cardController.request.DeleteCardsRequest
import com.whatever.caro.core.remote.dto.cardController.request.UpdateCardRequest
import com.whatever.caro.core.remote.dto.cardController.response.CardResponse
import com.whatever.caro.core.remote.dto.cardController.response.CreateCardsResponse
import com.whatever.caro.core.remote.dto.cardController.response.DeleteCardResponse
import com.whatever.caro.core.remote.dto.cardController.response.UpdateCardResponse

interface CardDataSource {
    suspend fun createCards(
        deckId: Long,
        request: CreateCardsRequest,
    ): CreateCardsResponse

    suspend fun getCards(deckId: Long): List<CardResponse>

    suspend fun updateCard(
        cardId: Long,
        request: UpdateCardRequest,
    ): UpdateCardResponse

    suspend fun deleteCards(request: DeleteCardsRequest): DeleteCardResponse
}
