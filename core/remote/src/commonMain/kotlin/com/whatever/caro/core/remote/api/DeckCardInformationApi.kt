package com.whatever.caro.core.remote.api

import com.whatever.caro.core.remote.dto.deckCardInformation.response.DeckCardResponse
import com.whatever.caro.core.remote.dto.deckCardInformation.response.DeckListResponse
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Path

internal interface DeckCardInformationApi {
    @GET("v1/decks")
    suspend fun requestDecks(): List<DeckListResponse>

    @GET("v2/decks/{deckId}/cards")
    suspend fun requestCardsByDeck(
        @Path("deckId") deckId: Long,
    ): List<DeckCardResponse>
}
