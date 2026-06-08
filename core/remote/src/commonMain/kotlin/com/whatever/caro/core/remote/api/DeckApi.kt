package com.whatever.caro.core.remote.api

import com.whatever.caro.core.remote.dto.deck.request.CreateDeckRequest
import com.whatever.caro.core.remote.dto.deck.request.UpdateDeckRequest
import com.whatever.caro.core.remote.dto.deck.response.CreateDeckResponse
import com.whatever.caro.core.remote.dto.deck.response.DeleteDeckResponse
import com.whatever.caro.core.remote.dto.deck.response.UpdateDeckResponse
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.DELETE
import de.jensklingenberg.ktorfit.http.PATCH
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Path

internal interface DeckApi {
    @POST("v1/decks")
    suspend fun requestCreateDeck(
        @Body request: CreateDeckRequest,
    ): CreateDeckResponse

    @PATCH("v1/decks/{deckId}")
    suspend fun requestUpdateDeck(
        @Path("deckId") deckId: Long,
        @Body request: UpdateDeckRequest,
    ): UpdateDeckResponse

    @DELETE("v1/decks/{deckId}")
    suspend fun requestDeleteDeck(
        @Path("deckId") deckId: Long,
    ): DeleteDeckResponse
}
