package com.whatever.caro.core.datastore.internal

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

internal object DataStoreFactory {
    const val AUTH_PREFERENCES_NAME = "caro_auth.preferences_pb"

    fun create(producePath: () -> String): DataStore<Preferences> =
        PreferenceDataStoreFactory.createWithPath(
            produceFile = { producePath().toPath() },
        )
}
