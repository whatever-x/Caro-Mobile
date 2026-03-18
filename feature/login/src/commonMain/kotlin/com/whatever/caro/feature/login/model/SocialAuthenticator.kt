package com.whatever.caro.feature.login.model

interface SocialAuthenticator<T> {
    suspend fun authenticate(): SocialLoginResult<T>
}
