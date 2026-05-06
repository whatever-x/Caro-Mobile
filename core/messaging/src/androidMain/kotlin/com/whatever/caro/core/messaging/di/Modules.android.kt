package com.whatever.caro.core.messaging.di

import com.whatever.caro.core.messaging.AndroidFirebaseMessagingClient
import com.whatever.caro.core.messaging.MessagingClient
import org.koin.dsl.module

actual val messagingModule =
    module {
        single<MessagingClient> { AndroidFirebaseMessagingClient() }
    }
