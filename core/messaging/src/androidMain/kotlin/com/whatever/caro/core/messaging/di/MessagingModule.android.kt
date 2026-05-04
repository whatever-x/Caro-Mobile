package com.whatever.caro.core.messaging.di

import com.whatever.caro.core.messaging.FirebaseMessagingClient
import com.whatever.caro.core.messaging.MessagingClient
import org.koin.dsl.module

actual val messagingModule =
    module {
        single<MessagingClient> { FirebaseMessagingClient() }
    }
