package com.whatever.caro.core.remote.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

actual object HttpClientEngineProvider {
    actual fun provide(): HttpClientEngine = OkHttp.create()
}
