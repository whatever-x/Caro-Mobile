package com.whatever.caro.core.messaging

import com.google.firebase.messaging.FirebaseMessagingService
import io.github.aakira.napier.Napier
import com.google.firebase.messaging.RemoteMessage as FcmRemoteMessage

class CaroFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Napier.d { "token: $token" }
        MessagingEventBus.publishToken(token)
    }

    override fun onMessageReceived(message: FcmRemoteMessage) {
        super.onMessageReceived(message)
        val cloudMessage = message.data
    }
}
