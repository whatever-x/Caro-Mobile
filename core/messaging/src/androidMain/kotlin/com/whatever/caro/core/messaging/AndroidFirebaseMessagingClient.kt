package com.whatever.caro.core.messaging

import com.google.firebase.messaging.FirebaseMessaging
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.StateFlow

internal class AndroidFirebaseMessagingClient : MessagingClient {
    override val tokenFlow: StateFlow<String> = MessagingEventBus.tokenFlow

    init {
        FirebaseMessaging
            .getInstance()
            .token
            .addOnSuccessListener { token -> MessagingEventBus.publishToken(token) }
            .addOnFailureListener { e -> Napier.e(e) { "FirebaseMessaging.getToken failed" } }
    }
}
