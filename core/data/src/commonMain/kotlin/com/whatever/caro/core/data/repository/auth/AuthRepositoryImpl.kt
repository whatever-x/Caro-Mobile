package com.whatever.caro.core.data.repository.auth

import com.whatever.caro.core.data.mapper.toAuthSession
import com.whatever.caro.core.data.repository.ads.AdEligibilityGate
import com.whatever.caro.core.data.repository.fcm.FcmTokenRepository
import com.whatever.caro.core.data.util.suspendRunCatching
import com.whatever.caro.core.datastore.datasource.LocalAuthDataSource
import com.whatever.caro.core.model.auth.AuthSession
import com.whatever.caro.core.model.auth.SocialLoginType
import com.whatever.caro.core.model.exception.CaroAuthException
import com.whatever.caro.core.remote.datasource.auth.AuthDataSource
import com.whatever.caro.core.remote.datasource.auth.NonAuthDataSource
import com.whatever.caro.core.remote.dto.auth.request.CompleteRegistrationV11Request
import com.whatever.caro.core.remote.dto.auth.request.RefreshTokenRequest
import com.whatever.caro.core.remote.dto.auth.request.SocialLoginRequest
import io.github.aakira.napier.Napier
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate

internal class AuthRepositoryImpl(
    private val remoteAuthDataSource: AuthDataSource,
    private val remoteNonAuthDataSource: NonAuthDataSource,
    private val localAuthDataSource: LocalAuthDataSource,
    private val fcmTokenRepository: FcmTokenRepository,
    private val adEligibilityGate: AdEligibilityGate,
) : AuthRepository {
    private val sessionMutation = Mutex()

    override suspend fun loginWithSocial(
        provider: SocialLoginType,
        idToken: String,
    ): Boolean =
        sessionMutation.withLock {
            adEligibilityGate.beginTransition()
            try {
                val request =
                    SocialLoginRequest(
                        provider = provider,
                        idToken = idToken,
                    )
                val response = remoteNonAuthDataSource.socialLogin(request = request)
                localAuthDataSource.saveSession(
                    accessToken = response.accessToken,
                    refreshToken = response.refreshToken,
                    isRegistrationComplete = response.isRegistrationComplete,
                )
                fcmTokenRepository.resumeRegistration()
                return response.isRegistrationComplete
            } finally {
                adEligibilityGate.endTransition()
            }
        }

    /**
     * 로그아웃은 "이 기기의 세션을 끝낸다"는 사용자 의도이므로 원격 호출 실패(네트워크·서버 오류)로 되돌리지 않는다.
     * 실패로 처리하면 사용자가 로그아웃하지 못한 채 유효한 토큰을 들고 앱에 남는다.
     */
    override suspend fun logout() =
        sessionMutation.withLock {
            adEligibilityGate.beginTransition()
            try {
                suspendRunCatching {
                    fcmTokenRepository.unregisterToken()
                }.onFailure {
                    Napier.w { "FCM token removal failed; continuing logout" }
                }
                suspendRunCatching {
                    remoteAuthDataSource.logout()
                }.onFailure { throwable ->
                    Napier.w(throwable = throwable) { "remote logout failed" }
                }
                clearLocalSession()
            } finally {
                adEligibilityGate.endTransition()
            }
        }

    override suspend fun withdraw() =
        sessionMutation.withLock {
            adEligibilityGate.beginTransition()
            try {
                remoteAuthDataSource.withdraw()
                clearLocalSession()
            } finally {
                adEligibilityGate.endTransition()
            }
        }

    /**
     * 원격 로그아웃·탈퇴는 되돌릴 수 없으므로, 로컬 토큰 삭제가 실패해도 전체 요청을 실패로 만들지 않는다.
     * 실패로 처리하면 클라이언트가 이미 무효해진 세션으로 로그인 상태를 유지하게 된다.
     */
    private suspend fun clearLocalSession() {
        suspendRunCatching {
            localAuthDataSource.clear()
        }.onFailure { throwable ->
            Napier.w(throwable = throwable) { "clearLocalSession failed" }
        }
    }

    override suspend fun completeRegistration(
        nickname: String,
        termsAgreed: Boolean,
        birthDate: String,
    ): AuthSession =
        sessionMutation.withLock {
            adEligibilityGate.beginTransition()
            try {
                val request =
                    CompleteRegistrationV11Request(
                        nickname = nickname,
                        isTermsAgreed = termsAgreed,
                        birthDate = LocalDate.parse(birthDate),
                    )
                val response = remoteAuthDataSource.completeRegistration(request = request)
                localAuthDataSource.saveSession(
                    accessToken = response.accessToken,
                    refreshToken = response.refreshToken,
                    isRegistrationComplete = true,
                )
                fcmTokenRepository.resumeRegistration()
                return response.toAuthSession()
            } finally {
                adEligibilityGate.endTransition()
            }
        }

    override suspend fun isRegistrationComplete(): Boolean = localAuthDataSource.fetchRegistrationComplete() ?: true

    override suspend fun refreshToken() =
        sessionMutation.withLock {
            adEligibilityGate.beginTransition()
            try {
                val current = localAuthDataSource.fetchTokens()
                val accessToken = current.accessToken
                val refreshToken = current.refreshToken

                if (accessToken.isNullOrEmpty() || refreshToken.isNullOrEmpty()) {
                    throw CaroAuthException.TokenEmpty(
                        debugMessage = "Token is Empty",
                    )
                }
                val request =
                    RefreshTokenRequest(
                        accessToken = accessToken,
                        refreshToken = refreshToken,
                    )
                remoteNonAuthDataSource
                    .refreshToken(
                        request = request,
                    ).also {
                        if (!localAuthDataSource.replaceTokensIfCurrent(current, it.accessToken, it.refreshToken)) {
                            throw CaroAuthException.TokenExpired(debugMessage = "Session changed during refresh")
                        }
                    }
                Unit
            } finally {
                adEligibilityGate.endTransition()
            }
        }
}
