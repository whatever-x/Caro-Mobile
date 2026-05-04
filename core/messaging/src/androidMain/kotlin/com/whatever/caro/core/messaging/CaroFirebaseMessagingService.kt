package com.whatever.caro.core.messaging

import com.google.firebase.messaging.FirebaseMessagingService

class CaroFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        MessagingEventBus.publishToken(token)
    }
}
