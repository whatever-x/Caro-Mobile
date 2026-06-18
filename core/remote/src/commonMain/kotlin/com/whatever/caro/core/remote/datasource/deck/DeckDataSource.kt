package com.whatever.caro.core.remote.datasource.deck

import com.whatever.caro.core.remote.dto.deck.request.CreateDeckRequest
import com.whatever.caro.core.remote.dto.deck.response.CreateDeckResponse

interface DeckDataSource {
    suspend fun createDeck(request: CreateDeckRequest): CreateDeckResponse
}
