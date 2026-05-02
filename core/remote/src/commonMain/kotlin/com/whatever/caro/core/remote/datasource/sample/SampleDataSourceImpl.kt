package com.whatever.caro.core.remote.datasource.sample

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class SampleDataSourceImpl(
    private val httpClient: HttpClient,
) : SampleDataSource {
    override suspend fun getString(): String = httpClient.get("/sample/sample").body()
}
