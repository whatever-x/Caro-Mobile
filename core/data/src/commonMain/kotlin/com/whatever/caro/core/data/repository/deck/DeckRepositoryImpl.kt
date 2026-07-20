package com.whatever.caro.core.data.repository.deck

import com.whatever.caro.core.data.mapper.toDeckCardModel
import com.whatever.caro.core.data.mapper.toDeckModel
import com.whatever.caro.core.model.card.Card
import com.whatever.caro.core.model.card.DeckCard
import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.remote.datasource.deck.DeckDataSource
import com.whatever.caro.core.remote.dto.deck.request.CreateDeckRequest
import com.whatever.caro.core.remote.dto.deck.request.UpdateDeckRequest

internal class DeckRepositoryImpl(
    private val deckDataSource: DeckDataSource,
) : DeckRepository {
    override suspend fun getDecks(): List<Deck> {
        val deckResponse = deckDataSource.getDecks()
        return deckResponse.map { it.toDeckModel() }
    }

    override suspend fun getDeckCards(deckId: Long): List<DeckCard> =
        deckDataSource
            .getDeckCards(deckId = deckId)
            .mapNotNull { it.toDeckCardModel() }

    override suspend fun createDeck(
        name: String,
        description: String,
    ) {
        val request =
            CreateDeckRequest(
                name = name,
                description = description,
            )
        deckDataSource.createDeck(request = request)
    }

    override suspend fun updateDeck(
        deckId: Long,
        name: String,
        description: String,
    ) {
        val request =
            UpdateDeckRequest(
                name = name,
                description = description,
            )
        deckDataSource.updateDeck(deckId = deckId, request = request)
    }

    override suspend fun deleteDeck(deckId: Long) {
        deckDataSource.deleteDeck(deckId = deckId)
    }
}
