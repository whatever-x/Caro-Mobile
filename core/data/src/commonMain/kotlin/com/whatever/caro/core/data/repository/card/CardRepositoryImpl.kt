package com.whatever.caro.core.data.repository.card

import com.whatever.caro.core.data.mapper.toCreateCardsRequest
import com.whatever.caro.core.data.mapper.toFields
import com.whatever.caro.core.model.card.CardContent
import com.whatever.caro.core.model.exception.CaroInvalidResponseException
import com.whatever.caro.core.remote.datasource.card.CardDataSource
import com.whatever.caro.core.remote.dto.card.request.DeleteCardsRequest
import com.whatever.caro.core.remote.dto.card.request.UpdateCardRequest

internal class CardRepositoryImpl(
    private val cardDataSource: CardDataSource,
) : CardRepository {
    override suspend fun createCards(
        deckId: Long,
        cards: List<CardContent>,
    ) {
        cardDataSource.createCards(deckId = deckId, request = cards.toCreateCardsRequest())
    }

    override suspend fun createCardsChunk(
        deckId: Long,
        cards: List<CardContent>,
        idempotencyKey: String,
    ): Int {
        val response =
            cardDataSource.createCardChunk(
                deckId = deckId,
                idempotencyKey = idempotencyKey,
                request = cards.toCreateCardsRequest(),
            )
        val createdCount =
            response.items?.size
                ?: throw CaroInvalidResponseException(debugMessage = "CreateCardsResponse.items is null")
        if (createdCount != cards.size) {
            throw CaroInvalidResponseException(
                debugMessage = "Created card count does not match request: requested=${cards.size}, created=$createdCount",
            )
        }
        return createdCount
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
