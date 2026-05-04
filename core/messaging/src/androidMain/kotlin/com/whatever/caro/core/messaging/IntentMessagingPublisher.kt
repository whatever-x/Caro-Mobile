package com.whatever.caro.core.messaging

import android.content.Intent

object IntentMessagingPublisher {
    fun publishFromIntent(intent: Intent?) {
        val extras = intent?.extras ?: return
        val data =
            extras
                .keySet()
                .mapNotNull { key ->
                    val value = extras.getString(key) ?: return@mapNotNull null
                    key to value
                }.toMap()
        if (data.isEmpty()) return

        val messageId = data[KEY_GCM_MESSAGE_ID]
        val sentTime = data[KEY_SENT_TIME]?.toLongOrNull() ?: System.currentTimeMillis()

        MessagingEventBus.publishMessage(
            RemoteMessage(
                messageId = messageId,
                title = null,
                body = null,
                data = data,
                sentTime = sentTime,
            ),
        )
    }

    private const val KEY_GCM_MESSAGE_ID = "google.message_id"
    private const val KEY_SENT_TIME = "google.sent_time"
}
