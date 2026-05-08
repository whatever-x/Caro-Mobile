package com.whatever.caro.core.data.mapper

import com.whatever.caro.core.model.auth.AuthSession
import com.whatever.caro.core.remote.dto.auth.response.LoginResponse

fun LoginResponse.toAuthSession(): AuthSession =
    AuthSession(
        accessToken = this.accessToken,
        refreshToken = this.refreshToken,
    )
