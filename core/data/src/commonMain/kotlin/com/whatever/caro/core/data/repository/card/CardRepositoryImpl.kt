package com.whatever.caro.core.data.repository.card

import com.whatever.caro.core.model.card.Card
import com.whatever.caro.core.model.card.CardContent
import com.whatever.caro.core.remote.datasource.card.CardDataSource
import com.whatever.caro.core.remote.dto.cardController.request.CreateCardItemDto
import com.whatever.caro.core.remote.dto.cardController.request.CreateCardItemDto.CardTypeDto
import com.whatever.caro.core.remote.dto.cardController.request.CreateCardsRequest

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
                            fields =
                                mapOf(
                                    FIELD_FRONT to card.front,
                                    FIELD_BACK to card.back,
                                ),
                            cardType = CardTypeDto.BASIC,
                        )
                    },
            )
        cardDataSource.createCards(deckId = deckId, request = request)
    }

    override suspend fun getCards(deckId: Long): List<Card> =
        cardDataSource.getCards(deckId = deckId).mapNotNull { response ->
            val cardId = response.cardId ?: return@mapNotNull null
            val fields = response.fields.orEmpty()
            Card(
                id = cardId,
                content =
                    CardContent(
                        front = fields[FIELD_FRONT].orEmpty(),
                        back = fields[FIELD_BACK].orEmpty(),
                    ),
            )
        }

    override suspend fun deleteCards(cardIds: List<Long>) {
        cardIds.forEach { cardId ->
            cardDataSource.deleteCard(cardId = cardId)
        }
    }

    private companion object {
        const val FIELD_FRONT = "front"
        const val FIELD_BACK = "back"
    }
}
