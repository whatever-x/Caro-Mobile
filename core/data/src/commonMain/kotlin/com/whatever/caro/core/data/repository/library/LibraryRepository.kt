package com.whatever.caro.core.data.repository.library

import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.model.library.LibraryDeck
import com.whatever.caro.core.model.library.LibraryDetail

interface LibraryRepository {
    suspend fun list(): List<LibraryDeck>

    suspend fun detail(libraryDeckId: Long): LibraryDetail

    suspend fun copy(
        libraryDeckId: Long,
        requestKey: String,
    ): Deck
}
