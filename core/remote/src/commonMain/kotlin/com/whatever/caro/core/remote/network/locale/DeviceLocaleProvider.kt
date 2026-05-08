package com.whatever.caro.core.remote.network.locale

expect object DeviceLocaleProvider {
    fun acceptLanguage(): String

    fun timezone(): String
}
