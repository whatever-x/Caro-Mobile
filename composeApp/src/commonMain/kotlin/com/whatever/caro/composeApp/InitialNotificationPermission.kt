package com.whatever.caro.composeApp

import com.whatever.caro.core.datastore.datasource.LocalNotificationDataSource
import com.whatever.caro.core.ui.notification.NotificationPermissionController

internal suspend fun requestInitialNotificationPermission(
    localNotificationDataSource: LocalNotificationDataSource,
    permissionController: NotificationPermissionController,
) {
    if (localNotificationDataSource.consumeInitialPermissionRequest()) permissionController.request()
}
