package com.whatever.caro.core.datastore.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit

internal class LocalNotificationDataSourceImpl(
    private val dataStore: DataStore<Preferences>,
) : LocalNotificationDataSource {
    override suspend fun consumeInitialPermissionRequest(): Boolean {
        var consumed = false
        dataStore.edit { preferences ->
            if (preferences[KEY_PERMISSION_REQUESTED] != true) {
                preferences[KEY_PERMISSION_REQUESTED] = true
                consumed = true
            }
        }
        return consumed
    }

    private companion object {
        val KEY_PERMISSION_REQUESTED = booleanPreferencesKey("notification_permission_requested")
    }
}
