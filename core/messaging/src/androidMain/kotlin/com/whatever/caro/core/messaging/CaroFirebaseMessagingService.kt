package com.whatever.caro.core.messaging

import com.google.firebase.messaging.FirebaseMessagingService
import io.github.aakira.napier.Napier
import org.koin.mp.KoinPlatform
import com.google.firebase.messaging.RemoteMessage as FcmRemoteMessage

class CaroFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Napier.d { "FCM token refreshed" }
        MessagingEventBus.publishToken(token)
    }

    override fun onMessageReceived(message: FcmRemoteMessage) {
        super.onMessageReceived(message)
        KoinPlatform
            .getKoin()
            .get<IncomingNotificationHandler>()
            .onReceived(message.notification?.title, message.notification?.body)
    }
}
