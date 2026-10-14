package com.whatever.caro.core.remote.api

import com.whatever.caro.core.remote.dto.notification.request.RegisterDeviceTokenRequest
import com.whatever.caro.core.remote.dto.notification.response.TestPushResponse
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.DELETE
import de.jensklingenberg.ktorfit.http.Headers
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.PUT
import de.jensklingenberg.ktorfit.http.Query

internal interface NotificationApi {
    @Headers(ApiVersionHeaders.V1_0)
    @PUT("notifications/devices")
    suspend fun requestRegisterToken(
        @Body request: RegisterDeviceTokenRequest,
    )

    @Headers(ApiVersionHeaders.V1_0)
    @DELETE("notifications/devices")
    suspend fun requestUnregisterToken(
        @Query("token") token: String,
    )

    @Headers(ApiVersionHeaders.V1_0)
    @POST("notifications/test")
    suspend fun requestSendTestPush(): TestPushResponse
}
