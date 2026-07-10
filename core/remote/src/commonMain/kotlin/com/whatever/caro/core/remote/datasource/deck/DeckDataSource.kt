package com.whatever.caro.core.remote.datasource.deck

import com.whatever.caro.core.remote.dto.deck.request.CreateDeckRequest
import com.whatever.caro.core.remote.dto.deck.request.UpdateDeckRequest
import com.whatever.caro.core.remote.dto.deck.response.CreateDeckResponse
import com.whatever.caro.core.remote.dto.deck.response.DeleteDeckResponse
import com.whatever.caro.core.remote.dto.deck.response.UpdateDeckResponse
import com.whatever.caro.core.remote.dto.deckCardInformation.response.DeckListResponse

interface DeckDataSource {
    suspend fun createDeck(request: CreateDeckRequest): CreateDeckResponse

    suspend fun updateDeck(
        deckId: Long,
        request: UpdateDeckRequest,
    ): UpdateDeckResponse

    suspend fun deleteDeck(deckId: Long): DeleteDeckResponse

    suspend fun getDecks(): List<DeckListResponse>
}
