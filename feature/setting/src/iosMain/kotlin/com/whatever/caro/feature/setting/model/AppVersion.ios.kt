package com.whatever.caro.feature.setting.model

import platform.Foundation.NSBundle

actual object AppConfig {
    actual val appVersion : String
        get() = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as String
}