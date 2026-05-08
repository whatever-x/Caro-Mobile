package com.whatever.caro.core.messaging.di

import com.whatever.caro.core.messaging.IosFirebaseMessagingClient
import com.whatever.caro.core.messaging.MessagingClient
import org.koin.core.module.Module
import org.koin.dsl.module

actual val messagingModule: Module =
    module {
        single<MessagingClient> { IosFirebaseMessagingClient() }
    }
