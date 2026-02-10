package com.whatever.caro.core.remote.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

actual class HttpClientEngineProvider {
    actual fun createEngine(): HttpClientEngine = OkHttp.create()
}
