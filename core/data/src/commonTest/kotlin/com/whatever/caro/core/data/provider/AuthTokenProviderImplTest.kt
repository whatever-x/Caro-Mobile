package com.whatever.caro.core.data.provider

import com.whatever.caro.core.datastore.datasource.AuthTokens
import com.whatever.caro.core.datastore.datasource.LocalAuthDataSource
import com.whatever.caro.core.model.auth.AuthSessionEventBus
import com.whatever.caro.core.model.exception.CaroAuthException
import com.whatever.caro.core.remote.datasource.auth.NonAuthDataSource
import com.whatever.caro.core.remote.dto.auth.response.TokenResponse
import dev.mokkery.answering.calls
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class AuthTokenProviderImplTest : FunSpec() {
    init {
        test("가입 완료 여부는 토큰과 같은 저장소 스냅샷에서 읽는다") {
            val local =
                mock<LocalAuthDataSource> {
                    everySuspend { fetchTokens() } calls { AuthTokens("pending", "refresh", 1, false) }
                }
            val provider =
                AuthTokenProviderImpl(
                    local,
                    mock(),
                    mock(),
                    com.whatever.caro.core.data.repository.ads
                        .AdEligibilityGate(),
                )
            provider.getSessionSnapshot().isRegistrationComplete shouldBe false
        }

        test("이전 계정 요청은 새 계정 토큰을 갱신하지 않는다") {
            runTest {
                val local =
                    mock<LocalAuthDataSource> {
                        everySuspend { fetchTokens() } calls { AuthTokens("new", "refresh", 2) }
                    }
                val provider =
                    AuthTokenProviderImpl(
                        local,
                        mock(),
                        mock(),
                        com.whatever.caro.core.data.repository.ads
                            .AdEligibilityGate(),
                    )
                shouldThrow<CaroAuthException.TokenExpired> { provider.refresh(1) }
            }
        }

        listOf(false, true).forEach { fail ->
            test("새 로그인 후 이전 토큰 갱신 ${if (fail) "실패" else "성공"}은 새 세션을 바꾸지 않는다") {
                runTest {
                    var current = AuthTokens("old", "oldRefresh", 1)
                    var published = false
                    val response = CompletableDeferred<TokenResponse>()
                    val local =
                        mock<LocalAuthDataSource> {
                            everySuspend { fetchTokens() } calls { current }
                            everySuspend { replaceTokensIfCurrent(any(), any(), any()) } calls {
                                val expected = it.arg<AuthTokens>(0)
                                if (current == expected) {
                                    current = AuthTokens(it.arg(1), it.arg(2), current.generation)
                                    true
                                } else {
                                    false
                                }
                            }
                            everySuspend { clearIfCurrent(any()) } calls {
                                if (current == it.arg<AuthTokens>(0)) {
                                    current = AuthTokens(null, null, current.generation + 1)
                                    true
                                } else {
                                    false
                                }
                            }
                        }
                    val remote =
                        mock<NonAuthDataSource> {
                            everySuspend { refreshToken(any()) } calls { response.await() }
                        }
                    val events =
                        mock<AuthSessionEventBus> {
                            everySuspend { publish(any()) } calls { published = true }
                        }
                    val provider =
                        AuthTokenProviderImpl(
                            local,
                            remote,
                            events,
                            com.whatever.caro.core.data.repository.ads
                                .AdEligibilityGate(),
                        )
                    val refresh = async { runCatching { provider.refresh(1) } }
                    runCurrent()
                    current = AuthTokens("new", "newRefresh", 2)
                    if (fail) {
                        response.completeExceptionally(IllegalStateException("offline"))
                    } else {
                        response.complete(TokenResponse("stale", "staleRefresh"))
                    }
                    (refresh.await().exceptionOrNull() is CaroAuthException.TokenExpired) shouldBe true
                    current shouldBe AuthTokens("new", "newRefresh", 2)
                    published shouldBe false
                }
            }
        }
    }
}
