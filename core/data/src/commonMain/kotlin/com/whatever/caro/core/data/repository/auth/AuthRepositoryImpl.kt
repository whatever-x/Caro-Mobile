package com.whatever.caro.core.data.repository.auth

import com.whatever.caro.core.data.mapper.toAuthSession
import com.whatever.caro.core.data.util.suspendRunCatching
import com.whatever.caro.core.datastore.datasource.LocalAuthDataSource
import com.whatever.caro.core.model.auth.AuthSession
import com.whatever.caro.core.model.auth.SocialLoginType
import com.whatever.caro.core.model.exception.CaroAuthException
import com.whatever.caro.core.remote.datasource.auth.AuthDataSource
import com.whatever.caro.core.remote.datasource.auth.NonAuthDataSource
import com.whatever.caro.core.remote.dto.auth.request.CompleteRegistrationRequest
import com.whatever.caro.core.remote.dto.auth.request.RefreshTokenRequest
import com.whatever.caro.core.remote.dto.auth.request.SocialLoginRequest
import io.github.aakira.napier.Napier

internal class AuthRepositoryImpl(
    private val remoteAuthDataSource: AuthDataSource,
    private val remoteNonAuthDataSource: NonAuthDataSource,
    private val localAuthDataSource: LocalAuthDataSource,
) : AuthRepository {
    override suspend fun loginWithSocial(
        provider: SocialLoginType,
        idToken: String,
    ): Boolean {
        val request =
            SocialLoginRequest(
                provider = provider,
                idToken = idToken,
            )
        val response = remoteNonAuthDataSource.socialLogin(request = request)
        localAuthDataSource.saveTokens(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
        )
        localAuthDataSource.saveRegistrationComplete(response.isRegistrationComplete)
        return response.isRegistrationComplete
    }

    /**
     * 로그아웃은 "이 기기의 세션을 끝낸다"는 사용자 의도이므로 원격 호출 실패(네트워크·서버 오류)로 되돌리지 않는다.
     * 실패로 처리하면 사용자가 로그아웃하지 못한 채 유효한 토큰을 들고 앱에 남는다.
     */
    override suspend fun logout() {
        suspendRunCatching {
            remoteAuthDataSource.logout()
        }.onFailure { throwable ->
            Napier.w(throwable = throwable) { "remote logout failed" }
        }
        clearLocalSession()
    }

    override suspend fun withdraw() {
        remoteAuthDataSource.withdraw()
        clearLocalSession()
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
    ): AuthSession {
        val request =
            CompleteRegistrationRequest(
                nickname = nickname,
                isTermsAgreed = termsAgreed,
            )
        val response = remoteAuthDataSource.completeRegistration(request = request)
        localAuthDataSource.saveTokens(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
        )
        localAuthDataSource.saveRegistrationComplete(true)
        return response.toAuthSession()
    }

    override suspend fun isRegistrationComplete(): Boolean = localAuthDataSource.fetchRegistrationComplete() ?: true

    override suspend fun refreshToken() {
        val accessToken = localAuthDataSource.fetchAccessToken()
        val refreshToken = localAuthDataSource.fetchRefreshToken()

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
                localAuthDataSource.saveTokens(
                    accessToken = it.accessToken,
                    refreshToken = it.refreshToken,
                )
            }
    }
}
