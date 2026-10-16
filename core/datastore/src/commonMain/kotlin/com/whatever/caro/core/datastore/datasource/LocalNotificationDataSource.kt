package com.whatever.caro.core.datastore.datasource

interface LocalNotificationDataSource {
    suspend fun consumeInitialPermissionRequest(): Boolean
}
