package com.whatever.caro.core.remote.network.config

import com.whatever.caro.core.remote.generated.BuildKonfig
import platform.Foundation.NSBundle
import kotlin.experimental.ExperimentalNativeApi

private const val CARO_BASE_URL_KEY = "Caro Base Url"
private const val FALLBACK_BASE_URL = "http://localhost/"

@OptIn(ExperimentalNativeApi::class)
actual object CaroNetworkConfig {
    actual val BASE_URL: String = resolveBaseUrl()
    actual val isDebug: Boolean = Platform.isDebugBinary
    actual val platform: String = "IOS"
    actual val appVersion: String =
        NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
            ?: BuildKonfig.APP_VERSION

    private fun resolveBaseUrl(): String {
        val baseUrl = NSBundle.mainBundle.objectForInfoDictionaryKey(CARO_BASE_URL_KEY) as? String
        if (baseUrl.isNullOrBlank().not()) return baseUrl

        if (Platform.isDebugBinary) {
            println("Missing Info.plist value for $CARO_BASE_URL_KEY")
        }
        return FALLBACK_BASE_URL
    }
}
