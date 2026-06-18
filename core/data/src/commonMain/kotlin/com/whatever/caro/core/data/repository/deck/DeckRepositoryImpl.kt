package com.whatever.caro.core.data.repository.deck

import com.whatever.caro.core.remote.datasource.deck.DeckDataSource
import com.whatever.caro.core.remote.dto.deck.request.CreateDeckRequest

internal class DeckRepositoryImpl(
    private val deckDataSource: DeckDataSource,
) : DeckRepository {
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
}
