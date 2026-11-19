package com.whatever.caro.core.remote.datasource.auth

import com.whatever.caro.core.remote.dto.auth.request.CompleteRegistrationV11Request
import com.whatever.caro.core.remote.dto.auth.response.TokenResponse

interface AuthDataSource {
    suspend fun completeRegistration(request: CompleteRegistrationV11Request): TokenResponse

    suspend fun logout()

    suspend fun withdraw()
}
