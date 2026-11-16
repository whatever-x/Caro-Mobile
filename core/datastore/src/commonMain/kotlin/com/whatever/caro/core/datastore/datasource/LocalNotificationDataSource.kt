package com.whatever.caro.core.datastore.datasource

import kotlinx.coroutines.flow.Flow

interface LocalNotificationDataSource {
    suspend fun consumeInitialPermissionRequest(): Boolean

    fun observeNotificationEnabled(): Flow<Boolean?>

    suspend fun saveNotificationEnabled(enabled: Boolean)

    suspend fun fetchRegisteredToken(): String?

    suspend fun saveRegisteredToken(token: String?)
}
