package com.whatever.caro.core.datastore.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal class LocalInterstitialFrequencyDataSourceImpl(
    private val dataStore: DataStore<Preferences>,
) : LocalInterstitialFrequencyDataSource {
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun reserve(localDate: String): String? {
        val requestedDate = LocalDate.parse(localDate)
        var reservationId: String? = null
        dataStore.edit { preferences ->
            val consumedDate = preferences[KEY_DATE]?.let { LocalDate.parse(it) }
            val pending = preferences[KEY_RESERVATION] != null
            check(consumedDate != null || !pending) { "Missing interstitial reservation date" }
            // 미확정 예약은 자정 이후 실제 노출 직후 앱이 종료됐을 가능성이 있어 다음 날도 차단한다.
            val blockedThrough = if (pending) consumedDate?.plus(DatePeriod(days = 1)) else consumedDate
            if (blockedThrough == null || requestedDate > blockedThrough) {
                val token = Uuid.random().toString()
                preferences[KEY_DATE] = requestedDate.toString()
                preferences[KEY_RESERVATION] = token
                reservationId = token
            }
        }
        return reservationId
    }

    override suspend fun confirmShown(
        reservationId: String,
        localDate: String,
    ) {
        dataStore.edit { preferences ->
            if (preferences[KEY_RESERVATION] == reservationId) {
                preferences[KEY_DATE] = LocalDate.parse(localDate).toString()
                preferences.remove(KEY_RESERVATION)
            }
        }
    }

    override suspend fun release(reservationId: String) {
        dataStore.edit { preferences ->
            if (preferences[KEY_RESERVATION] == reservationId) {
                preferences.remove(KEY_DATE)
                preferences.remove(KEY_RESERVATION)
            }
        }
    }

    private companion object {
        val KEY_DATE = stringPreferencesKey("interstitial_consumed_local_date")
        val KEY_RESERVATION = stringPreferencesKey("interstitial_reservation_id")
    }
}
