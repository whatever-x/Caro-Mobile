package com.whatever.caro.core.data.repository.deck

import com.whatever.caro.core.model.card.DeckCard
import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.model.deck.DeckCardSortType

interface DeckRepository {
    suspend fun getDecks(): List<Deck>

    suspend fun getDeckCards(deckId: Long): List<DeckCard>

    suspend fun getDeckCards(
        deckId: Long,
        sortType: DeckCardSortType,
    ): List<DeckCard> = getDeckCards(deckId)

    suspend fun createDeck(
        name: String,
        description: String,
    )

    suspend fun updateDeck(
        deckId: Long,
        name: String,
        description: String,
    )

    suspend fun deleteDeck(deckId: Long)
}
