package com.whatever.caro.core.data.repository.card

import com.whatever.caro.core.model.card.Card
import com.whatever.caro.core.model.card.CardContent
import com.whatever.caro.core.remote.datasource.card.CardDataSource
import com.whatever.caro.core.remote.dto.cardController.request.CreateCardItemDto
import com.whatever.caro.core.remote.dto.cardController.request.CreateCardItemDto.CardTypeDto
import com.whatever.caro.core.remote.dto.cardController.request.CreateCardsRequest
import com.whatever.caro.core.remote.dto.cardController.request.DeleteCardsRequest
import com.whatever.caro.core.remote.dto.cardController.request.UpdateCardRequest
import com.whatever.caro.core.remote.dto.cardController.response.CardResponse

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
        cardDataSource
            .getCards(deckId = deckId)
            .mapNotNull { response -> response.toModel() }

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

    private fun CardResponse.toModel(): Card? {
        val id = cardId ?: return null
        return Card(
            id = id,
            content = fields.toCardContent(),
        )
    }

    private fun CardContent.toFields(): Map<String, String> =
        mapOf(
            FIELD_FRONT to front,
            FIELD_BACK to back,
        )

    private fun Map<String, String>?.toCardContent(): CardContent =
        CardContent(
            front = this?.get(FIELD_FRONT).orEmpty(),
            back = this?.get(FIELD_BACK).orEmpty(),
        )

    private companion object {
        const val FIELD_FRONT = "front"
        const val FIELD_BACK = "back"
    }
}
