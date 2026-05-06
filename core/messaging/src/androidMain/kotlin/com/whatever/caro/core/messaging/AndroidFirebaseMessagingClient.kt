package com.whatever.caro.core.messaging

import com.google.firebase.messaging.FirebaseMessaging
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.SharedFlow

internal class AndroidFirebaseMessagingClient : MessagingClient {
    override val tokenFlow: SharedFlow<String> = MessagingEventBus.tokenFlow
    override val messageFlow: SharedFlow<RemoteMessage> = MessagingEventBus.messageFlow

    init {
        // onNewToken은 토큰 변경 시에만 호출되므로, 앱 실행 시 캐시된 토큰을 한 번 push.
        FirebaseMessaging
            .getInstance()
            .token
            .addOnSuccessListener { token -> MessagingEventBus.publishToken(token) }
            .addOnFailureListener { e -> Napier.e(e) { "FirebaseMessaging.getToken failed" } }
    }
}
