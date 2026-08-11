package com.whatever.caro.core.remote.network

import io.ktor.client.engine.HttpClientEngine

expect object HttpClientEngineProvider {
    fun provide(): HttpClientEngine
}
