package com.whatever.caro.core.messaging

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

internal class AndroidNotificationPresenter(
    private val context: Context,
) : NotificationPresenter {
    override fun show(
        title: String,
        body: String,
    ) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                context.applicationInfo.loadLabel(context.packageManager),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )

        val notificationId = System.currentTimeMillis().toInt()
        val launchIntent =
            context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        val pendingIntent =
            launchIntent?.let {
                PendingIntent.getActivity(
                    context,
                    notificationId,
                    it,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            }

        notificationManager.notify(
            notificationId,
            Notification
                .Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(context.applicationInfo.icon)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build(),
        )
    }

    private companion object {
        const val NOTIFICATION_CHANNEL_ID = "caro_learning"
    }
}
