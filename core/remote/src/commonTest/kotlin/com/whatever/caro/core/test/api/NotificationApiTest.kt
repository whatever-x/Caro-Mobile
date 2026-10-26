package com.whatever.caro.core.test.api

import com.whatever.caro.core.remote.api.createNotificationApi
import com.whatever.caro.core.remote.dto.notification.request.RegisterDeviceTokenRequest
import com.whatever.caro.core.remote.network.plugins.CaroBaseResponseConverter
import de.jensklingenberg.ktorfit.Ktorfit
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class NotificationApiTest : FunSpec() {
    init {
        test("등록 요청은 플랫폼과 토큰을 PUT으로 보내고 빈 성공 응답을 처리한다") {
            val client =
                HttpClient(
                    MockEngine { request ->
                        request.method shouldBe HttpMethod.Put
                        request.url.encodedPath shouldBe "/notifications/devices"
                        request.headers["API-Version"] shouldBe "1.0"
                        val body =
                            Json
                                .parseToJsonElement(
                                    (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString(),
                                ).jsonObject
                        body["token"]?.jsonPrimitive?.content shouldBe "fcm-token"
                        body["platform"]?.jsonPrimitive?.content shouldBe "ANDROID"
                        respond("", HttpStatusCode.OK)
                    },
                ) {
                    defaultRequest { contentType(ContentType.Application.Json) }
                    install(CaroBaseResponseConverter) { json = Json }
                    install(ContentNegotiation) { json() }
                }
            try {
                Ktorfit
                    .Builder()
                    .baseUrl("https://caro.test/")
                    .httpClient(client)
                    .build()
                    .createNotificationApi()
                    .requestRegisterToken(
                        RegisterDeviceTokenRequest("fcm-token", RegisterDeviceTokenRequest.PlatformDto.ANDROID),
                    )
            } finally {
                client.close()
            }
        }

        test("삭제 요청은 토큰을 쿼리로 보내고 204 응답을 처리한다") {
            val client =
                HttpClient(
                    MockEngine { request ->
                        request.method shouldBe HttpMethod.Delete
                        request.url.encodedPath shouldBe "/notifications/devices"
                        request.url.parameters["token"] shouldBe "fcm-token"
                        request.headers["API-Version"] shouldBe "1.0"
                        respond("", HttpStatusCode.NoContent)
                    },
                )
            try {
                Ktorfit
                    .Builder()
                    .baseUrl("https://caro.test/")
                    .httpClient(client)
                    .build()
                    .createNotificationApi()
                    .requestUnregisterToken("fcm-token")
            } finally {
                client.close()
            }
        }

        test("테스트 푸시 요청은 응답 래퍼에서 결과를 꺼낸다") {
            val client =
                HttpClient(
                    MockEngine { request ->
                        request.method shouldBe HttpMethod.Post
                        request.url.encodedPath shouldBe "/notifications/test"
                        respond(
                            """{"success":true,"data":{"successCount":1,"failureCount":0,"removedTokenCount":0},"error":null}""",
                            HttpStatusCode.OK,
                            headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                        )
                    },
                ) {
                    install(CaroBaseResponseConverter) { json = Json }
                }
            try {
                val result =
                    Ktorfit
                        .Builder()
                        .baseUrl("https://caro.test/")
                        .httpClient(client)
                        .build()
                        .createNotificationApi()
                        .requestSendTestPush()
                result.successCount shouldBe 1
                result.failureCount shouldBe 0
                result.removedTokenCount shouldBe 0
            } finally {
                client.close()
            }
        }
    }
}
