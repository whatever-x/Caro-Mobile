package com.whatever.caro.core.remote.network.plugins

import com.whatever.caro.core.remote.model.CaroBaseResponse
import com.whatever.caro.core.remote.model.NetworkException
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.readRemaining
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

internal class CaroBaseResponseUnwrapConfig {
    lateinit var json: Json
}

internal val CaroBaseResponseUnwrap: ClientPlugin<CaroBaseResponseUnwrapConfig> =
    createClientPlugin(
        name = "CaroBaseResponseUnwrap",
        createConfiguration = ::CaroBaseResponseUnwrapConfig
    ) {
        val json: Json = pluginConfig.json

        transformResponseBody { response, content, requestedType ->
            val contentType = response.contentType()
            if (contentType?.match(ContentType.Application.Json) != true) {
                return@transformResponseBody null
            }

            val kotlinType = requestedType.kotlinType ?: return@transformResponseBody null
            val payloadText = content.readRemaining().readText(Charsets.UTF_8)

            val requestedSerializer = json.serializersModule.serializer(kotlinType)

            val envelopeSerializer = CaroBaseResponse.serializer(requestedSerializer)
            val baseResponse = json.decodeFromString(envelopeSerializer, payloadText)

            if (!baseResponse.success) {
                val error = baseResponse.error

                throw NetworkException(
                    code = error?.code ?: "Unknown",
                    message = error?.message ?: "Unknown Error",
                    debugMessage = error?.debugMessage ?: "Unknown Error",
                    description = error?.description,
                )
            }

            val data = baseResponse.data

            if (data != null) {
                return@transformResponseBody data
            }

            if (requestedType.type == Unit::class) {
                return@transformResponseBody Unit
            }

            throw NetworkException(
                code = "Unknown",
                message = "예상치 못한 에러가 발생했습니다.",
                debugMessage = "Data is null",
                description = null,
            )
        }
    }
