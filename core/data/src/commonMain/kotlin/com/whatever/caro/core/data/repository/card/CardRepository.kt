package com.whatever.caro.core.data.repository.card

import com.whatever.caro.core.model.card.Card
import com.whatever.caro.core.model.card.CardContent

interface CardRepository {
    suspend fun getCardsByDeck(deckId: Long): List<Card>

    suspend fun createCards(
        deckId: Long,
        cards: List<CardContent>,
    )

    suspend fun updateCard(
        cardId: Long,
        content: CardContent,
    )
}
