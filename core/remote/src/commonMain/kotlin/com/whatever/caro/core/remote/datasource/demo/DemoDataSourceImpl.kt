package com.whatever.caro.core.remote.datasource.demo

import com.whatever.caro.core.remote.di.qualifier.CaroClient
import com.whatever.caro.core.remote.di.qualifier.NetworkClient
import com.whatever.caro.core.remote.model.DemoDto
import com.whatever.caro.core.remote.model.demo.request.DemoRequest
import com.whatever.caro.core.remote.model.demo.response.DemoResponse
import io.ktor.client.HttpClient
import org.koin.core.annotation.Single

@Single(binds = [DemoDataSource::class])
internal class DemoDataSourceImpl(
    @NetworkClient(CaroClient.Auth) private val authClient: HttpClient,
    @NetworkClient(CaroClient.Default) private val defaultClient: HttpClient,
) : DemoDataSource {

    private val demoResponse = DemoResponse(
        user = DemoDto(
            id = 0L,
            name = "이름"
        )
    )

    override suspend fun getRestWithPath(path: Long): DemoResponse {
//        return authClient.get(BASE_DEMO_URL + "/$path")
//            .body<DemoResponse>()

        return demoResponse
    }

    override suspend fun getRestWithQuery(query: Int): DemoResponse {
//        return authClient.get(BASE_DEMO_URL) {
//            parameter("queryKey", query)
//        }.body<DemoResponse>()

        return demoResponse
    }

    override suspend fun getRestWithQueryPath(
        query: Int,
        path: Long
    ): DemoResponse {
//        return authClient.get(BASE_DEMO_URL + "/$path") {
//            parameter("queryKey", query)
//        }.body<DemoResponse>()

        return demoResponse
    }

    override suspend fun postRestWithRequest(request: DemoRequest): DemoResponse {
//        return authClient.post(BASE_DEMO_URL + "/some/entrypoint") {
//            setBody(body = request)
//        }.body<DemoResponse>()

        return demoResponse
    }

    companion object {
        private const val BASE_DEMO_URL = "/v1/demo..."
    }

}