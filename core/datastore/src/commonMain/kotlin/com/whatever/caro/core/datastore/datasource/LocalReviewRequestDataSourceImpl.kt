package com.whatever.caro.core.datastore.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal class LocalReviewRequestDataSourceImpl(
    private val dataStore: DataStore<Preferences>,
) : LocalReviewRequestDataSource {
    override suspend fun tryReserveRequest(
        nowEpochMilliseconds: Long,
        minimumIntervalMilliseconds: Long,
    ): Boolean {
        currentCoroutineContext().ensureActive()
        var reserved = false
        dataStore.edit { preferences ->
            currentCoroutineContext().ensureActive()
            val previous = preferences[KEY_LAST_REVIEW_REQUEST]
            if (previous == null || (nowEpochMilliseconds >= previous && nowEpochMilliseconds - previous >= minimumIntervalMilliseconds)) {
                preferences[KEY_LAST_REVIEW_REQUEST] = nowEpochMilliseconds
                reserved = true
            }
        }
        return reserved
    }

    private companion object {
        val KEY_LAST_REVIEW_REQUEST = longPreferencesKey("last_in_app_review_request_epoch_milliseconds")
    }
}
