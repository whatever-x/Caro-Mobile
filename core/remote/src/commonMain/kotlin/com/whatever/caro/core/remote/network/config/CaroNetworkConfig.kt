package com.whatever.caro.core.remote.network.config

expect object CaroNetworkConfig {
    val BASE_URL: String

    val isDebug: Boolean

    val platform: String

    val appVersion: String
}
