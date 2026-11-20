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
import de.jensklingenberg.ktorfit.http.Header
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

    /**
     * 가져오기 전용 묶음 저장. 같은 경로·메서드의 1.0과 Swagger에서 한 operation으로 합쳐져 swagger-sync가 만들 수 없으므로 직접 선언한다.
     * `API-Version: 1.1`이 빠지면 서버가 오류 없이 1.0으로 처리해 1,000장 상한과 멱등 처리가 모두 사라진다.
     */
    @Headers(ApiVersionHeaders.V1_1)
    @POST("decks/{deckId}/cards")
    suspend fun requestCreateCardChunk(
        @Path("deckId") deckId: Long,
        @Header("Idempotency-Key") idempotencyKey: String,
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
