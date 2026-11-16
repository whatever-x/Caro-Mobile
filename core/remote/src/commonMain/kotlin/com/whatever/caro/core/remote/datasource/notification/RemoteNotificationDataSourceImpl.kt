package com.whatever.caro.core.remote.datasource.notification

import com.whatever.caro.core.remote.api.NotificationApi
import com.whatever.caro.core.remote.dto.notification.request.RegisterDeviceTokenRequest
import com.whatever.caro.core.remote.dto.notification.response.TestPushResponse

internal class RemoteNotificationDataSourceImpl(
    private val notificationApi: NotificationApi,
) : NotificationDataSource {
    override suspend fun registerToken(request: RegisterDeviceTokenRequest) = notificationApi.requestRegisterToken(request)

    override suspend fun unregisterToken(token: String) = notificationApi.requestUnregisterToken(token)

    override suspend fun sendTestPush(): TestPushResponse = notificationApi.requestSendTestPush()
}
