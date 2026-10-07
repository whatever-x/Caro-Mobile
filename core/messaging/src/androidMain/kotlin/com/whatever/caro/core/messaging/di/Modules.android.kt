package com.whatever.caro.core.messaging.di

import com.whatever.caro.core.messaging.AndroidFirebaseMessagingClient
import com.whatever.caro.core.messaging.AndroidNotificationPresenter
import com.whatever.caro.core.messaging.IncomingNotificationHandler
import com.whatever.caro.core.messaging.MessagingClient
import com.whatever.caro.core.messaging.NotificationPresenter
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

actual val messagingModule =
    module {
        single<AndroidFirebaseMessagingClient>() bind MessagingClient::class
        single<AndroidNotificationPresenter>() bind NotificationPresenter::class
        single<IncomingNotificationHandler>()
    }
