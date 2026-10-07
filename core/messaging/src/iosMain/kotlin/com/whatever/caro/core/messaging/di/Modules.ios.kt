package com.whatever.caro.core.messaging.di

import com.whatever.caro.core.messaging.IosFirebaseMessagingClient
import com.whatever.caro.core.messaging.MessagingClient
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

actual val messagingModule: Module =
    module {
        single<IosFirebaseMessagingClient>() bind MessagingClient::class
    }
