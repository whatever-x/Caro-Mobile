package com.whatever.caro.core.test.api

import com.whatever.caro.core.remote.api.createNotificationApi
import com.whatever.caro.core.remote.auth.AuthTokenProvider
import com.whatever.caro.core.remote.dto.notification.request.RegisterDeviceTokenRequest
import com.whatever.caro.core.remote.network.HttpClientFactory
import com.whatever.caro.core.remote.network.device.DeviceIdProvider
import de.jensklingenberg.ktorfit.Ktorfit
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json

class NotificationAuthFailureTest : FunSpec() {
    init {
        test("로그아웃 토큰 삭제는 만료된 인증을 갱신한 뒤 재시도한다") {
            var access = "expired"
            var refreshCalls = 0
            var deleteCalls = 0
            val provider =
                object : AuthTokenProvider {
                    override suspend fun getAccessToken(): String = access

                    override suspend fun getRefreshToken(): String = "refresh"

                    override suspend fun refresh(): String {
                        refreshCalls++
                        access = "renewed"
                        return access
                    }

                    override suspend fun clearTokens() = error("must not clear")
                }
            val client =
                HttpClientFactory.createCaroClient(
                    engine =
                        MockEngine { request ->
                            deleteCalls++
                            if (deleteCalls == 1) {
                                request.headers["Authorization"] shouldBe "Bearer expired"
                                respond("", HttpStatusCode.Unauthorized)
                            } else {
                                request.headers["Authorization"] shouldBe "Bearer renewed"
                                respond("", HttpStatusCode.NoContent)
                            }
                        },
                    jsonParser = Json,
                    deviceIdProvider =
                        object : DeviceIdProvider {
                            override fun get(): String = "device"
                        },
                    authTokenProvider = provider,
                )
            try {
                Ktorfit
                    .Builder()
                    .baseUrl("https://caro.test/")
                    .httpClient(client)
                    .build()
                    .createNotificationApi()
                    .requestUnregisterToken("token")
                refreshCalls shouldBe 1
                deleteCalls shouldBe 2
            } finally {
                client.close()
            }
        }

        test("등록 API의 401은 인증 갱신이나 세션 삭제를 유발하지 않는다") {
            var refreshCalls = 0
            var clearCalls = 0
            val provider =
                object : AuthTokenProvider {
                    override suspend fun getAccessToken(): String = "access"

                    override suspend fun getRefreshToken(): String = "refresh"

                    override suspend fun refresh(): String {
                        refreshCalls++
                        throw IllegalStateException("refresh failed")
                    }

                    override suspend fun clearTokens() {
                        clearCalls++
                    }
                }
            val client =
                HttpClientFactory.createCaroClient(
                    engine =
                        MockEngine { request ->
                            request.headers["Authorization"] shouldBe "Bearer access"
                            respond("", HttpStatusCode.Unauthorized)
                        },
                    jsonParser = Json,
                    deviceIdProvider =
                        object : DeviceIdProvider {
                            override fun get(): String = "device"
                        },
                    authTokenProvider = provider,
                )
            try {
                val api =
                    Ktorfit
                        .Builder()
                        .baseUrl("https://caro.test/")
                        .httpClient(client)
                        .build()
                        .createNotificationApi()
                shouldThrowAny {
                    api.requestRegisterToken(RegisterDeviceTokenRequest("token", RegisterDeviceTokenRequest.PlatformDto.ANDROID))
                }
                refreshCalls shouldBe 0
                clearCalls shouldBe 0
            } finally {
                client.close()
            }
        }
    }
}
