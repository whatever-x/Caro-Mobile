package com.whatever.caro.core.remote.network

import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.HttpClientEngine

actual class HttpClientEngineProvider {

    actual fun createEngine(): HttpClientEngine = OkHttp.create()

}