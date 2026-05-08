package com.whatever.caro.core.test.plugin

import com.whatever.caro.core.model.exception.CaroInvalidResponseException
import com.whatever.caro.core.model.exception.CaroServerException
import com.whatever.caro.core.model.exception.ErrorCode
import com.whatever.caro.core.remote.dto.auth.response.LoginResponse
import com.whatever.caro.core.remote.network.plugins.CaroBaseResponseConverter
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class ApiResponseObjectConverterTest : FunSpec() {
    init {
        test("success=true 이면 data payload 를 요청 타입으로 변환한다") {
            val client =
                createClient(
                    responseBody =
                        """
                        {
                          "success": true,
                          "data": {
                            "accessToken": "access-token",
                            "refreshToken": "refresh-token"
                          },
                          "error": null
                        }
                        """.trimIndent(),
                )

            val body = shouldNotThrowAny { client.get("https://caro.test/auth/login").body<LoginResponse>() }

            body shouldBe LoginResponse(accessToken = "access-token", refreshToken = "refresh-token")
        }

        test("success=true 이고 data=null 일 때 Unit 요청은 성공 처리한다") {
            val client =
                createClient(
                    responseBody =
                        """
                        {
                          "success": true,
                          "data": null,
                          "error": null
                        }
                        """.trimIndent(),
                )

            shouldNotThrowAny {
                client.get("https://caro.test/auth/login").body<Unit>() shouldBe Unit
            }
        }

        test("success=false 이면 200 응답이어도 CaroServerException 을 던진다") {
            val client =
                createClient(
                    responseBody =
                        """
                        {
                          "success": false,
                          "data": null,
                          "error": {
                            "code": "AUTH-401",
                            "message": "사용자 토큰 인증에 실패했습니다.",
                            "debugMessage": "사용자 토큰 인증 실패",
                            "description": "login again"
                          }
                        }
                        """.trimIndent(),
                )

            val exception =
                shouldThrow<CaroServerException> {
                    client.get("https://caro.test/auth/login").body<LoginResponse>()
                }

            exception.code shouldBe "AUTH-401"
            exception.message shouldBe "사용자 토큰 인증에 실패했습니다."
            exception.debugMessage shouldBe "사용자 토큰 인증 실패"
            exception.description shouldBe "login again"
        }

        test("success=false 인데 error 가 없으면 INVALID_RESPONSE인 CaroInvalidResponseException 예외를 던진다") {
            val client =
                createClient(
                    responseBody =
                        """
                        {
                          "success": false,
                          "data": null,
                          "error": null
                        }
                        """.trimIndent(),
                )

            val exception =
                shouldThrow<CaroInvalidResponseException> {
                    client.get("https://caro.test/auth/login").body<LoginResponse>()
                }

            exception.code shouldBe ErrorCode.INVALID_RESPONSE
            exception.message shouldBe "Invalid Response Error"
        }

        test("success=true 인데 data=null 이면 INVALID_RESPONSE인 CaroInvalidResponseException 예외를 던진다") {
            val client =
                createClient(
                    responseBody =
                        """
                        {
                          "success": true,
                          "data": null,
                          "error": null
                        }
                        """.trimIndent(),
                )

            val exception =
                shouldThrow<CaroInvalidResponseException> {
                    client.get("https://caro.test/auth/login").body<LoginResponse>()
                }

            exception.code shouldBe ErrorCode.INVALID_RESPONSE
            exception.message shouldBe "Invalid Response Error"
        }

        test("응답 decode에 실패하면 INVALID_RESPONSE인 CaroInvalidResponseException 예외를 던진다") {
            val client =
                createClient(
                    responseBody =
                        """
                        {
                          "success": true,
                          "data": "invalid-shape",
                          "error": null
                        }
                        """.trimIndent(),
                )

            val exception =
                shouldThrow<CaroInvalidResponseException> {
                    client.get("https://caro.test/auth/login").body<LoginResponse>()
                }

            exception.code shouldBe ErrorCode.INVALID_RESPONSE
            exception.message shouldBe "Invalid Response Error"
        }

        test("JSON 이 아닌 응답이면 converter가 개입하지 않는다") {
            val client =
                createClient(
                    responseBody = "plain-text",
                    contentType = ContentType.Text.Plain,
                )

            shouldNotThrowAny {
                client.get("https://caro.test/auth/login").body<String>() shouldBe "plain-text"
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
            responseBody: String,
            contentType: ContentType = ContentType.Application.Json,
        ): HttpClient =
            HttpClient(
                MockEngine {
                    respond(
                        content = responseBody,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, contentType.toString()),
                    )
                },
            ) {
                install(CaroBaseResponseConverter) {
                    this.json = jsonParser
                }

                install(ContentNegotiation) {
                    json(jsonParser)
                }
            }
    }
}
