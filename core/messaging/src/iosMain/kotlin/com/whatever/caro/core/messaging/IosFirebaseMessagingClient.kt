package com.whatever.caro.core.messaging

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class IosFirebaseMessagingClient : MessagingClient {
    override val tokenFlow: SharedFlow<String> = IosMessagingEvents.tokenFlow
    override val messageFlow: SharedFlow<RemoteMessage> = IosMessagingEvents.messageFlow

    override suspend fun currentToken(): String? =
        suspendCancellableCoroutine { continuation ->
            requireBridge().fetchToken { token, error ->
                if (error != null) {
                    continuation.resumeWithException(MessagingException(error))
                } else {
                    continuation.resume(token)
                }
            }
        }

    override suspend fun deleteToken() =
        awaitErrorCallback { callback ->
            requireBridge().deleteToken(callback)
        }

    override suspend fun subscribeToTopic(topic: String) =
        awaitErrorCallback { callback ->
            requireBridge().subscribe(topic, callback)
        }

    override suspend fun unsubscribeFromTopic(topic: String) =
        awaitErrorCallback { callback ->
            requireBridge().unsubscribe(topic, callback)
        }

    private suspend fun awaitErrorCallback(action: ((error: String?) -> Unit) -> Unit) =
        suspendCancellableCoroutine { continuation ->
            action { error ->
                if (error != null) {
                    continuation.resumeWithException(MessagingException(error))
                } else {
                    continuation.resume(Unit)
                }
            }
        }

    private fun requireBridge(): IosMessagingBridge =
        IosMessagingBridgeHolder.bridge
            ?: throw MessagingException(
                "IosMessagingBridge is not registered. Call registerIosMessagingBridge from Swift before using MessagingClient.",
            )
}
