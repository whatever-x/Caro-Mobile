package com.whatever.caro.core.remote.datasource.deck

import com.whatever.caro.core.remote.api.DeckApi
import com.whatever.caro.core.remote.dto.deck.request.CreateDeckRequest
import com.whatever.caro.core.remote.dto.deck.response.CreateDeckResponse

internal class RemoteDeckDataSourceImpl(
    private val deckApi: DeckApi,
) : DeckDataSource {
    override suspend fun createDeck(request: CreateDeckRequest): CreateDeckResponse = deckApi.requestCreateDeck(request = request)
}
