package com.whatever.caro.core.data.repository.deck

import com.whatever.caro.core.model.deck.Deck

interface DeckRepository {
    suspend fun getDecks(): List<Deck>

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
