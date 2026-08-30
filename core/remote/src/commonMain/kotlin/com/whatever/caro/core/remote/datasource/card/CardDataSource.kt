package com.whatever.caro.core.remote.datasource.card

import com.whatever.caro.core.remote.dto.card.request.CreateCardsRequest
import com.whatever.caro.core.remote.dto.card.request.DeleteCardsRequest
import com.whatever.caro.core.remote.dto.card.request.UpdateCardRequest
import com.whatever.caro.core.remote.dto.card.response.CardResponse
import com.whatever.caro.core.remote.dto.card.response.CreateCardsResponse
import com.whatever.caro.core.remote.dto.card.response.DeleteCardResponse
import com.whatever.caro.core.remote.dto.card.response.UpdateCardResponse

interface CardDataSource {
    suspend fun createCards(
        deckId: Long,
        request: CreateCardsRequest,
    ): CreateCardsResponse

    suspend fun getCard(cardId: Long): CardResponse

    suspend fun updateCard(
        cardId: Long,
        request: UpdateCardRequest,
    ): UpdateCardResponse

    suspend fun deleteCards(request: DeleteCardsRequest): DeleteCardResponse
}
