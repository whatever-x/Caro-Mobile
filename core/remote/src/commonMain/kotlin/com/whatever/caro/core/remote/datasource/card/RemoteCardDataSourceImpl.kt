package com.whatever.caro.core.remote.datasource.card

import com.whatever.caro.core.remote.api.CardControllerApi
import com.whatever.caro.core.remote.dto.cardController.request.CreateCardsRequest
import com.whatever.caro.core.remote.dto.cardController.response.CreateCardsResponse

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
}
