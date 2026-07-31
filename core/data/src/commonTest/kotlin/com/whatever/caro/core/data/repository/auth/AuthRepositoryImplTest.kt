package com.whatever.caro.core.data.repository.auth

import com.whatever.caro.core.datastore.datasource.LocalAuthDataSource
import com.whatever.caro.core.model.auth.AuthSession
import com.whatever.caro.core.model.auth.SocialLoginType
import com.whatever.caro.core.model.exception.CaroAuthException
import com.whatever.caro.core.remote.datasource.auth.AuthDataSource
import com.whatever.caro.core.remote.datasource.auth.NonAuthDataSource
import com.whatever.caro.core.remote.dto.auth.request.CompleteRegistrationRequest
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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest

class AuthRepositoryImplTest : FunSpec() {
    init {
        fun repositoryWith(
            remoteAuthDataSource: AuthDataSource = mock(),
            remoteNonAuthDataSource: NonAuthDataSource = mock(),
            localAuthDataSource: LocalAuthDataSource = mock(),
        ) = AuthRepositoryImpl(
            remoteAuthDataSource = remoteAuthDataSource,
            remoteNonAuthDataSource = remoteNonAuthDataSource,
            localAuthDataSource = localAuthDataSource,
        )

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
                        everySuspend { saveTokens(any(), any()) } returns Unit
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
                    localAuthDataSource.saveTokens(accessToken = "access", refreshToken = "refresh")
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
                        everySuspend { saveTokens(any(), any()) } returns Unit
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
                    localAuthDataSource.saveTokens(accessToken = "access", refreshToken = "refresh")
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
                        everySuspend { saveTokens(any(), any()) } returns Unit
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
                    )

                result shouldBe AuthSession(accessToken = "access", refreshToken = "refresh")
                verifySuspend {
                    remoteAuthDataSource.completeRegistration(
                        CompleteRegistrationRequest(
                            nickname = "caro",
                            isTermsAgreed = true,
                        ),
                    )
                    localAuthDataSource.saveTokens(accessToken = "access", refreshToken = "refresh")
                }
            }
        }

        test("refreshToken은 저장된 토큰으로 갱신 후 새 토큰을 저장한다") {
            runTest {
                val localAuthDataSource =
                    mock<LocalAuthDataSource> {
                        everySuspend { fetchAccessToken() } returns "oldAccess"
                        everySuspend { fetchRefreshToken() } returns "oldRefresh"
                        everySuspend { saveTokens(any(), any()) } returns Unit
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
                    localAuthDataSource.saveTokens(accessToken = "newAccess", refreshToken = "newRefresh")
                }
            }
        }

        test("refreshToken은 저장된 토큰이 비어 있으면 TokenEmpty를 던진다") {
            runTest {
                val localAuthDataSource =
                    mock<LocalAuthDataSource> {
                        everySuspend { fetchAccessToken() } returns null
                        everySuspend { fetchRefreshToken() } returns "oldRefresh"
                    }
                val repository = repositoryWith(localAuthDataSource = localAuthDataSource)

                shouldThrow<CaroAuthException.TokenEmpty> {
                    repository.refreshToken()
                }
            }
        }
    }
}
