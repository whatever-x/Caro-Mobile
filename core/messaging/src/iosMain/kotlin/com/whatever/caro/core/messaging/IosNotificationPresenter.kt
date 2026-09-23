package com.whatever.caro.core.messaging

import io.github.aakira.napier.Napier
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSUUID
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal class IosNotificationPresenter : NotificationPresenter {
    override fun show(
        title: String,
        body: String,
    ) {
        val content =
            UNMutableNotificationContent().apply {
                setTitle(title)
                setBody(body)
                setSound(UNNotificationSound.defaultSound())
            }
        val request =
            UNNotificationRequest.requestWithIdentifier(
                identifier = NSUUID().UUIDString,
                content = content,
                trigger = null,
            )
        UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(request) { error ->
            if (error != null) Napier.e { "Local notification failed: $error" }
        }
    }
}
