package com.whatever.caro.core.datastore.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.whatever.caro.core.datastore.datasource.TokenLocalDataSource
import com.whatever.caro.core.datastore.DataStoreFactory
import com.whatever.caro.core.datastore.PreferencesPathProvider
import com.whatever.caro.core.datastore.datasource.TokenLocalDataSourceImpl
import org.koin.core.module.Module
import org.koin.dsl.module

actual val dataStoreModule: Module =
    module {
        single { PreferencesPathProvider() }

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
