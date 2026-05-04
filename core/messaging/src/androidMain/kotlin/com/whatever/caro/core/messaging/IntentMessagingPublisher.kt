package com.whatever.caro.core.messaging

import android.content.Intent

object IntentMessagingPublisher {
    fun publishFromIntent(intent: Intent?) {
        val extras = intent?.extras ?: return
        val deckId = extras.getString(KEY_DECK_ID) ?: return

        MessagingEventBus.publishMessage(RemoteMessage(deckId = deckId))
    }

    private const val KEY_DECK_ID = "deck_id"
}
