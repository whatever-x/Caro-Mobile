package com.whatever.caro.core.messaging

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.StateFlow

internal class IosFirebaseMessagingClient : MessagingClient {
    override val tokenFlow: StateFlow<String> = IosMessagingEvents.tokenFlow
    override val messages: ReceiveChannel<CloudMessage> = IosMessagingEvents.messages
}
