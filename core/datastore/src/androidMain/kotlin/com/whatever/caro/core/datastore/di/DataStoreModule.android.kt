package com.whatever.caro.core.datastore.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.whatever.caro.core.datastore.DataStoreFactory
import com.whatever.caro.core.datastore.PreferencesPathProvider
import com.whatever.caro.core.datastore.datasource.LocalAuthDataSource
import com.whatever.caro.core.datastore.datasource.LocalAuthDataSourceImpl
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

actual val dataStoreModule: Module =
    module {
        single<PreferencesPathProvider>()

        single<DataStore<Preferences>> {
            val pathProvider: PreferencesPathProvider = get()
            DataStoreFactory.create(
                producePath = {
                    pathProvider.resolve(DataStoreFactory.AUTH_PREFERENCES_NAME)
                },
            )
        }

        single<LocalAuthDataSourceImpl>() bind LocalAuthDataSource::class
    }
