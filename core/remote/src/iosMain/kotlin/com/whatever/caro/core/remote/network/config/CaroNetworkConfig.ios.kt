package com.whatever.caro.core.remote.network.config

import platform.Foundation.NSBundle
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
actual object CaroNetworkConfig {
    actual val BASE_URL: String = NSBundle.mainBundle.objectForInfoDictionaryKey("CARO_BASE_URL") as String
    actual val isDebug: Boolean = Platform.isDebugBinary
}
