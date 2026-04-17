package com.whatever.caro.core.test.plugin

import com.whatever.caro.core.remote.model.NetworkException
import com.whatever.caro.core.remote.network.plugins.CaroBaseResponseUnwrap
import com.whatever.caro.core.remote.network.plugins.installCaroResponseHandler
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
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

class CaroResponseHandlerTest :
    FunSpec(
        {
            test("에러 응답의 Caro error body를 NetworkException으로 변환한다") {
                val client =
                    createClient(
                        status = HttpStatusCode.InternalServerError,
                        responseBody =
                            """
                            {
                              "success": false,
                              "data": { },
                              "error": {
                                "code": "SERVER-500",
                                "message": "server exploded",
                                "debugMessage": "stacktrace",
                                "description": "retry later"
                              }
                            }
                            """.trimIndent(),
                    )

                val exception =
                    shouldThrow<NetworkException> {
                        client.get("https://caro.test/sample").body<String>()
                    }

                exception.code shouldBe "SERVER-500"
                exception.message shouldBe "server exploded"
                exception.debugMessage shouldBe "stacktrace"
                exception.description shouldBe "retry later"
            }

            test("unwrap 플러그인이 함께 설치되어도 에러 상세를 유지한다") {
                val client =
                    createClient(
                        status = HttpStatusCode.InternalServerError,
                        responseBody =
                            """
                            {
                              "success": false,
                              "data": null,
                              "error": {
                                "code": "SERVER-500",
                                "message": "server exploded",
                                "debugMessage": "stacktrace",
                                "description": "retry later"
                              }
                            }
                            """.trimIndent(),
                    )

                val exception =
                    shouldThrow<NetworkException> {
                        client.get("https://caro.test/sample").body<String>()
                    }

                exception.code shouldBe "SERVER-500"
                exception.message shouldBe "server exploded"
                exception.debugMessage shouldBe "stacktrace"
                exception.description shouldBe "retry later"
            }

            test("에러 응답 body 파싱에 실패하면 status code와 기본 메시지로 NetworkException을 만든다") {
                val client =
                    createClient(
                        status = HttpStatusCode.BadGateway,
                        responseBody = "not-json",
                        contentType = ContentType.Text.Plain,
                    )

                val exception =
                    shouldThrow<NetworkException> {
                        client.get("https://caro.test/sample").body<String>()
                    }

                exception.code shouldBe HttpStatusCode.BadGateway.value.toString()
                exception.message shouldBe "Server error"
                exception.description shouldBe null
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
        },
    ) {
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
                    installCaroResponseHandler(jsonParser)
                }

                install(CaroBaseResponseUnwrap) {
                    this.json = jsonParser
                }
            }
    }
}
