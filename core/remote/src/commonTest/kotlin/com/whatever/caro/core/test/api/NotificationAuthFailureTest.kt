package com.whatever.caro.core.test.api

import com.whatever.caro.core.remote.api.createAuthApi
import com.whatever.caro.core.remote.api.createNotificationApi
import com.whatever.caro.core.remote.auth.AuthTokenProvider
import com.whatever.caro.core.remote.auth.AuthTokenSnapshot
import com.whatever.caro.core.remote.dto.auth.request.CompleteRegistrationV11Request
import com.whatever.caro.core.remote.dto.notification.request.RegisterDeviceTokenRequest
import com.whatever.caro.core.remote.network.HttpClientFactory
import com.whatever.caro.core.remote.network.device.DeviceIdProvider
import de.jensklingenberg.ktorfit.Ktorfit
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json

class NotificationAuthFailureTest : FunSpec() {
    init {
        test("로그아웃 토큰 삭제는 만료된 인증을 갱신한 뒤 재시도한다") {
            var access = "expired"
            var refreshCalls = 0
            var deleteCalls = 0
            val provider =
                object : AuthTokenProvider {
                    override suspend fun getSessionSnapshot() = AuthTokenSnapshot(access, 1)

                    override suspend fun getAccessToken(): String = access

                    override suspend fun getRefreshToken(): String = "refresh"

                    override suspend fun refresh(expectedGeneration: Long): String {
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

        test("계정이 바뀌면 이전 요청을 새 계정 토큰으로 재전송하지 않는다") {
            var generation = 1L
            var requests = 0
            var refreshes = 0
            val provider =
                object : AuthTokenProvider {
                    override suspend fun getSessionSnapshot() = AuthTokenSnapshot(if (generation == 1L) "old" else "new", generation)

                    override suspend fun getAccessToken() = if (generation == 1L) "old" else "new"

                    override suspend fun getRefreshToken() = "refresh"

                    override suspend fun refresh(expectedGeneration: Long): String {
                        refreshes++
                        return "refreshed"
                    }

                    override suspend fun clearTokens() = error("must not clear new session")
                }
            val client =
                HttpClientFactory.createCaroClient(
                    engine =
                        MockEngine {
                            requests++
                            generation = 2
                            respond("", HttpStatusCode.Unauthorized)
                        },
                    jsonParser = Json,
                    deviceIdProvider =
                        object : DeviceIdProvider {
                            override fun get() = "device"
                        },
                    authTokenProvider = provider,
                )
            try {
                shouldThrowAny { client.get("https://caro.test/users/me/info") }
                requests shouldBe 1
                refreshes shouldBe 0
            } finally {
                client.close()
            }
        }

        listOf("users/nicknames/caro/availability", "nicknames/random").forEach { path ->
            test("가입 중 $path 요청의 401은 화면을 강제로 종료하지 않는다") {
                var refreshCalls = 0
                val provider =
                    object : AuthTokenProvider {
                        override suspend fun getSessionSnapshot() = AuthTokenSnapshot("pending", 1, false)

                        override suspend fun getAccessToken() = "pending"

                        override suspend fun getRefreshToken() = "refresh"

                        override suspend fun refresh(expectedGeneration: Long): String {
                            refreshCalls++
                            error("must not expire pending signup")
                        }

                        override suspend fun clearTokens() = error("must not clear")
                    }
                val client =
                    HttpClientFactory.createCaroClient(
                        engine = MockEngine { respond("", HttpStatusCode.Unauthorized) },
                        jsonParser = Json,
                        deviceIdProvider =
                            object : DeviceIdProvider {
                                override fun get() = "device"
                            },
                        authTokenProvider = provider,
                    )
                try {
                    shouldThrowAny { client.get("https://caro.test/$path") }
                    refreshCalls shouldBe 0
                } finally {
                    client.close()
                }
            }
        }

        listOf("기기 토큰 등록", "가입 완료").forEach { operation ->
            test("$operation API의 401은 인증 갱신이나 세션 삭제를 유발하지 않는다") {
                var refreshCalls = 0
                var clearCalls = 0
                val provider =
                    object : AuthTokenProvider {
                        override suspend fun getSessionSnapshot() = AuthTokenSnapshot("access", 1)

                        override suspend fun getAccessToken(): String = "access"

                        override suspend fun getRefreshToken(): String = "refresh"

                        override suspend fun refresh(expectedGeneration: Long): String {
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
                    shouldThrowAny {
                        if (operation == "가입 완료") {
                            api.createAuthApi().requestCompleteRegistration(
                                CompleteRegistrationV11Request(
                                    birthDate = LocalDate(2000, 1, 1),
                                    isTermsAgreed = true,
                                    nickname = "카로친구",
                                ),
                            )
                        } else {
                            api.createNotificationApi().requestRegisterToken(
                                RegisterDeviceTokenRequest("token", RegisterDeviceTokenRequest.PlatformDto.ANDROID),
                            )
                        }
                    }
                    refreshCalls shouldBe 0
                    clearCalls shouldBe 0
                } finally {
                    client.close()
                }
            }
        }
    }
}
