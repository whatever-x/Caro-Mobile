package com.whatever.caro.core.test.api

import com.whatever.caro.core.remote.api.createAuthApi
import com.whatever.caro.core.remote.api.createCardApi
import com.whatever.caro.core.remote.api.createDeckApi
import com.whatever.caro.core.remote.dto.card.request.CreateCardItemDto
import com.whatever.caro.core.remote.dto.card.request.CreateCardsRequest
import com.whatever.caro.core.remote.network.plugins.CaroBaseResponseConverter
import de.jensklingenberg.ktorfit.Ktorfit
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class ApiVersionHeaderTest : FunSpec() {
    init {
        test("1.0 API 요청은 API-Version 1.0 헤더를 전송한다") {
            val request =
                captureRequest(responseData = "null") { ktorfit ->
                    ktorfit.createAuthApi().requestLogout()
                }

            request.headers[API_VERSION_HEADER] shouldBe "1.0"
        }

        test("덱 카드 조회 요청은 API-Version 2.0 헤더를 전송한다") {
            val request =
                captureRequest(responseData = "[]") { ktorfit ->
                    ktorfit.createDeckApi().requestCardsByDeck(
                        deckId = 1L,
                        sortType = null,
                    )
                }

            request.headers[API_VERSION_HEADER] shouldBe "2.0"
        }

        test("가져오기 카드 묶음 저장 요청은 API-Version 1.1과 Idempotency-Key를 전송한다") {
            val request =
                captureRequest(responseData = CREATED_ITEMS) { ktorfit ->
                    ktorfit.createCardApi().requestCreateCardChunk(
                        deckId = 1L,
                        idempotencyKey = IDEMPOTENCY_KEY,
                        request = chunkRequest,
                    )
                }

            request.headers[API_VERSION_HEADER] shouldBe "1.1"
            request.headers[IDEMPOTENCY_KEY_HEADER] shouldBe IDEMPOTENCY_KEY
        }

        test("수동 카드 생성 요청은 Idempotency-Key 없이 API-Version 1.0을 전송한다") {
            val request =
                captureRequest(responseData = CREATED_ITEMS) { ktorfit ->
                    ktorfit.createCardApi().requestCreateCards(deckId = 1L, request = chunkRequest)
                }

            request.headers[API_VERSION_HEADER] shouldBe "1.0"
            request.headers[IDEMPOTENCY_KEY_HEADER] shouldBe null
        }

        test("같은 묶음 요청을 다시 보내면 본문 바이트가 같다") {
            suspend fun send(): ByteArray =
                captureRequest(responseData = CREATED_ITEMS) { ktorfit ->
                    ktorfit.createCardApi().requestCreateCardChunk(
                        deckId = 1L,
                        idempotencyKey = IDEMPOTENCY_KEY,
                        request = chunkRequest,
                    )
                }.body.toByteArray()

            send().contentEquals(send()) shouldBe true
        }
    }

    companion object {
        private const val API_VERSION_HEADER = "API-Version"
        private const val IDEMPOTENCY_KEY_HEADER = "Idempotency-Key"
        private const val IDEMPOTENCY_KEY = "4f8f2c64-5f0e-4b55-9d2a-0a8c3e9b7a11"
        private const val CREATED_ITEMS = """{"items":[{"cardId":1,"fields":{"front":"사과","back":"apple"}}]}"""

        private val chunkRequest =
            CreateCardsRequest(
                items =
                    listOf(
                        CreateCardItemDto(
                            fields = mapOf("front" to "사과", "back" to "apple\n\"따옴표\""),
                            cardType = CreateCardItemDto.CardTypeDto.BASIC,
                        ),
                    ),
            )

        private val jsonParser =
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            }

        private suspend fun captureRequest(
            responseData: String,
            request: suspend (Ktorfit) -> Unit,
        ): HttpRequestData {
            lateinit var capturedRequest: HttpRequestData
            val engine =
                MockEngine { requestData ->
                    capturedRequest = requestData
                    respond(
                        content =
                            """
                            {
                              "success": true,
                              "data": $responseData,
                              "error": null
                            }
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            val client =
                HttpClient(engine) {
                    // 앱 HttpClientFactory와 같이 JSON 본문 형식을 기본으로 둬야 요청 본문이 직렬화된다.
                    install(DefaultRequest) {
                        contentType(ContentType.Application.Json)
                    }
                    install(CaroBaseResponseConverter) {
                        json = jsonParser
                    }
                    install(ContentNegotiation) {
                        json(jsonParser)
                    }
                }
            val ktorfit =
                Ktorfit
                    .Builder()
                    .baseUrl("https://caro.test/")
                    .httpClient(client)
                    .build()

            request(ktorfit)
            client.close()

            return capturedRequest
        }
    }
}
