package com.whatever.caro.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import okio.IOException

internal suspend fun <T> DataStore<Preferences>.read(key: Preferences.Key<T>): T? =
    data
        .catch { cause ->
            if (cause is IOException) {
                emit(emptyPreferences())
            } else {
                throw cause
            }
        }.map { it[key] }
        .firstOrNull()
