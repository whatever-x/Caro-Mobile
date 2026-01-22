package com.whatever.caro.core.remote.network.config

import platform.Foundation.NSBundle
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
actual object CaroNetworkConfig {
    actual val BASE_URL: String = NSBundle.mainBundle.objectForInfoDictionaryKey("Caro Base Url") as String
    actual val isDebug: Boolean = Platform.isDebugBinary
}