package com.whatever.caro.core.data.repository.card

import com.whatever.caro.core.data.mapper.toFields
import com.whatever.caro.core.model.card.CardContent
import com.whatever.caro.core.model.exception.CaroInvalidResponseException
import com.whatever.caro.core.remote.datasource.card.CardDataSource
import com.whatever.caro.core.remote.dto.card.request.CreateCardItemDto
import com.whatever.caro.core.remote.dto.card.request.CreateCardItemDto.CardTypeDto
import com.whatever.caro.core.remote.dto.card.request.CreateCardsRequest
import com.whatever.caro.core.remote.dto.card.request.DeleteCardsRequest
import com.whatever.caro.core.remote.dto.card.request.UpdateCardRequest

internal class CardRepositoryImpl(
    private val cardDataSource: CardDataSource,
) : CardRepository {
    override suspend fun createCards(
        deckId: Long,
        cards: List<CardContent>,
    ) {
        val request =
            CreateCardsRequest(
                items =
                    cards.map { card ->
                        CreateCardItemDto(
                            fields = card.toFields(),
                            cardType = CardTypeDto.BASIC,
                        )
                    },
            )
        cardDataSource.createCards(deckId = deckId, request = request)
    }

    override suspend fun updateCard(
        cardId: Long,
        content: CardContent,
    ) {
        val request =
            UpdateCardRequest(
                fields = content.toFields(),
            )
        cardDataSource.updateCard(cardId = cardId, request = request)
    }

    override suspend fun deleteCards(cardIds: List<Long>) {
        val uniqueCardIds = cardIds.toSet()
        val response =
            cardDataSource.deleteCards(
                request = DeleteCardsRequest(cardIds = uniqueCardIds),
            )
        val deletedCardsCount =
            response.deletedCardsCount
                ?: throw CaroInvalidResponseException(
                    debugMessage = "DeleteCardResponse.deletedCardsCount is null",
                )

        if (deletedCardsCount != uniqueCardIds.size) {
            throw CaroInvalidResponseException(
                debugMessage =
                    "Deleted card count does not match request: " +
                        "requested=${uniqueCardIds.size}, deleted=$deletedCardsCount",
            )
        }
    }
}
