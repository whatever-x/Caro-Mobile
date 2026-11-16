package com.whatever.caro.core.data.repository.fcm

import com.whatever.caro.core.remote.dto.notification.request.RegisterDeviceTokenRequest

internal expect fun notificationPlatform(): RegisterDeviceTokenRequest.PlatformDto
