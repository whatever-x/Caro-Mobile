package com.whatever.caro.core.messaging

import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.mp.KoinPlatform
import platform.Foundation.NSData
import platform.UIKit.UIApplication

@OptIn(ExperimentalForeignApi::class)
fun attachMessaging(application: UIApplication) {
    KoinPlatform.getKoin().get<IosFirebaseMessagingClient>().attach(application)
}

@OptIn(ExperimentalForeignApi::class)
fun applyApnsToken(deviceToken: NSData) {
    KoinPlatform.getKoin().get<IosFirebaseMessagingClient>().applyApnsToken(deviceToken)
}
