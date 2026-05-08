package com.whatever.caro.core.test.module

import com.whatever.caro.core.remote.datasource.auth.RemoteAuthDataSource
import com.whatever.caro.core.remote.device.DeviceIdProvider
import com.whatever.caro.core.remote.di.networkModule
import com.whatever.caro.core.remote.di.qualifier.NetworkClient
import com.whatever.caro.core.remote.dto.auth.request.SocialLoginRequest
import com.whatever.caro.core.remote.dto.auth.request.TokenRefreshRequest
import com.whatever.caro.core.remote.dto.auth.response.LoginResponse
import com.whatever.caro.core.remote.network.plugins.CaroBaseResponseConverter
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.koin.KoinExtension
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpCallValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.plugin
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get

class NetworkModuleTest :
    FunSpec(),
    KoinTest {
    init {
        val testDependenciesModule =
            module {
                single<DeviceIdProvider> {
                    object : DeviceIdProvider {
                        override fun get(): String = "test-device-id"
                    }
                }
                single<RemoteAuthDataSource> {
                    object : RemoteAuthDataSource {
                        override suspend fun login(request: SocialLoginRequest): LoginResponse =
                            LoginResponse(accessToken = "", refreshToken = "")

                        override suspend fun refresh(request: TokenRefreshRequest): LoginResponse =
                            LoginResponse(accessToken = "", refreshToken = "")
                    }
                }
            }

        extensions(KoinExtension(listOf(networkModule, testDependenciesModule)))

        test("AUTH 클라이언트와 NON_AUTH 클라이언트는 서로 다른 인스턴스를 가진다.") {
            val authClient: HttpClient = get(qualifier = named(NetworkClient.Caro.AUTH))
            val nonAuthClient: HttpClient = get(qualifier = named(NetworkClient.Caro.NON_AUTH))

            (authClient === nonAuthClient) shouldBe false
        }

        test("Caro AUTH 클라이언트는 Auth 플러그인을 install 해야한다.") {
            val authClient: HttpClient = get(qualifier = named(NetworkClient.Caro.AUTH))

            shouldNotThrowAny {
                authClient.plugin(Auth)
            }
        }

        test("Caro AUTH 클라이언트는 공통 네트워크 플러그인을 install 해야한다.") {
            val authClient: HttpClient = get(qualifier = named(NetworkClient.Caro.AUTH))

            shouldNotThrowAny { authClient.plugin(ContentNegotiation) }
            shouldNotThrowAny { authClient.plugin(HttpTimeout) }
            shouldNotThrowAny { authClient.plugin(Logging) }
            shouldNotThrowAny { authClient.plugin(HttpCallValidator) }
            shouldNotThrowAny { authClient.plugin(CaroBaseResponseConverter) }
        }

        test("Caro NON_AUTH 클라이언트는 공통 네트워크 플러그인을 install 해야한다.") {
            val nonAuthClient: HttpClient = get(qualifier = named(NetworkClient.Caro.NON_AUTH))

            shouldNotThrowAny { nonAuthClient.plugin(ContentNegotiation) }
            shouldNotThrowAny { nonAuthClient.plugin(HttpTimeout) }
            shouldNotThrowAny { nonAuthClient.plugin(Logging) }
            shouldNotThrowAny { nonAuthClient.plugin(HttpCallValidator) }
            shouldNotThrowAny { nonAuthClient.plugin(CaroBaseResponseConverter) }
        }

        test("networkModule은 공통 Json 설정을 제공한다.") {
            val json: Json = get()

            json.configuration.ignoreUnknownKeys shouldBe true
            json.configuration.prettyPrint shouldBe true
        }
    }
}
