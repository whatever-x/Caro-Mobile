package com.whatever.caro.core.remote.network.device

import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.currentLocale
import platform.Foundation.localTimeZone
import platform.Foundation.localeIdentifier
import platform.Foundation.preferredLanguages

actual object DeviceLocaleProvider {
    actual fun acceptLanguage(): String =
        (NSLocale.preferredLanguages.firstOrNull() as? String)
            ?: NSLocale.currentLocale.localeIdentifier

    actual fun timezone(): String = NSTimeZone.localTimeZone.name
}
