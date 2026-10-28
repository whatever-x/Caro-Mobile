package com.whatever.caro.core.remote.datasource.notification

import com.whatever.caro.core.remote.dto.notification.request.RegisterDeviceTokenRequest
import com.whatever.caro.core.remote.dto.notification.response.TestPushResponse

interface NotificationDataSource {
    suspend fun registerToken(request: RegisterDeviceTokenRequest)

    suspend fun unregisterToken(token: String)

    suspend fun sendTestPush(): TestPushResponse
}
