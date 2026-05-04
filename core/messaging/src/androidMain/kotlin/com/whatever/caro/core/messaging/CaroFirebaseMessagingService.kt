package com.whatever.caro.core.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.google.firebase.messaging.FirebaseMessagingService
import kotlin.random.Random
import com.google.firebase.messaging.RemoteMessage as FcmRemoteMessage

class CaroFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        MessagingEventBus.publishToken(token)
    }

    override fun onMessageReceived(message: FcmRemoteMessage) {
        super.onMessageReceived(message)

        val deckId = message.data[KEY_DECK_ID]
        if (deckId != null) {
            MessagingEventBus.publishMessage(RemoteMessage(deckId = deckId))
        }

        val title = message.notification?.title ?: message.data[KEY_TITLE]
        val body = message.notification?.body ?: message.data[KEY_BODY]
        if (title == null && body == null) return

        showNotification(title = title, body = body, deckId = deckId)
    }

    private fun showNotification(
        title: String?,
        body: String?,
        deckId: String?,
    ) {
        val manager = getSystemService<NotificationManager>() ?: return
        ensureChannel(manager)

        val launchIntent =
            packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                if (deckId != null) putExtra(KEY_DECK_ID, deckId)
            }

        val pendingIntent =
            launchIntent?.let {
                PendingIntent.getActivity(
                    this,
                    Random.nextInt(),
                    it,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            }

        val notification =
            NotificationCompat
                .Builder(this, DEFAULT_CHANNEL_ID)
                .setSmallIcon(applicationInfo.icon)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

        manager.notify(Random.nextInt(), notification)
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (manager.getNotificationChannel(DEFAULT_CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                DEFAULT_CHANNEL_ID,
                DEFAULT_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH,
            ),
        )
    }

    companion object {
        const val DEFAULT_CHANNEL_ID = "caro_default"
        private const val DEFAULT_CHANNEL_NAME = "Caro"
        private const val KEY_DECK_ID = "deck_id"
        private const val KEY_TITLE = "title"
        private const val KEY_BODY = "body"
    }
}
