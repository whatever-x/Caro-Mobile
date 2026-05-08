package com.whatever.caro.core.remote.network.locale

import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone

actual object DeviceLocaleProvider {
    actual fun acceptLanguage(): String =
        (NSLocale.preferredLanguages.firstOrNull() as? String)
            ?: NSLocale.currentLocale.localeIdentifier

    actual fun timezone(): String = NSTimeZone.localTimeZone.name
}
