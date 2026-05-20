package com.whatever.caro.core.datastore.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import okio.IOException

internal class LocalAuthDataSourceImpl(
    private val dataStore: DataStore<Preferences>,
) : LocalAuthDataSource {
    override suspend fun fetchAccessToken(): String? = readPreference(KEY_ACCESS_TOKEN)

    override suspend fun fetchRefreshToken(): String? = readPreference(KEY_REFRESH_TOKEN)

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

    private suspend fun readPreference(key: Preferences.Key<String>): String? =
        dataStore.data
            .catch { cause ->
                if (cause is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw cause
                }
            }.map { it[key] }
            .firstOrNull()

    private companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("auth_access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("auth_refresh_token")
    }
}
