package com.whatever.caro.core.test.api

import com.whatever.caro.core.remote.api.createLibraryApi
import com.whatever.caro.core.remote.network.plugins.CaroBaseResponseConverter
import de.jensklingenberg.ktorfit.Ktorfit
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class LibraryApiContractTest :
    FunSpec({
        test("조회와 body 없는 복사 요청은 1.0과 request key를 전송하고 서버 응답 필드를 읽는다") {
            val key = "12345678-1234-1234-1234-123456789abc"
            var calls = 0
            val parser =
                Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                }
            val engine =
                MockEngine { request ->
                    request.headers["API-Version"] shouldBe "1.0"
                    val data =
                        when (request.url.encodedPath) {
                            "/library/decks" -> {
                                request.method shouldBe HttpMethod.Get
                                """[{"libraryDeckId":3,"name":"단어","description":"뜻","cardCount":1}]"""
                            }

                            "/library/decks/3" -> {
                                """{"libraryDeckId":3,"name":"단어","description":"뜻","cardCount":1,"cards":[{"libraryCardId":4,"front":"apply","back":"지원하다","position":0}]}"""
                            }

                            "/library/decks/3/copies" -> {
                                request.method shouldBe HttpMethod.Post
                                request.headers["Idempotency-Key"] shouldBe key
                                request.body.contentLength shouldBe 0L
                                """{"deckId":8,"name":"단어 (2)","description":"뜻","cardCount":1}"""
                            }

                            else -> {
                                error("Unexpected library path")
                            }
                        }
                    calls++
                    respond(
                        """{"success":true,"data":$data,"error":null}""",
                        if (request.method == HttpMethod.Post) HttpStatusCode.Created else HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            val client =
                HttpClient(engine) {
                    install(CaroBaseResponseConverter) { json = parser }
                    install(ContentNegotiation) { json(parser) }
                }
            try {
                val api =
                    Ktorfit
                        .Builder()
                        .baseUrl("https://caro.test/")
                        .httpClient(client)
                        .build()
                        .createLibraryApi()
                api.list().single().libraryDeckId shouldBe 3L
                api
                    .detail(3)
                    .cards
                    .single()
                    .back shouldBe "지원하다"
                api.copy(3, key).name shouldBe "단어 (2)"
                calls shouldBe 3
            } finally {
                client.close()
            }
        }
    })
