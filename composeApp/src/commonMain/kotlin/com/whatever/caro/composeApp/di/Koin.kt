package com.whatever.caro.composeApp.di

import com.whatever.caro.core.analytics.di.analyticsModule
import com.whatever.caro.core.crashlytics.di.crashlyticsModule
import com.whatever.caro.core.data.di.dataModule
import com.whatever.caro.core.datastore.di.dataStoreModule
import com.whatever.caro.core.messaging.di.messagingModule
import com.whatever.caro.core.navigator.di.navigatorModule
import com.whatever.caro.core.remote.di.apiModule
import com.whatever.caro.core.remote.di.deviceModule
import com.whatever.caro.core.remote.di.networkModule
import com.whatever.caro.core.remote.di.remoteModule
import com.whatever.caro.feature.deck.di.deckModule
import com.whatever.caro.feature.home.di.homeModule
import com.whatever.caro.feature.login.di.loginModule
import com.whatever.caro.feature.login.di.socialModule
import com.whatever.caro.feature.profile.di.profileModule
import com.whatever.caro.feature.splash.di.splashModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.includes

fun initKoin(configuration: KoinAppDeclaration? = null) {
    startKoin {
        includes(configuration)
        modules(
            // Navigation
            navEntryModule,
            navigatorModule,
            // data
            dataModule,
            dataStoreModule,
            networkModule,
            apiModule,
            remoteModule,
            deviceModule,
            analyticsModule,
            crashlyticsModule,
            messagingModule,
            // feature
            homeModule,
            loginModule,
            socialModule,
            splashModule,
            profileModule,
            deckModule,
        )
    }
}
