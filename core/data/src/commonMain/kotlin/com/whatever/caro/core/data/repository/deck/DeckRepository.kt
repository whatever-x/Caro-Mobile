package com.whatever.caro.core.data.repository.deck

interface DeckRepository {
    suspend fun createDeck(
        name: String,
        description: String,
    )
}
