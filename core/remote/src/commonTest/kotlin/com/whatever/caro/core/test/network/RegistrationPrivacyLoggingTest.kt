package com.whatever.caro.core.test.network

import com.whatever.caro.core.remote.network.HttpClientFactory
import com.whatever.caro.core.remote.network.device.DeviceIdProvider
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json

class RegistrationPrivacyLoggingTest :
    FunSpec({
        test("가입 요청의 생년월일은 디버그 로그에 남지 않는다") {
            val logs = mutableListOf<String>()
            val client =
                HttpClientFactory.createCaroClient(
                    engine = MockEngine { respond("{}", HttpStatusCode.OK) },
                    jsonParser = Json,
                    deviceIdProvider =
                        object : DeviceIdProvider {
                            override fun get(): String = "test-device"
                        },
                    configure = {
                        install(Logging) {
                            level = LogLevel.ALL
                            logger =
                                object : Logger {
                                    override fun log(message: String) {
                                        logs += message
                                    }
                                }
                        }
                    },
                )
            try {
                client.post("https://caro.test/auth/complete-registration") { setBody("""{"birthDate":"2000-01-01"}""") }
                logs.any { it.contains("2000-01-01") || it.contains("complete-registration") } shouldBe false
                client.post("https://caro.test/other") { setBody("visible-control") }
                logs.any { it.contains("visible-control") } shouldBe true
            } finally {
                client.close()
            }
        }
    })
