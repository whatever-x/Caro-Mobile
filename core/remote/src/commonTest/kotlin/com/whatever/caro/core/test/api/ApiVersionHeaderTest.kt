package com.whatever.caro.core.test.api

import com.whatever.caro.core.remote.api.createAuthApi
import com.whatever.caro.core.remote.api.createDeckApi
import com.whatever.caro.core.remote.network.plugins.CaroBaseResponseConverter
import de.jensklingenberg.ktorfit.Ktorfit
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
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
    }

    companion object {
        private const val API_VERSION_HEADER = "API-Version"

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
