package com.whatever.caro.core.test.plugin

import com.whatever.caro.core.model.exception.CaroInvalidResponseException
import com.whatever.caro.core.model.exception.CaroServerException
import com.whatever.caro.core.model.exception.NetworkException
import com.whatever.caro.core.remote.network.plugins.configureCaroExceptionMapping
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpCallValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class ConfigureCaroExceptionMappingTest : FunSpec() {
    init {
        test("InternalServerError 발생시 Caro error body를 CaroServerException으로 변환한다") {
            val errorStatus = HttpStatusCode.InternalServerError
            val client =
                createClient(
                    status = errorStatus,
                    responseBody =
                        """
                        {
                          "success": false,
                          "data": null,
                          "error": {
                            "code": "SERVER-500",
                            "message": "server exploded",
                            "traceId": null,
                            "fieldErrors": null
                          }
                        }
                        """.trimIndent(),
                )

            val exception =
                shouldThrow<CaroServerException> {
                    client.get("https://caro.test/sample").body<String>()
                }

            exception.code shouldBe "SERVER-500"
            exception.message shouldBe "server exploded"
        }

        test("에러 응답 body 파싱에 실패하면 CaroInvalidResponseException을 만든다") {
            val errorStatus = HttpStatusCode.BadGateway
            val client =
                createClient(
                    status = errorStatus,
                    responseBody = "not-json",
                    contentType = ContentType.Text.Plain,
                )

            val exception =
                shouldThrow<CaroInvalidResponseException> {
                    client.get("https://caro.test/sample").body<Unit>()
                }

            exception.message shouldBe "Invalid Response Error"
        }

        test("응답이 없는 네트워크 오류는 NetworkException.Connection으로 변환한다") {
            val client =
                HttpClient(
                    MockEngine {
                        throw io.ktor.util.network
                            .UnresolvedAddressException()
                    },
                ) {
                    expectSuccess = true

                    install(ContentNegotiation) {
                        json(jsonParser)
                    }

                    install(HttpCallValidator) {
                        configureCaroExceptionMapping(jsonParser)
                    }
                }

            val exception =
                shouldThrow<NetworkException> {
                    client.get("https://caro.test/sample").body<Unit>()
                }

            exception.shouldBeInstanceOf<NetworkException.Connection>()
            exception.message shouldBe "Network Error"
        }

        test("정상 응답이면 NetworkException을 던지지 않는다") {
            val client =
                createClient(
                    status = HttpStatusCode.OK,
                    responseBody = "ok",
                    contentType = ContentType.Text.Plain,
                )

            shouldNotThrowAny {
                client.get("https://caro.test/sample").body<String>() shouldBe "ok"
            }
        }
    }

    companion object {
        private val jsonParser =
            Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            }

        private fun createClient(
            status: HttpStatusCode,
            responseBody: String,
            contentType: ContentType = ContentType.Application.Json,
        ): HttpClient =
            HttpClient(
                MockEngine {
                    respond(
                        content = responseBody,
                        status = status,
                        headers = headersOf(HttpHeaders.ContentType, contentType.toString()),
                    )
                },
            ) {
                expectSuccess = true

                install(ContentNegotiation) {
                    json(jsonParser)
                }

                install(HttpCallValidator) {
                    configureCaroExceptionMapping(jsonParser)
                }
            }
    }
}
