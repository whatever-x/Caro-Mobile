package com.whatever.caro.feature.login.config

import platform.Foundation.NSBundle
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
internal object GoogleAuthConfig {
    val GID_CLIENT_ID = NSBundle.mainBundle.objectForInfoDictionaryKey("GIDClientID") as String
    val GID_WEB_CLIENT_ID = NSBundle.mainBundle.objectForInfoDictionaryKey("GIDWebClientID") as String
}
