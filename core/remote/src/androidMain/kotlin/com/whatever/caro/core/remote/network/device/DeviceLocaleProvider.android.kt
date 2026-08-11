package com.whatever.caro.core.remote.network.device

import java.util.Locale
import java.util.TimeZone

actual object DeviceLocaleProvider {
    actual fun acceptLanguage(): String = Locale.getDefault().toLanguageTag()

    actual fun timezone(): String = TimeZone.getDefault().id
}
