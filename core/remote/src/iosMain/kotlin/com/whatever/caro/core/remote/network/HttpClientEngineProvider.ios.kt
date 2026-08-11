package com.whatever.caro.core.remote.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

actual object HttpClientEngineProvider {
    actual fun provide(): HttpClientEngine = Darwin.create()
}
