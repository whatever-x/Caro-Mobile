package com.whatever.caro.core.data.repository.fcm

import com.whatever.caro.core.remote.dto.notification.request.RegisterDeviceTokenRequest

internal actual fun notificationPlatform(): RegisterDeviceTokenRequest.PlatformDto = RegisterDeviceTokenRequest.PlatformDto.ANDROID
