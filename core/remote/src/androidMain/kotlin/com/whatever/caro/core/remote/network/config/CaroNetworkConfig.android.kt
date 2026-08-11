package com.whatever.caro.core.remote.network.config

import com.whatever.caro.core.remote.generated.BuildKonfig

actual object CaroNetworkConfig {
    actual val BASE_URL: String = BuildKonfig.SERVER_BASE_URL
    actual val isDebug: Boolean = BuildKonfig.IS_DEBUG
}
