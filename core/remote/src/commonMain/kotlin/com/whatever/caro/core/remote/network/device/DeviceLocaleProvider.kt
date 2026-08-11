package com.whatever.caro.core.remote.network.device

expect object DeviceLocaleProvider {
    fun acceptLanguage(): String

    fun timezone(): String
}
