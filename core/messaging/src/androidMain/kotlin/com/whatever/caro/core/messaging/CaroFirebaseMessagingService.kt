package com.whatever.caro.core.messaging

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.google.firebase.messaging.FirebaseMessagingService
import io.github.aakira.napier.Napier
import com.google.firebase.messaging.RemoteMessage as FcmRemoteMessage

class CaroFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Napier.d { "FCM token refreshed" }
        MessagingEventBus.publishToken(token)
    }

    override fun onMessageReceived(message: FcmRemoteMessage) {
        super.onMessageReceived(message)
        val cloudMessage = CloudMessage(anyValue = message.data[MESSAGE_VALUE_KEY])
        MessagingEventBus.publishMessage(cloudMessage)
        showNotification(
            title = message.notification?.title ?: message.data[TITLE_KEY],
            body = message.notification?.body ?: message.data[BODY_KEY],
            messageValue = cloudMessage.anyValue,
        )
    }

    private fun showNotification(
        title: String?,
        body: String?,
        messageValue: String?,
    ) {
        if (title.isNullOrBlank() && body.isNullOrBlank()) return
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    applicationInfo.loadLabel(packageManager),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }

        val launchIntent =
            packageManager.getLaunchIntentForPackage(packageName)?.apply {
                putExtra(MESSAGE_VALUE_KEY, messageValue)
                addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        val pendingIntent =
            launchIntent?.let {
                PendingIntent.getActivity(
                    this,
                    0,
                    it,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            }
        val builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(this)
            }
        builder
            .setSmallIcon(applicationInfo.icon)
            .setContentTitle(title.orEmpty())
            .setContentText(body.orEmpty())
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(messageValue?.hashCode() ?: System.currentTimeMillis().toInt(), builder.build())
    }

    private companion object {
        const val MESSAGE_VALUE_KEY = "anyValue"
        const val TITLE_KEY = "title"
        const val BODY_KEY = "body"
        const val NOTIFICATION_CHANNEL_ID = "caro_learning"
    }
}
