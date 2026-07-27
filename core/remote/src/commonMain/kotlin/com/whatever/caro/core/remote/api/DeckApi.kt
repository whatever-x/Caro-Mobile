package com.whatever.caro.core.remote.api

import com.whatever.caro.core.remote.dto.deck.request.CreateDeckRequest
import com.whatever.caro.core.remote.dto.deck.request.UpdateDeckRequest
import com.whatever.caro.core.remote.dto.deck.response.CreateDeckResponse
import com.whatever.caro.core.remote.dto.deck.response.DeckCardResponse
import com.whatever.caro.core.remote.dto.deck.response.DeckListResponse
import com.whatever.caro.core.remote.dto.deck.response.DeleteDeckResponse
import com.whatever.caro.core.remote.dto.deck.response.UpdateDeckResponse
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.DELETE
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.PATCH
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Path
import de.jensklingenberg.ktorfit.http.Query

internal interface DeckApi {
    @GET("v1/decks")
    suspend fun requestDecks(): List<DeckListResponse>

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

    @GET("v2/decks/{deckId}/cards")
    suspend fun requestCardsByDeck(
        @Path("deckId") deckId: Long,
        @Query("sortType") sortType: String?,
    ): List<DeckCardResponse>
}
