package com.whatever.caro.core.remote.api

import com.whatever.caro.core.remote.dto.cardController.request.CreateCardsRequest
import com.whatever.caro.core.remote.dto.cardController.request.UpdateCardRequest
import com.whatever.caro.core.remote.dto.cardController.response.CardResponse
import com.whatever.caro.core.remote.dto.cardController.response.CreateCardsResponse
import com.whatever.caro.core.remote.dto.cardController.response.DeleteCardResponse
import com.whatever.caro.core.remote.dto.cardController.response.UpdateCardResponse
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.DELETE
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.PATCH
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Path

internal interface CardControllerApi {
    @POST("v1/decks/{deckId}/cards")
    suspend fun requestCreateCards(
        @Path("deckId") deckId: Long,
        @Body request: CreateCardsRequest,
    ): CreateCardsResponse

    @GET("v1/decks/{deckId}/cards")
    suspend fun requestCardsByDeck(
        @Path("deckId") deckId: Long,
    ): List<CardResponse>

    @GET("v1/cards/{id}")
    suspend fun requestCard(
        @Path("id") id: Long,
    ): CardResponse

    @PATCH("v1/cards/{id}")
    suspend fun requestUpdateCard(
        @Path("id") id: Long,
        @Body request: UpdateCardRequest,
    ): UpdateCardResponse

    @DELETE("v1/cards/{id}")
    suspend fun requestDeleteCard(
        @Path("id") id: Long,
    ): DeleteCardResponse
}
