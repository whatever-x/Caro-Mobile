package com.whatever.caro.core.remote.dto.notification.request

import kotlinx.serialization.Serializable

// AUTO-GENERATED FROM SWAGGER — 직접 수정하지 마세요
@Serializable
data class RegisterDeviceTokenRequest(
    /** FCM registration token */
    val token: String,
    /** 기기 플랫폼 */
    val platform: PlatformDto,
) {
    @Serializable
    enum class PlatformDto {
        ANDROID,
        IOS,
    }
}
