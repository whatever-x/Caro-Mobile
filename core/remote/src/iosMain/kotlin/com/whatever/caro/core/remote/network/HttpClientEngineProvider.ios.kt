package com.whatever.caro.core.remote.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

actual class HttpClientEngineProvider {
    actual fun createEngine(): HttpClientEngine = Darwin.create()
}
