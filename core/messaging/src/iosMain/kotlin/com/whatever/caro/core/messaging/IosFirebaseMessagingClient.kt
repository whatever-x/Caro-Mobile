package com.whatever.caro.core.messaging

import kotlinx.coroutines.flow.SharedFlow

internal class IosFirebaseMessagingClient : MessagingClient {
    override val tokenFlow: SharedFlow<String> = IosMessagingEvents.tokenFlow
    override val messageFlow: SharedFlow<RemoteMessage> = IosMessagingEvents.messageFlow
}
