package com.whatever.caro.core.messaging

import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class FirebaseMessagingClient : MessagingClient {
    private val firebaseMessaging: FirebaseMessaging
        get() = FirebaseMessaging.getInstance()

    override val tokenFlow: SharedFlow<String> = MessagingEventBus.tokenFlow
    override val messageFlow: SharedFlow<RemoteMessage> = MessagingEventBus.messageFlow

    override suspend fun currentToken(): String? = firebaseMessaging.token.awaitTask()

    override suspend fun deleteToken() {
        firebaseMessaging.deleteToken().awaitTask()
    }

    override suspend fun subscribeToTopic(topic: String) {
        firebaseMessaging.subscribeToTopic(topic).awaitTask()
    }

    override suspend fun unsubscribeFromTopic(topic: String) {
        firebaseMessaging.unsubscribeFromTopic(topic).awaitTask()
    }
}

private suspend fun <T> Task<T>.awaitTask(): T =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            val exception = task.exception
            if (exception != null) {
                continuation.resumeWithException(exception)
            } else if (task.isCanceled) {
                continuation.cancel()
            } else {
                @Suppress("UNCHECKED_CAST")
                continuation.resume(task.result as T)
            }
        }
    }
