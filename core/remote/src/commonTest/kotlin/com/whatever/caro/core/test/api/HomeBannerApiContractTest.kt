package com.whatever.caro.core.test.api

import com.whatever.caro.core.remote.api.createHomeBannerApi
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
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class HomeBannerApiContractTest : FunSpec() {
    init {
        test("배너 조회에 API 버전과 플랫폼 및 앱 버전을 보내고 응답을 읽는다") {
            val parser = Json { ignoreUnknownKeys = true }
            val client =
                HttpClient(
                    MockEngine { request ->
                        request.method shouldBe HttpMethod.Get
                        request.url.encodedPath shouldBe "/home/banners"
                        request.url.parameters["platform"] shouldBe "ANDROID"
                        request.url.parameters["appVersion"] shouldBe "1.0.2"
                        request.headers["API-Version"] shouldBe "1.0"
                        respond(
                            """{"success":true,"data":{"items":[],"serverTime":"2026-11-11T00:00:00Z","configRevision":"config-1"},"error":null}""",
                            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                        )
                    },
                ) {
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
                        .createHomeBannerApi()
                val result = api.list("ANDROID", "1.0.2")
                result.items shouldBe emptyList()
                result.configRevision shouldBe "config-1"
            } finally {
                client.close()
            }
        }
    }
}
