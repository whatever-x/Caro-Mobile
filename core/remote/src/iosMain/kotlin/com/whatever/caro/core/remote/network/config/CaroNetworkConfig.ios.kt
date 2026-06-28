package com.whatever.caro.core.remote.network.config

import platform.Foundation.NSBundle
import kotlin.experimental.ExperimentalNativeApi

private const val CARO_BASE_URL_KEY = "Caro Base Url"

@OptIn(ExperimentalNativeApi::class)
actual object CaroNetworkConfig {
    actual val BASE_URL: String = NSBundle.mainBundle.objectForInfoDictionaryKey(CARO_BASE_URL_KEY) as? String ?: ""
    actual val isDebug: Boolean = Platform.isDebugBinary
}
