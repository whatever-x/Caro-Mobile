package com.whatever.caro.core.datastore.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.whatever.caro.core.datastore.read
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import okio.IOException

internal class LocalAuthDataSourceImpl(
    private val dataStore: DataStore<Preferences>,
) : LocalAuthDataSource {
    override fun observeRegisteredAccessToken(): Flow<String?> =
        dataStore.data
            .catch { cause ->
                if (cause is IOException) emit(emptyPreferences()) else throw cause
            }.map { preferences ->
                preferences[KEY_ACCESS_TOKEN]?.takeIf { it.isNotBlank() && preferences[KEY_REGISTRATION_COMPLETE] == true }
            }.distinctUntilChanged()

    override suspend fun fetchAccessToken(): String? = dataStore.read(KEY_ACCESS_TOKEN)

    override suspend fun fetchRefreshToken(): String? = dataStore.read(KEY_REFRESH_TOKEN)

    override suspend fun saveTokens(
        accessToken: String,
        refreshToken: String,
    ) {
        dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = accessToken
            prefs[KEY_REFRESH_TOKEN] = refreshToken
        }
    }

    override suspend fun fetchRegistrationComplete(): Boolean? = dataStore.read(KEY_REGISTRATION_COMPLETE)

    override suspend fun saveRegistrationComplete(isComplete: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_REGISTRATION_COMPLETE] = isComplete
        }
    }

    override suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_ACCESS_TOKEN)
            prefs.remove(KEY_REFRESH_TOKEN)
            prefs.remove(KEY_REGISTRATION_COMPLETE)
        }
    }

    private companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("auth_access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("auth_refresh_token")
        private val KEY_REGISTRATION_COMPLETE = booleanPreferencesKey("auth_registration_complete")
    }
}
