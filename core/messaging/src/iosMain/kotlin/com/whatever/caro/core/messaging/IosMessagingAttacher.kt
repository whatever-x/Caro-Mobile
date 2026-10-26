package com.whatever.caro.core.messaging

import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.mp.KoinPlatform
import platform.Foundation.NSData

@OptIn(ExperimentalForeignApi::class)
fun attachMessaging() {
    KoinPlatform.getKoin().get<IosFirebaseMessagingClient>().attach()
}

@OptIn(ExperimentalForeignApi::class)
fun applyApnsToken(deviceToken: NSData) {
    KoinPlatform.getKoin().get<IosFirebaseMessagingClient>().applyApnsToken(deviceToken)
}
