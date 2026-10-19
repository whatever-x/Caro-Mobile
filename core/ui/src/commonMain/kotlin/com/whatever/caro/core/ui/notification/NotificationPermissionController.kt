package com.whatever.caro.core.ui.notification

import androidx.compose.runtime.Composable

interface NotificationPermissionController {
    suspend fun isGranted(): Boolean

    suspend fun request(): Boolean

    fun openSettings()
}

@Composable
expect fun rememberNotificationPermissionController(): NotificationPermissionController
