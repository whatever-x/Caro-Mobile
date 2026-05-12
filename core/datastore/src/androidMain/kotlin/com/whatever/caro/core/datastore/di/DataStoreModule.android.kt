package com.whatever.caro.core.datastore.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.whatever.caro.core.datastore.auth.TokenLocalDataSource
import com.whatever.caro.core.datastore.internal.DataStoreFactory
import com.whatever.caro.core.datastore.internal.PreferencesPathProvider
import com.whatever.caro.core.datastore.internal.TokenLocalDataSourceImpl
import org.koin.core.module.Module
import org.koin.dsl.module

actual val dataStoreModule: Module =
    module {
        single { PreferencesPathProvider(context = get()) }

        single<DataStore<Preferences>> {
            val pathProvider: PreferencesPathProvider = get()
            DataStoreFactory.create(
                producePath = {
                    pathProvider.resolve(DataStoreFactory.AUTH_PREFERENCES_NAME)
                },
            )
        }

        single<TokenLocalDataSource> { TokenLocalDataSourceImpl(dataStore = get()) }
    }
