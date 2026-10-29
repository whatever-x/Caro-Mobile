package com.whatever.caro.core.data.mapper

import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.model.deck.DeckState
import com.whatever.caro.core.model.library.LibraryCard
import com.whatever.caro.core.model.library.LibraryDeck
import com.whatever.caro.core.model.library.LibraryDetail
import com.whatever.caro.core.remote.dto.library.LibraryCopyResponse
import com.whatever.caro.core.remote.dto.library.LibraryDeckResponse
import com.whatever.caro.core.remote.dto.library.LibraryDetailResponse

internal fun LibraryDeckResponse.toModel() = LibraryDeck(libraryDeckId, name, description, cardCount)

internal fun LibraryDetailResponse.toModel() =
    LibraryDetail(
        LibraryDeck(libraryDeckId, name, description, cardCount),
        cards
            .sortedWith(
                compareBy({ it.position }, { it.libraryCardId }),
            ).map { LibraryCard(it.libraryCardId, it.front, it.back, it.position) },
    )

internal fun LibraryCopyResponse.toPersonalDeck() = Deck(deckId, name, description, cardCount, 0, 0, DeckState.NOT_STARTED)
