package com.whatever.caro.core.remote.datasource.card

import com.whatever.caro.core.remote.api.CardApi
import com.whatever.caro.core.remote.dto.card.request.CreateCardsRequest
import com.whatever.caro.core.remote.dto.card.request.DeleteCardsRequest
import com.whatever.caro.core.remote.dto.card.request.UpdateCardRequest
import com.whatever.caro.core.remote.dto.card.response.CardResponse
import com.whatever.caro.core.remote.dto.card.response.CreateCardsResponse
import com.whatever.caro.core.remote.dto.card.response.DeleteCardResponse
import com.whatever.caro.core.remote.dto.card.response.UpdateCardResponse

internal class RemoteCardDataSourceImpl(
    private val cardApi: CardApi,
) : CardDataSource {
    override suspend fun createCards(
        deckId: Long,
        request: CreateCardsRequest,
    ): CreateCardsResponse =
        cardApi.requestCreateCards(
            deckId = deckId,
            request = request,
        )

    override suspend fun getCardsByDeck(deckId: Long): List<CardResponse> = cardApi.requestCardsByDeck(deckId = deckId)

    override suspend fun getCard(cardId: Long): CardResponse = cardApi.requestCard(id = cardId)

    override suspend fun updateCard(
        cardId: Long,
        request: UpdateCardRequest,
    ): UpdateCardResponse =
        cardApi.requestUpdateCard(
            id = cardId,
            request = request,
        )

    override suspend fun deleteCards(request: DeleteCardsRequest): DeleteCardResponse = cardApi.requestDeleteCards(request = request)
}
