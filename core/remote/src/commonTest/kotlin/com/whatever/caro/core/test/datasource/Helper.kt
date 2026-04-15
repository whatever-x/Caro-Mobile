package com.whatever.caro.core.test.datasource

import com.whatever.caro.core.remote.network.HttpClientFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler

internal fun <T> createDataSource(
    handler: MockRequestHandler,
    factory: (HttpClient) -> T,
): T {
    val engine = MockEngine(handler)
    val httpClient = HttpClientFactory.create(engine)

    return factory(httpClient)
}
