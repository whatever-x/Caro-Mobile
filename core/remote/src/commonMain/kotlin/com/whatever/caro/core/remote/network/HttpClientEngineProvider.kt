package com.whatever.caro.core.remote.network

import io.ktor.client.engine.HttpClientEngine

expect class HttpClientEngineProvider() {

    fun createEngine(): HttpClientEngine

}