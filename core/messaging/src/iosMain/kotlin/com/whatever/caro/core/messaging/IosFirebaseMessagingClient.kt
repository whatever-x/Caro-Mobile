package com.whatever.caro.core.messaging

import FirebaseMessaging.FIRMessaging
import FirebaseMessaging.FIRMessagingDelegateProtocol
import io.github.aakira.napier.Napier
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSData
import platform.UIKit.UIApplication
import platform.UIKit.registerForRemoteNotifications
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationPresentationOptionBanner
import platform.UserNotifications.UNNotificationPresentationOptionList
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationPresentationOptions
import platform.UserNotifications.UNNotificationResponse
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal class IosFirebaseMessagingClient : MessagingClient {
    private val mutableTokenFlow = MutableStateFlow("")
    private val mutableMessages = Channel<CloudMessage>(capacity = Channel.CONFLATED)

    override val tokenFlow: StateFlow<String> = mutableTokenFlow.asStateFlow()
    override val messages: ReceiveChannel<CloudMessage> = mutableMessages

    private val delegate = MessagingDelegate(mutableTokenFlow, mutableMessages)

    fun attach(application: UIApplication) {
        FIRMessaging.messaging().delegate = delegate
        UNUserNotificationCenter.currentNotificationCenter().delegate = delegate
        application.registerForRemoteNotifications()
    }

    fun applyApnsToken(deviceToken: NSData) {
        FIRMessaging.messaging().setAPNSToken(deviceToken)
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class MessagingDelegate(
    private val tokenFlow: MutableStateFlow<String>,
    private val messages: Channel<CloudMessage>,
) : NSObject(),
    FIRMessagingDelegateProtocol,
    UNUserNotificationCenterDelegateProtocol {
    override fun messaging(
        messaging: FIRMessaging,
        didReceiveRegistrationToken: String?,
    ) {
        val token = didReceiveRegistrationToken ?: return
        Napier.d { "FCM token refreshed: $token" }
        tokenFlow.tryEmit(token)
    }

    // 포그라운드 상태에서 푸쉬 도착
    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        willPresentNotification: UNNotification,
        withCompletionHandler: (UNNotificationPresentationOptions) -> Unit,
    ) {
        val userInfo = willPresentNotification.request.content.userInfo
        // TODO: userInfo 파싱 후 CloudMessage 발행
        withCompletionHandler(
            UNNotificationPresentationOptionBanner or
                UNNotificationPresentationOptionList or
                UNNotificationPresentationOptionSound,
        )
    }

    // 백그라운드, 알림센터 등에서 푸쉬를 눌렀을 경우 액션
    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        didReceiveNotificationResponse: UNNotificationResponse,
        withCompletionHandler: () -> Unit,
    ) {
        val userInfo = didReceiveNotificationResponse.notification.request.content.userInfo
        // TODO: userInfo 파싱 후 CloudMessage 발행
        withCompletionHandler()
    }
}
