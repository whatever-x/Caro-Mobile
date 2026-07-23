package com.whatever.caro.core.data.repository.card

import com.whatever.caro.core.data.mapper.toFields
import com.whatever.caro.core.model.card.CardContent
import com.whatever.caro.core.remote.datasource.card.CardDataSource
import com.whatever.caro.core.remote.dto.cardController.request.CreateCardItemDto
import com.whatever.caro.core.remote.dto.cardController.request.CreateCardItemDto.CardTypeDto
import com.whatever.caro.core.remote.dto.cardController.request.CreateCardsRequest
import com.whatever.caro.core.remote.dto.cardController.request.DeleteCardsRequest
import com.whatever.caro.core.remote.dto.cardController.request.UpdateCardRequest

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
        val request = DeleteCardsRequest(cardIds = cardIds.toSet())
        cardDataSource.deleteCards(request = request)
    }
}
