package com.whatever.caro.core.remote.datasource.card

import com.whatever.caro.core.remote.api.CardControllerApi
import com.whatever.caro.core.remote.dto.cardController.request.CreateCardsRequest
import com.whatever.caro.core.remote.dto.cardController.response.CardResponse
import com.whatever.caro.core.remote.dto.cardController.response.CreateCardsResponse
import com.whatever.caro.core.remote.dto.cardController.response.DeleteCardResponse

internal class RemoteCardDataSourceImpl(
    private val cardControllerApi: CardControllerApi,
) : CardDataSource {
    override suspend fun createCards(
        deckId: Long,
        request: CreateCardsRequest,
    ): CreateCardsResponse =
        cardControllerApi.requestCreateCards(
            deckId = deckId,
            request = request,
        )

    override suspend fun getCards(deckId: Long): List<CardResponse> = cardControllerApi.requestCardsByDeck(deckId = deckId)

    override suspend fun deleteCard(cardId: Long): DeleteCardResponse = cardControllerApi.requestDeleteCard(id = cardId)
}
