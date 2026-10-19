package com.whatever.caro.core.datastore.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

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

    override fun observeNotificationEnabled(): Flow<Boolean?> = dataStore.data.map { it[KEY_ENABLED] }.distinctUntilChanged()

    override suspend fun saveNotificationEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_ENABLED] = enabled }
    }

    override suspend fun fetchRegisteredToken(): String? = dataStore.data.first()[KEY_REGISTERED_TOKEN]

    override suspend fun saveRegisteredToken(token: String?) {
        dataStore.edit { preferences ->
            if (token == null) preferences.remove(KEY_REGISTERED_TOKEN) else preferences[KEY_REGISTERED_TOKEN] = token
        }
    }

    private companion object {
        val KEY_ENABLED = booleanPreferencesKey("notification_enabled")
        val KEY_REGISTERED_TOKEN = stringPreferencesKey("notification_registered_token")
        val KEY_PERMISSION_REQUESTED = booleanPreferencesKey("notification_permission_requested")
    }
}
