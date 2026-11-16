package com.whatever.caro.core.datastore.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal class LocalReviewRequestDataSourceImpl(
    private val dataStore: DataStore<Preferences>,
) : LocalReviewRequestDataSource {
    override suspend fun tryReserveRequest(
        reservationId: String,
        nowEpochMilliseconds: Long,
        minimumIntervalMilliseconds: Long,
    ): Boolean {
        currentCoroutineContext().ensureActive()
        var reserved = false
        dataStore.edit { preferences ->
            currentCoroutineContext().ensureActive()
            val previous = preferences[KEY_LAST_REVIEW_REQUEST]
            if (previous == null || (nowEpochMilliseconds >= previous && nowEpochMilliseconds - previous >= minimumIntervalMilliseconds)) {
                if (previous == null) {
                    preferences.remove(KEY_PREVIOUS_REVIEW_REQUEST)
                } else {
                    preferences[KEY_PREVIOUS_REVIEW_REQUEST] = previous
                }
                preferences[KEY_RESERVATION_ID] = reservationId
                // A process death at the SDK boundary is ambiguous. This durable claim expires
                // through the same interval check; an orphan never becomes a permanent lock.
                preferences[KEY_LAST_REVIEW_REQUEST] = nowEpochMilliseconds
                reserved = true
            }
        }
        return reserved
    }

    override suspend fun commitRequest(
        reservationId: String,
        attemptedAtEpochMilliseconds: Long,
    ): Boolean = finishReservation(reservationId, attemptedAtEpochMilliseconds)

    override suspend fun cancelRequest(reservationId: String): Boolean = finishReservation(reservationId, null)

    private suspend fun finishReservation(
        reservationId: String,
        attemptedAtEpochMilliseconds: Long?,
    ): Boolean {
        var finished = false
        dataStore.edit { preferences ->
            if (preferences[KEY_RESERVATION_ID] == reservationId) {
                val timestamp = attemptedAtEpochMilliseconds ?: preferences[KEY_PREVIOUS_REVIEW_REQUEST]
                if (timestamp == null) {
                    preferences.remove(KEY_LAST_REVIEW_REQUEST)
                } else {
                    preferences[KEY_LAST_REVIEW_REQUEST] = timestamp
                }
                preferences.remove(KEY_RESERVATION_ID)
                preferences.remove(KEY_PREVIOUS_REVIEW_REQUEST)
                finished = true
            }
        }
        return finished
    }

    private companion object {
        val KEY_LAST_REVIEW_REQUEST = longPreferencesKey("last_in_app_review_request_epoch_milliseconds")
        val KEY_RESERVATION_ID = stringPreferencesKey("in_app_review_reservation_id")
        val KEY_PREVIOUS_REVIEW_REQUEST = longPreferencesKey("previous_in_app_review_request_epoch_milliseconds")
    }
}
