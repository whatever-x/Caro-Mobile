package com.whatever.caro.core.remote.datasource.deck

import com.whatever.caro.core.remote.api.DeckApi
import com.whatever.caro.core.remote.dto.deck.request.CreateDeckRequest
import com.whatever.caro.core.remote.dto.deck.request.UpdateDeckRequest
import com.whatever.caro.core.remote.dto.deck.response.CreateDeckResponse
import com.whatever.caro.core.remote.dto.deck.response.DeckCardResponse
import com.whatever.caro.core.remote.dto.deck.response.DeckListResponse
import com.whatever.caro.core.remote.dto.deck.response.DeleteDeckResponse
import com.whatever.caro.core.remote.dto.deck.response.UpdateDeckResponse

internal class RemoteDeckDataSourceImpl(
    private val deckApi: DeckApi,
) : DeckDataSource {
    override suspend fun createDeck(request: CreateDeckRequest): CreateDeckResponse = deckApi.requestCreateDeck(request = request)

    override suspend fun updateDeck(
        deckId: Long,
        request: UpdateDeckRequest,
    ): UpdateDeckResponse =
        deckApi.requestUpdateDeck(
            deckId = deckId,
            request = request,
        )

    override suspend fun deleteDeck(deckId: Long): DeleteDeckResponse = deckApi.requestDeleteDeck(deckId = deckId)

    override suspend fun getDecks(): List<DeckListResponse> = deckApi.requestDecks()

    override suspend fun getDeckCards(
        deckId: Long,
        sortType: String?,
    ): List<DeckCardResponse> =
        deckApi.requestCardsByDeck(
            deckId = deckId,
            sortType = sortType,
        )
}
