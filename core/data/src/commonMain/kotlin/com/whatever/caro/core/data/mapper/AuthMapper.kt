package com.whatever.caro.core.data.mapper

import com.whatever.caro.core.model.auth.AuthSession
import com.whatever.caro.core.remote.dto.auth.response.SocialLoginResponse
import com.whatever.caro.core.remote.dto.auth.response.TokenResponse

fun SocialLoginResponse.toAuthSession(): AuthSession =
    AuthSession(
        accessToken = this.accessToken,
        refreshToken = this.refreshToken,
    )

fun TokenResponse.toAuthSession(): AuthSession =
    AuthSession(
        accessToken = this.accessToken,
        refreshToken = this.refreshToken,
    )
