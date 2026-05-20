package com.whatever.caro.core.datastore.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

internal class LocalAuthDataSourceImpl(
    private val dataStore: DataStore<Preferences>,
) : LocalAuthDataSource {
    override suspend fun fetchAccessToken(): String? =
        dataStore.data
            .map { it[KEY_ACCESS_TOKEN] }
            .firstOrNull()

    override suspend fun fetchRefreshToken(): String? =
        dataStore.data
            .map { it[KEY_REFRESH_TOKEN] }
            .firstOrNull()

    override suspend fun saveTokens(
        accessToken: String,
        refreshToken: String,
    ) {
        dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = accessToken
            prefs[KEY_REFRESH_TOKEN] = refreshToken
        }
    }

    override suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_ACCESS_TOKEN)
            prefs.remove(KEY_REFRESH_TOKEN)
        }
    }

    private companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("auth_access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("auth_refresh_token")
    }
}
