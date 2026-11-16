package com.whatever.caro.core.data.repository.auth

import com.whatever.caro.core.data.repository.fcm.FcmTokenRepository
import com.whatever.caro.core.datastore.datasource.AuthTokens
import com.whatever.caro.core.datastore.datasource.LocalAuthDataSource
import com.whatever.caro.core.model.auth.AuthSession
import com.whatever.caro.core.model.auth.SocialLoginType
import com.whatever.caro.core.model.exception.CaroAuthException
import com.whatever.caro.core.remote.datasource.auth.AuthDataSource
import com.whatever.caro.core.remote.datasource.auth.NonAuthDataSource
import com.whatever.caro.core.remote.dto.auth.request.CompleteRegistrationV11Request
import com.whatever.caro.core.remote.dto.auth.request.RefreshTokenRequest
import com.whatever.caro.core.remote.dto.auth.request.SocialLoginRequest
import com.whatever.caro.core.remote.dto.auth.response.SocialLoginResponse
import com.whatever.caro.core.remote.dto.auth.response.TokenResponse
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

class AuthRepositoryImplTest : FunSpec() {
    init {
        fun repositoryWith(
            remoteAuthDataSource: AuthDataSource = mock(),
            remoteNonAuthDataSource: NonAuthDataSource = mock(),
            localAuthDataSource: LocalAuthDataSource = mock(),
            fcmTokenRepository: FcmTokenRepository =
                mock {
                    everySuspend { unregisterToken() } returns Unit
                    everySuspend { resumeRegistration() } returns Unit
                },
        ) = AuthRepositoryImpl(
            remoteAuthDataSource = remoteAuthDataSource,
            remoteNonAuthDataSource = remoteNonAuthDataSource,
            localAuthDataSource = localAuthDataSource,
            fcmTokenRepository = fcmTokenRepository,
            adEligibilityGate =
                com.whatever.caro.core.data.repository.ads
                    .AdEligibilityGate(),
        )

        test("로그아웃은 FCM 삭제를 인증 로그아웃과 로컬 세션 삭제보다 먼저 수행한다") {
            runTest {
                val operations = mutableListOf<String>()
                val fcm =
                    object : FcmTokenRepository {
                        override suspend fun syncTokens(tokens: kotlinx.coroutines.flow.Flow<String>) = Unit

                        override fun observeNotificationEnabled(): kotlinx.coroutines.flow.Flow<Boolean> = error("unused")

                        override fun updateNotificationPermission(granted: Boolean) = Unit

                        override suspend fun setNotificationEnabled(enabled: Boolean) = error("unused")

                        override suspend fun resumeRegistration() = Unit

                        override suspend fun unregisterToken() {
                            operations.add("fcm")
                        }
                    }
                val remote =
                    object : AuthDataSource {
                        override suspend fun logout() {
                            operations.add("logout")
                        }

                        override suspend fun withdraw() = error("unused")

                        override suspend fun completeRegistration(request: CompleteRegistrationV11Request): TokenResponse = error("unused")
                    }
                val local = mock<LocalAuthDataSource> { everySuspend { clear() } returns Unit }
                repositoryWith(remoteAuthDataSource = remote, localAuthDataSource = local, fcmTokenRepository = fcm).logout()
                operations shouldBe listOf("fcm", "logout")
                verifySuspend { local.clear() }
            }
        }

        test("FCM 삭제 실패로 로그아웃이나 로컬 세션 삭제를 막지 않는다") {
            runTest {
                val fcm = mock<FcmTokenRepository> { everySuspend { unregisterToken() } throws IllegalStateException("delete failed") }
                val remote = mock<AuthDataSource> { everySuspend { logout() } returns Unit }
                val local = mock<LocalAuthDataSource> { everySuspend { clear() } returns Unit }
                shouldNotThrowAny {
                    repositoryWith(remoteAuthDataSource = remote, localAuthDataSource = local, fcmTokenRepository = fcm).logout()
                }
                verifySuspend {
                    fcm.unregisterToken()
                    remote.logout()
                    local.clear()
                }
            }
        }

        test("loginWithSocial은 발급된 토큰을 저장하고 가입 완료 여부를 반환한다") {
            runTest {
                val remoteNonAuthDataSource =
                    mock<NonAuthDataSource> {
                        everySuspend { socialLogin(any()) } returns
                            SocialLoginResponse(
                                accessToken = "access",
                                refreshToken = "refresh",
                                isRegistrationComplete = true,
                            )
                    }
                val localAuthDataSource =
                    mock<LocalAuthDataSource> {
                        everySuspend { saveSession(any(), any(), any()) } returns Unit
                    }
                val repository =
                    repositoryWith(
                        remoteNonAuthDataSource = remoteNonAuthDataSource,
                        localAuthDataSource = localAuthDataSource,
                    )

                val result =
                    repository.loginWithSocial(
                        provider = SocialLoginType.GOOGLE,
                        idToken = "idToken",
                    )

                result shouldBe true
                verifySuspend {
                    remoteNonAuthDataSource.socialLogin(
                        SocialLoginRequest(provider = SocialLoginType.GOOGLE, idToken = "idToken"),
                    )
                    localAuthDataSource.saveSession("access", "refresh", true)
                }
            }
        }

        test("loginWithSocial은 가입이 완료되지 않은 응답이면 토큰 저장 후 false를 반환한다") {
            runTest {
                val remoteNonAuthDataSource =
                    mock<NonAuthDataSource> {
                        everySuspend { socialLogin(any()) } returns
                            SocialLoginResponse(
                                accessToken = "access",
                                refreshToken = "refresh",
                                isRegistrationComplete = false,
                            )
                    }
                val localAuthDataSource =
                    mock<LocalAuthDataSource> {
                        everySuspend { saveSession(any(), any(), any()) } returns Unit
                    }
                val repository =
                    repositoryWith(
                        remoteNonAuthDataSource = remoteNonAuthDataSource,
                        localAuthDataSource = localAuthDataSource,
                    )

                val result =
                    repository.loginWithSocial(
                        provider = SocialLoginType.GOOGLE,
                        idToken = "idToken",
                    )

                result shouldBe false
                verifySuspend {
                    localAuthDataSource.saveSession("access", "refresh", false)
                }
            }
        }

        test("logout은 원격 로그아웃 후 로컬 토큰을 비운다") {
            runTest {
                val remoteAuthDataSource =
                    mock<AuthDataSource> { everySuspend { logout() } returns Unit }
                val localAuthDataSource =
                    mock<LocalAuthDataSource> { everySuspend { clear() } returns Unit }
                val repository =
                    repositoryWith(
                        remoteAuthDataSource = remoteAuthDataSource,
                        localAuthDataSource = localAuthDataSource,
                    )

                repository.logout()

                verifySuspend {
                    remoteAuthDataSource.logout()
                    localAuthDataSource.clear()
                }
            }
        }

        test("withdraw는 원격 회원탈퇴 후 로컬 토큰을 비운다") {
            runTest {
                val remoteAuthDataSource =
                    mock<AuthDataSource> { everySuspend { withdraw() } returns Unit }
                val localAuthDataSource =
                    mock<LocalAuthDataSource> { everySuspend { clear() } returns Unit }
                val repository =
                    repositoryWith(
                        remoteAuthDataSource = remoteAuthDataSource,
                        localAuthDataSource = localAuthDataSource,
                    )

                repository.withdraw()

                verifySuspend {
                    remoteAuthDataSource.withdraw()
                    localAuthDataSource.clear()
                }
            }
        }

        test("withdraw는 로컬 토큰 삭제가 실패해도 예외를 전파하지 않는다") {
            runTest {
                val remoteAuthDataSource =
                    mock<AuthDataSource> { everySuspend { withdraw() } returns Unit }
                val localAuthDataSource =
                    mock<LocalAuthDataSource> {
                        everySuspend { clear() } throws RuntimeException("clear failed")
                    }
                val repository =
                    repositoryWith(
                        remoteAuthDataSource = remoteAuthDataSource,
                        localAuthDataSource = localAuthDataSource,
                    )

                shouldNotThrowAny { repository.withdraw() }

                verifySuspend {
                    remoteAuthDataSource.withdraw()
                    localAuthDataSource.clear()
                }
            }
        }

        test("logout은 로컬 토큰 삭제가 실패해도 예외를 전파하지 않는다") {
            runTest {
                val remoteAuthDataSource =
                    mock<AuthDataSource> { everySuspend { logout() } returns Unit }
                val localAuthDataSource =
                    mock<LocalAuthDataSource> {
                        everySuspend { clear() } throws RuntimeException("clear failed")
                    }
                val repository =
                    repositoryWith(
                        remoteAuthDataSource = remoteAuthDataSource,
                        localAuthDataSource = localAuthDataSource,
                    )

                shouldNotThrowAny { repository.logout() }

                verifySuspend {
                    remoteAuthDataSource.logout()
                    localAuthDataSource.clear()
                }
            }
        }

        test("logout은 원격 로그아웃이 실패해도 로컬 토큰을 비우고 예외를 전파하지 않는다") {
            runTest {
                val remoteAuthDataSource =
                    mock<AuthDataSource> {
                        everySuspend { logout() } throws RuntimeException("network error")
                    }
                val localAuthDataSource =
                    mock<LocalAuthDataSource> { everySuspend { clear() } returns Unit }
                val repository =
                    repositoryWith(
                        remoteAuthDataSource = remoteAuthDataSource,
                        localAuthDataSource = localAuthDataSource,
                    )

                shouldNotThrowAny { repository.logout() }

                verifySuspend {
                    remoteAuthDataSource.logout()
                    localAuthDataSource.clear()
                }
            }
        }

        test("withdraw는 원격 회원탈퇴 실패 시 로컬 토큰을 유지한다") {
            runTest {
                val failure = RuntimeException("withdraw failed")
                val remoteAuthDataSource =
                    mock<AuthDataSource> { everySuspend { withdraw() } throws failure }
                val localAuthDataSource = mock<LocalAuthDataSource>()
                val repository =
                    repositoryWith(
                        remoteAuthDataSource = remoteAuthDataSource,
                        localAuthDataSource = localAuthDataSource,
                    )

                shouldThrow<RuntimeException> { repository.withdraw() } shouldBe failure
                verifySuspend(exactly(0)) { localAuthDataSource.clear() }
            }
        }

        test("completeRegistration은 발급된 토큰을 저장하고 AuthSession을 반환한다") {
            runTest {
                val remoteAuthDataSource =
                    mock<AuthDataSource> {
                        everySuspend { completeRegistration(any()) } returns
                            TokenResponse(accessToken = "access", refreshToken = "refresh")
                    }
                val localAuthDataSource =
                    mock<LocalAuthDataSource> {
                        everySuspend { saveSession(any(), any(), any()) } returns Unit
                    }
                val repository =
                    repositoryWith(
                        remoteAuthDataSource = remoteAuthDataSource,
                        localAuthDataSource = localAuthDataSource,
                    )

                val result =
                    repository.completeRegistration(
                        nickname = "caro",
                        termsAgreed = true,
                        birthDate = "2000-01-01",
                    )

                result shouldBe AuthSession(accessToken = "access", refreshToken = "refresh")
                verifySuspend {
                    remoteAuthDataSource.completeRegistration(
                        CompleteRegistrationV11Request(
                            nickname = "caro",
                            isTermsAgreed = true,
                            birthDate = LocalDate(2000, 1, 1),
                        ),
                    )
                    localAuthDataSource.saveSession("access", "refresh", true)
                }
            }
        }

        test("isRegistrationComplete는 저장된 가입 완료 여부를 반환한다") {
            runTest {
                val localAuthDataSource =
                    mock<LocalAuthDataSource> {
                        everySuspend { fetchRegistrationComplete() } returns false
                    }

                repositoryWith(localAuthDataSource = localAuthDataSource).isRegistrationComplete() shouldBe false
            }
        }

        test("isRegistrationComplete는 저장된 값이 없으면 true를 반환한다") {
            runTest {
                val localAuthDataSource =
                    mock<LocalAuthDataSource> {
                        everySuspend { fetchRegistrationComplete() } returns null
                    }

                repositoryWith(localAuthDataSource = localAuthDataSource).isRegistrationComplete() shouldBe true
            }
        }

        test("refreshToken은 저장된 토큰으로 갱신 후 새 토큰을 저장한다") {
            runTest {
                val localAuthDataSource =
                    mock<LocalAuthDataSource> {
                        everySuspend { fetchTokens() } returns AuthTokens("oldAccess", "oldRefresh")
                        everySuspend { replaceTokensIfCurrent(any(), any(), any()) } returns true
                    }
                val remoteNonAuthDataSource =
                    mock<NonAuthDataSource> {
                        everySuspend { refreshToken(any()) } returns
                            TokenResponse(accessToken = "newAccess", refreshToken = "newRefresh")
                    }
                val repository =
                    repositoryWith(
                        remoteNonAuthDataSource = remoteNonAuthDataSource,
                        localAuthDataSource = localAuthDataSource,
                    )

                repository.refreshToken()

                verifySuspend {
                    remoteNonAuthDataSource.refreshToken(
                        RefreshTokenRequest(accessToken = "oldAccess", refreshToken = "oldRefresh"),
                    )
                    localAuthDataSource.replaceTokensIfCurrent(AuthTokens("oldAccess", "oldRefresh"), "newAccess", "newRefresh")
                }
            }
        }

        test("refreshToken은 저장된 토큰이 비어 있으면 TokenEmpty를 던진다") {
            runTest {
                val localAuthDataSource =
                    mock<LocalAuthDataSource> {
                        everySuspend { fetchTokens() } returns AuthTokens(null, "oldRefresh")
                    }
                val repository = repositoryWith(localAuthDataSource = localAuthDataSource)

                shouldThrow<CaroAuthException.TokenEmpty> {
                    repository.refreshToken()
                }
            }
        }
    }
}
