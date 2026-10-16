package com.whatever.caro.core.ui.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenNotificationSettingsURLString
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.registerForRemoteNotifications
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatus
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Composable
actual fun rememberNotificationPermissionController(): NotificationPermissionController = remember { IosNotificationPermissionController() }

@OptIn(ExperimentalForeignApi::class)
private class IosNotificationPermissionController : NotificationPermissionController {
    private val center = UNUserNotificationCenter.currentNotificationCenter()
    private val mutex = Mutex()

    override suspend fun isGranted(): Boolean {
        val status = authorizationStatus()
        return status == UNAuthorizationStatusAuthorized || status == UNAuthorizationStatusProvisional
    }

    override suspend fun request(): Boolean =
        withContext(Dispatchers.Main.immediate) {
            mutex.withLock {
                if (authorizationStatus() == UNAuthorizationStatusNotDetermined) {
                    suspendCancellableCoroutine { continuation ->
                        center.requestAuthorizationWithOptions(
                            UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
                        ) { granted, error ->
                            if (continuation.isActive) {
                                if (error != null) {
                                    continuation.resumeWithException(IllegalStateException(error.localizedDescription))
                                } else {
                                    continuation.resume(granted)
                                }
                            }
                        }
                    }
                }
                isGranted().also { granted ->
                    if (granted) UIApplication.sharedApplication.registerForRemoteNotifications()
                }
            }
        }

    override fun openSettings() {
        val application = UIApplication.sharedApplication
        val notificationSettings = NSURL.URLWithString(UIApplicationOpenNotificationSettingsURLString)
        val settings =
            notificationSettings?.takeIf { application.canOpenURL(it) }
                ?: NSURL.URLWithString(UIApplicationOpenSettingsURLString)
        if (settings != null) application.openURL(settings, options = emptyMap<Any?, Any>(), completionHandler = null)
    }

    private suspend fun authorizationStatus(): UNAuthorizationStatus =
        suspendCancellableCoroutine { continuation ->
            center.getNotificationSettingsWithCompletionHandler { settings ->
                if (continuation.isActive) continuation.resume(checkNotNull(settings).authorizationStatus)
            }
        }
}
