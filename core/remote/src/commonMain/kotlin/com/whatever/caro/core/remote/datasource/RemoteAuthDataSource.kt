package com.whatever.caro.core.remote.datasource

import com.whatever.caro.core.remote.dto.auth.request.CompleteRegistrationRequest
import com.whatever.caro.core.remote.dto.auth.response.TokenResponse

interface RemoteAuthDataSource {
    suspend fun completeRegistration(request: CompleteRegistrationRequest): TokenResponse

    suspend fun logout()
}
