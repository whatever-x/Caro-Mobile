package com.whatever.caro.core.remote.api

import com.whatever.caro.core.remote.dto.card.request.CreateCardsRequest
import com.whatever.caro.core.remote.dto.card.request.DeleteCardsRequest
import com.whatever.caro.core.remote.dto.card.request.UpdateCardRequest
import com.whatever.caro.core.remote.dto.card.response.CardResponse
import com.whatever.caro.core.remote.dto.card.response.CreateCardsResponse
import com.whatever.caro.core.remote.dto.card.response.DeleteCardResponse
import com.whatever.caro.core.remote.dto.card.response.UpdateCardResponse
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.DELETE
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Headers
import de.jensklingenberg.ktorfit.http.PATCH
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Path

internal interface CardApi {
    @Headers(ApiVersionHeaders.V1_0)
    @POST("decks/{deckId}/cards")
    suspend fun requestCreateCards(
        @Path("deckId") deckId: Long,
        @Body request: CreateCardsRequest,
    ): CreateCardsResponse

    @Headers(ApiVersionHeaders.V1_0)
    @GET("cards/{id}")
    suspend fun requestCard(
        @Path("id") id: Long,
    ): CardResponse

    @Headers(ApiVersionHeaders.V1_0)
    @PATCH("cards/{id}")
    suspend fun requestUpdateCard(
        @Path("id") id: Long,
        @Body request: UpdateCardRequest,
    ): UpdateCardResponse

    @Headers(ApiVersionHeaders.V1_0)
    @DELETE("cards")
    suspend fun requestDeleteCards(
        @Body request: DeleteCardsRequest,
    ): DeleteCardResponse
}
