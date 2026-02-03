package com.whatever.caro.core.remote.datasource.demo

import com.whatever.caro.core.remote.model.demo.request.DemoRequest
import com.whatever.caro.core.remote.model.demo.response.DemoResponse

interface DemoDataSource {
    suspend fun getRestWithPath(path: Long): DemoResponse

    suspend fun getRestWithQuery(query: Int): DemoResponse

    suspend fun getRestWithQueryPath(
        query: Int,
        path: Long,
    ): DemoResponse

    suspend fun postRestWithRequest(request: DemoRequest): DemoResponse
}
